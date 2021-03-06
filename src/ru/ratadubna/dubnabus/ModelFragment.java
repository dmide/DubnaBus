package ru.ratadubna.dubnabus;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.maps.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.ratadubna.dubnabus.tasks.ScheduleLoadTask;
import ru.ratadubna.dubnabus.tasks.TaxiPhonesLoadTask;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelFragment extends SherlockFragment {

    public static final String NUMBER_SYMBOL = "�";
    public static final String TAXI_DIALOG_SHOWED = "TAXI_DIALOG_SHOWED";
    private static final int[] COLORS = {0x00FF0000, 0x000000FF, 0x00FF00FF,
            0x00FF8800, 0x000088FF, 0x00FF88FF, 0x00880000, 0x00000088,
            0x00880088, 0x00888888};

    public String selectedBusStopSchedule;
    private Timer busLoadingTimer;
    private volatile boolean busLoadingTimerMutex = false;
    private volatile boolean busRoutesAndMarkersTaskMutex = false;
    private final HashMap<String, Integer> descStopIdMap = new HashMap<String, Integer>();

    private final Handler busLocationUpdateHandler = new Handler() {
        public void handleMessage(Message msg) {
            ((DubnaBusActivity) getActivity()).addBuses();
        }
    };

    @TargetApi(11)
    static private <T> void executeAsyncTask(AsyncTask<T, ?, ?> task,
                                             T... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }

    public static void showPromoDialog(final Activity activity, final boolean onExit) {
        View rateDialog = View.inflate(activity, R.layout.promo_dialog, null);
        CheckBox checkBox = (CheckBox) rateDialog.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(TAXI_DIALOG_SHOWED, isChecked).commit();
            }
        });
        new AlertDialog.Builder(activity)
                .setView(rateDialog)
                .setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse(activity.getString(R.string.taxi_app_link));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton(activity.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (onExit) {
                            activity.finish();
                        }
                    }
                })
                .setInverseBackgroundForced(true)
                .show();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    public void showProblemToast() {
        Toast.makeText(getActivity(), getString(R.string.problem),
                Toast.LENGTH_LONG).show();
    }

    boolean isMapLoaded() {
        return (!descStopIdMap.isEmpty());
    }

    void loadMapRoutes() {
        if (!busRoutesAndMarkersTaskMutex) {
            executeAsyncTask(new GetBusRoutesTask());
            executeAsyncTask(new GetBusStopsTask());
        }
    }

    void processMarker(Marker marker) {
        if ((descStopIdMap != null) && (marker.getTitle() != null)) {
            Integer id = descStopIdMap.get(marker.getTitle());
            ScheduleLoadTask scheduleLoadTask = new ScheduleLoadTask(this, id, marker);
            executeAsyncTask(scheduleLoadTask);
        }
    }

    Entry<Integer, Integer> getNearestBusDelay(int targetDelay) {
        TreeMap<Integer, Integer> delays = new TreeMap<Integer, Integer>();
        int timeDelay, routeId;
        ArrayList<BusRoute> busRoutes = BusRoute.getRoutesArray();
        for (int i = 0; i < busRoutes.size(); i++) {
            if (BusRoute.getRoute(i).isActive()) {
                routeId = busRoutes.get(i).getRouteRealId();
                timeDelay = getDelayFromSelectedSchedule(targetDelay, routeId);
                if (timeDelay != 0) {
                    delays.put(timeDelay, routeId);
                }
            }
        }
        return delays.firstEntry();
    }

    void loadTaxiPage(String url) {
        TaxiPhonesLoadTask taxiTask = new TaxiPhonesLoadTask(this, url);
        ModelFragment.executeAsyncTask(taxiTask);
    }

    void startLoadingBusLocations() {
        if (!busLoadingTimerMutex) {
            busLoadingTimerMutex = true;
            busLoadingTimer = new Timer();
            busLoadingTimer.schedule(new GetBusLocationsTask(), 0);
        }
    }

    void continueLoadingBusLocations() {
        if (busLoadingTimerMutex) {
            busLoadingTimer.schedule(new GetBusLocationsTask(), 5000);
        }
    }

    void stopLoadingBusLocations() {
        if (busLoadingTimer != null) {
            busLoadingTimer.cancel();
        }
        busLoadingTimerMutex = false;
    }

    private Integer getColor(int i) {
        if (i > 9)
            i -= 10; // in case of new routes appearing
        Integer color = 0x6A000000; // AARRGGBB
        color += COLORS[i];
        return color;
    }

    private int getDelayFromSelectedSchedule(int targetDelay, int route) {
        boolean suitableTimeFound = false;
        Pattern pattern = Pattern.compile(NUMBER_SYMBOL + route + "[<&](.+?)<br");
        Matcher matcher = pattern.matcher(selectedBusStopSchedule);
        if (matcher.find()) {
            // resultTime variable is here because server returns arrival times
            // in wrong order after 00:00
            String scheduleTime;
            Date resultTime = new Date(new Date().getTime() + 300 * 60 * 1000);
            Date targetTime = new Date(new Date().getTime() + targetDelay * 60000);
            Pattern pattern2 = Pattern.compile("(\\d+:\\d+)");
            Matcher matcher2 = pattern2.matcher(matcher.group());
            Calendar calendarNow = new GregorianCalendar();
            calendarNow.setTime(new Date());
            while (matcher2.find()) {
                scheduleTime = matcher2.group(1);
                Calendar calendarSchedule = new GregorianCalendar();
                calendarSchedule.set(Calendar.HOUR_OF_DAY,
                        Integer.parseInt(scheduleTime.substring(0, 2)));
                calendarSchedule.set(Calendar.MINUTE,
                        Integer.parseInt(scheduleTime.substring(3, 5)));
                // in case of day change, but not in case of ratadubna.ru sent
                // outdated data. (data is usually outdated for a couple of
                // minutes)
                if ((calendarNow.getTimeInMillis() - calendarSchedule
                        .getTimeInMillis()) > 120 * 60000) {
                    calendarSchedule.add(Calendar.DATE, 1);
                }
                if (targetTime.before(calendarSchedule.getTime())
                        && resultTime.after(calendarSchedule.getTime())) {
                    resultTime = calendarSchedule.getTime();
                    suitableTimeFound = true;
                }
            }
            if (suitableTimeFound) {
                return (int) (resultTime.getTime() - targetTime.getTime());
            }
        }
        return 0;
    }

    private class GetBusLocationsTask extends TimerTask {
        public void run() {
            if (busLoadingTimerMutex) {
                try {
                    WebHelper.loadContent(
                            new URL(getString(R.string.bus_location_url)),
                            new BusLocationLoader(), "");
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(),
                            "Exception retrieving bus location", e);
                }
            } else {
                return;
            }
            busLocationUpdateHandler.obtainMessage(1).sendToTarget();
        }

        private class BusLocationLoader implements WebHelper.Parser {
            @Override
            public void parse(String line) throws Exception {
                JSONArray jsonArray = new JSONArray(line);
                String id, time;
                int bearing, routeNum, routeId, type;
                LatLng latLng;
                JSONObject bus;
                for (int i = 0; i < jsonArray.length(); i++) {
                    bus = (JSONObject) jsonArray.get(i);
                    id = String.valueOf(bus.get("id"));
                    time = (String) bus.get("dt");
                    bearing = (Integer) bus.get("dg");
                    JSONArray jLatLng = (JSONArray) bus.get("lc");
                    latLng = new LatLng(Double.valueOf((String) jLatLng.get(0)),
                            Double.valueOf((String) jLatLng.get(1)));
                    routeNum = Integer.valueOf((String) bus.get("rn"));
                    routeId = Integer.valueOf((String) bus.get("rid"));
                    type = Integer.valueOf((String) bus.get("md"));

                    if (Bus.isActive(id)) {
                        Bus.updateBus(id, latLng, 0, bearing, time);
                    } else {
                        Bus.addToList(new Bus(id, latLng, 0, bearing, type, routeId, time, routeNum));
                    }
                }
            }
        }
    }

    private class GetBusStopsTask extends AsyncTask<Void, Void, Void> {
        final ArrayList<MarkerOptions> busMarkerOptionsArray = new ArrayList<MarkerOptions>();
        private Exception e;

        @Override
        protected Void doInBackground(Void... params) {
            busRoutesAndMarkersTaskMutex = true;
            descStopIdMap.clear();
            for (BusRoute route : BusRoute.getRoutesArray()) {
                try {
                    if (route.isActive()) {
                        int serviceId = route.getRouteServiceId();
                        WebHelper.loadContent(
                                new URL(getString(R.string.map_stops_url) + String.valueOf(serviceId)),
                                new BusStopsLoader(), "56.");
                    }
                } catch (Exception e) {
                    this.e = e;
                    Log.e(getClass().getSimpleName(),
                            "Exception retrieving bus maproutes content", e);
                }
            }
            return (null);
        }

        @Override
        public void onPostExecute(Void arg0) {
            for (MarkerOptions options : busMarkerOptionsArray) {
                ((DubnaBusActivity) getActivity()).addMarker(options);
            }
            startLoadingBusLocations();
            busRoutesAndMarkersTaskMutex = false;
        }

        private class BusStopsLoader implements WebHelper.Parser {
            @Override
            public void parse(String line) throws Exception {
                JSONArray jsonArray = new JSONArray(line);
                JSONObject stop;
                int id;
                double lat, lng;
                String name;
                for (int i = 0; i < jsonArray.length(); i++) {
                    stop = (JSONObject) jsonArray.get(i);
                    id = Integer.valueOf((String) stop.get("id"));
                    JSONArray lc = (JSONArray) stop.get("lc");
                    lat = Double.parseDouble((String) lc.get(0));
                    lng = Double.parseDouble((String) lc.get(1));
                    name = decodeFromUtf8((String) stop.get("name"));
                    if (!descStopIdMap.containsValue(id)) {
                        busMarkerOptionsArray.add(new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .title(name)
                                .icon(BitmapDescriptorFactory
                                        .fromResource(R.drawable.bustop31)));
                        descStopIdMap.put(name, id);
                    }
                }
            }
        }
    }

    //it's surprisingly unfortunate that there's no way in Java to do this
    //apart from haul an apache dependency. hacky and ugly.
    private String decodeFromUtf8(String name) {
        String utf8CharRegexp = "\\\\u.{4}";
        Pattern pattern = Pattern.compile(utf8CharRegexp);
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            do {
                String utf8Char = matcher.group();
                int hexVal = Integer.parseInt(utf8Char.substring(2), 16);
                utf8Char = "";
                utf8Char += (char) hexVal;
                name = name.replaceFirst(utf8CharRegexp, utf8Char);
            } while (matcher.find());
        }
        return name;
    }

    private class GetBusRoutesTask extends AsyncTask<Void, Void, Void> {
        final ArrayList<PolylineOptions> busRoutesOptionsArray = new ArrayList<PolylineOptions>();
        private Exception e;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                busRoutesAndMarkersTaskMutex = true;
                for (BusRoute route : BusRoute.getRoutesArray()) {
                    if (route.isActive()) {
                        int serviceId = route.getRouteServiceId();
                        WebHelper.loadContent(
                                new URL(getString(R.string.map_routes_url) + String.valueOf(serviceId)),
                                new BusRoutesLoader(), "56.");
                    }
                }
            } catch (Exception e) {
                this.e = e;
                Log.e(getClass().getSimpleName(),
                        "Exception retrieving bus maproutes content", e);
            }
            return (null);
        }

        @Override
        public void onPostExecute(Void arg0) {
            if (e == null) {
                int i = 0, j = 0;
                for (PolylineOptions options : busRoutesOptionsArray) {
                    ((DubnaBusActivity) getActivity()).addRoute(options
                            .color(getColor(j)));
                    i++;
                    j = i / 2;
                }
                startLoadingBusLocations();
            } else {
                showProblemToast();
            }
            busRoutesAndMarkersTaskMutex = false;
        }

        private class BusRoutesLoader implements WebHelper.Parser {
            @Override
            public void parse(String line) throws Exception {
                JSONArray jsonArray = new JSONArray(line);
                JSONArray subArray;
                JSONArray coordinates;
                double lat, lng;
                for (int i = 0; i < jsonArray.length(); i++) {
                    subArray = (JSONArray) jsonArray.get(i);
                    PolylineOptions busRoutesOptions = new PolylineOptions();
                    for (int j = 0; j < subArray.length(); j++) {
                        coordinates = (JSONArray) subArray.get(j);
                        lat = Double.parseDouble((String) coordinates.get(0));
                        lng = Double.parseDouble((String) coordinates.get(1));
                        busRoutesOptions.add(new LatLng(lat, lng));
                    }
                    busRoutesOptionsArray.add(busRoutesOptions);
                }
            }
        }
    }
}

