package ru.ratadubna.dubnabus;

import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import ru.ratadubna.dubnabus.tasks.ContentsLoadTask;
import ru.ratadubna.dubnabus.tasks.GetScheduleTask;
import ru.ratadubna.dubnabus.tasks.GetTaxiPhonesTask;

public class ModelFragment extends SherlockFragment {

    public static final String ROUTES_ARRAY_SIZE = "routes_array_size";
    public String lastBusSchedule;

    private ContentsLoadTask contentsTask = null;
    private SharedPreferences prefs = null;
    private Timer busLoadingTimer;
    private volatile boolean busLoadingTimerMutex = false;
    private volatile boolean busRoutesAndMarkersTaskMutex = false;
    private HashMap<String, Integer> descStopIdMap = new HashMap<String, Integer>();
    private CopyOnWriteArrayList<Integer> currentRoutesIds = new CopyOnWriteArrayList<Integer>();
    private final Handler busLocationUpdateHandler = new Handler() {
        public void handleMessage(Message msg) {
            ((DubnaBusActivity) getActivity()).addBuses();
        }
    };
    private static final int[] COLORS = {0x00FF0000, 0x000000FF, 0x00FF00FF,
            0x00FF8800, 0x000088FF, 0x00FF88FF, 0x00880000, 0x00000088,
            0x00880088, 0x00888888};

    @TargetApi(11)
    static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task,
                                            T... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        }
        setRetainInstance(true);
        deliverModel();
    }

    public void showProblemToast() {
        try {
            Toast.makeText(getActivity(), getString(R.string.problem),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
        }
    }

    boolean isMapLoaded() {
        return (!descStopIdMap.isEmpty());
    }

    void loadMapRoutes() {
        if (!busRoutesAndMarkersTaskMutex) {
            executeAsyncTask(new GetBusRoutesAndMarkersTask());
        }
    }

    void processMarker(Marker marker) {
        if ((descStopIdMap != null) && (marker.getTitle() != null)) {
            Integer id = descStopIdMap.get(marker.getTitle());
            GetScheduleTask getScheduleTask = new GetScheduleTask(this, id, marker);
            executeAsyncTask(getScheduleTask);
        }
    }

    Entry<Integer, Integer> observeBusStop(int targetDelay) {
        TreeMap<Integer, Integer> delays = new TreeMap<Integer, Integer>();
        Integer timeDelay;
        int routeRealId;
        for (Integer i = 0; i < BusRoutes.GetRoutes().size(); i++) {
            if (prefs.getBoolean(i.toString(), false)) {
                try {
                    routeRealId = BusRoutes.GetRoutes().get(i).getRouteRealId();
                    timeDelay = getRealArrivalDelay(targetDelay, routeRealId);
                    if (timeDelay != 0) {
                        delays.put(timeDelay, routeRealId);
                    }
                } catch (ParseException e) {
                    Log.e(getClass().getSimpleName(),
                            "Exception parsing time from string", e);
                }
            }
        }
        return delays.firstEntry();
    }

    synchronized private void deliverModel() {
        if (BusRoutes.GetRoutes().isEmpty() && contentsTask == null) {
            contentsTask = new ContentsLoadTask(this);
            executeAsyncTask(contentsTask);
        }
    }

    void loadTaxiPage(String url) {
        GetTaxiPhonesTask taxiTask = new GetTaxiPhonesTask(this, url);
        ModelFragment.executeAsyncTask(taxiTask);
    }

    void startLoadingBusLocations() {
        if (!currentRoutesIds.isEmpty() && !busLoadingTimerMutex) {
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

    private int getRealArrivalDelay(int targetDelay, int route)
            throws ParseException {
        boolean suitableTimeFound = false;
        Pattern pattern = Pattern.compile("¹" + route + "[<&](.+?)<br");
        Matcher matcher = pattern.matcher(lastBusSchedule);
        if (matcher.find()) {
            // resultTime variable is here because server returns arrival times
            // in wrong order after 00:00
            String scheduleTime;
            Date resultTime = new Date(new Date().getTime() + 300 * 60 * 1000);
            Date targetTime = new Date(new Date().getTime() + targetDelay * 60000);
            Pattern pattern2 = Pattern.compile("(\\d+:\\d+)");
            Matcher matcher2 = pattern2.matcher(matcher.group());
            Calendar calendarNow = new GregorianCalendar();
            Calendar calendarSchedule = new GregorianCalendar();
            calendarNow.setTime(new Date());
            while (matcher2.find()) {
                scheduleTime = matcher2.group(1);
                calendarSchedule.clear();
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
        private int routeId;

        public void run() {
            for (int id : currentRoutesIds) {
                if (busLoadingTimerMutex) {
                    routeId = id;
                    try {
                        WebHelper.loadContent(
                                new URL(getString(R.string.bus_location_url) + String.valueOf(id)),
                                new BusLocationLoader(), "");
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(),
                                "Exception retrieving bus location", e);
                    }
                } else {
                    return;
                }
            }
            busLocationUpdateHandler.obtainMessage(1).sendToTarget();
        }

        private class BusLocationLoader implements WebHelper.Parser {
            @Override
            public void parse(String line) throws Exception {
                line = line.replaceAll(",", ".");
                String[] contents = line.split("\\s");
                if (contents.length > 1) {
                    if (Bus.isActive(contents[1])) {
                        Bus.updateBus(contents[1],
                                new LatLng(Double.parseDouble(contents[4]),
                                        Double.parseDouble(contents[5])),
                                Integer.parseInt(contents[6]), Integer
                                .parseInt(contents[7]), contents[3]);
                    } else {
                        Bus.addToList(new Bus(contents[1], new LatLng(Double
                                .parseDouble(contents[4]), Double
                                .parseDouble(contents[5])), Integer
                                .parseInt(contents[6]), Integer
                                .parseInt(contents[7]), Integer
                                .parseInt(contents[0]), routeId, contents[3]));
                    }
                } else {
                    throw new Exception("parseBusLocs problem");
                }

            }
        }
    }

    private class GetBusRoutesAndMarkersTask extends
            AsyncTask<Void, Void, Void> {
        private Exception e = null;
        PolylineOptions busRoutesOption;
        ArrayList<PolylineOptions> busRoutesOptionsArray = new ArrayList<PolylineOptions>();
        ArrayList<MarkerOptions> busMarkerOptionsArray = new ArrayList<MarkerOptions>();

        @Override
        protected Void doInBackground(Void... params) {
            try {
                busRoutesAndMarkersTaskMutex = true;
                descStopIdMap.clear();
                currentRoutesIds.clear();
                for (Integer i = 0; i < prefs.getInt(ROUTES_ARRAY_SIZE, 0); i++) {
                    if (prefs.getBoolean(i.toString(), false)) {
                        int id = prefs.getInt("id_at_" + i.toString(), 0);
                        busRoutesOption = new PolylineOptions();
                        WebHelper.loadContent(
                                new URL(getString(R.string.map_routes_url) + String.valueOf(id)),
                                new BusRoutesAndMarkersLoader(), "56,");
                        busRoutesOptionsArray.add(busRoutesOption);
                        currentRoutesIds.add(id);
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
                int i = 0;
                for (PolylineOptions options : busRoutesOptionsArray) {
                    ((DubnaBusActivity) getActivity()).addRoute(options
                            .color(getColor(i++)));
                }
                for (MarkerOptions options : busMarkerOptionsArray) {
                    ((DubnaBusActivity) getActivity()).addMarker(options);
                }
                startLoadingBusLocations();
            } else {
                showProblemToast();
            }
            busRoutesAndMarkersTaskMutex = false;
        }

        private class BusRoutesAndMarkersLoader implements WebHelper.Parser {
            @Override
            public void parse(String line) throws Exception {
                line = line.replaceAll(",", ".");

                Pattern pattern = Pattern.compile("([0-9]{2}.[0-9]+)"), pattern2 = Pattern
                        .compile("([0-9]{2}.[0-9]+)\\s+([0-9]{2}.[0-9]+)\\s+(.+)\\s+([0-9]+)");

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    double lat, lng;
                    lat = Double.parseDouble(matcher.group());
                    if (matcher.find())
                        lng = Double.parseDouble(matcher.group());
                    else
                        throw new Exception("parseBusRoutes problem");
                    busRoutesOption.add(new LatLng(lat, lng));
                } else {
                    throw new Exception("parseBusRoutes problem");
                }

                matcher = pattern2.matcher(line);
                if (matcher.find()) {
                    double lat = Double.parseDouble(matcher.group(1)), lng = Double
                            .parseDouble(matcher.group(2));
                    String desc = matcher.group(3);
                    int id = Integer.parseInt(matcher.group(4));
                    if (!descStopIdMap.containsValue(id)) {
                        busMarkerOptionsArray.add(new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .title(desc)
                                .icon(BitmapDescriptorFactory
                                        .fromAsset("bustop31.png")));
                        descStopIdMap.put(desc, id);
                    }
                }
            }
        }
    }
}
