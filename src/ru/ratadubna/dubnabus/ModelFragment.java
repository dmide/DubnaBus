package ru.ratadubna.dubnabus;

import android.annotation.TargetApi;
import android.os.*;
import android.util.Log;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.maps.model.*;
import com.json.parsers.JSONParser;
import ru.ratadubna.dubnabus.tasks.BusRoutesLoadTask;
import ru.ratadubna.dubnabus.tasks.ScheduleLoadTask;
import ru.ratadubna.dubnabus.tasks.TaxiPhonesLoadTask;

import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelFragment extends SherlockFragment {

    public static final String NUMBER_SYMBOL = "¹";
    private static final int[] COLORS = {0x00FF0000, 0x000000FF, 0x00FF00FF,
            0x00FF8800, 0x000088FF, 0x00FF88FF, 0x00880000, 0x00000088,
            0x00880088, 0x00888888};

    public String selectedBusStopSchedule;
    private BusRoutesLoadTask contentsTask;
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        deliverModel();
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
            executeAsyncTask(new GetBusRoutesAndMarkersTask());
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

    private void deliverModel() {
        if (BusRoute.getRoutesArraySize() == 0 && contentsTask == null) {
            contentsTask = new BusRoutesLoadTask(this);
            executeAsyncTask(contentsTask);
        }
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
            public void parse(String line) {
                JSONParser parser = new JSONParser();
                Map jsonData = parser.parseJson(line);
                ArrayList<HashMap> busesList = (ArrayList<HashMap>) jsonData.get("root");
                String id, time;
                int bearing, routeNum, routeId, type;
                LatLng latLng;
                for (HashMap bus : busesList) {
                    id = (String) bus.get("id");
                    time = (String) bus.get("dt");
                    bearing = Integer.valueOf((String) bus.get("dg"));
                    ArrayList jLatLng = (ArrayList) bus.get("lc");
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

    private class GetBusRoutesAndMarkersTask extends AsyncTask<Void, Void, Void> {
        PolylineOptions busRoutesOption;
        final ArrayList<PolylineOptions> busRoutesOptionsArray = new ArrayList<PolylineOptions>();
        final ArrayList<MarkerOptions> busMarkerOptionsArray = new ArrayList<MarkerOptions>();
        private Exception e;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                busRoutesAndMarkersTaskMutex = true;
                descStopIdMap.clear();
                for (BusRoute route : BusRoute.getRoutesArray()) {
                    if (route.isActive()) {
                        int serviceId = route.getRouteServiceId();
                        busRoutesOption = new PolylineOptions();
                        WebHelper.loadContent(
                                new URL(getString(R.string.map_routes_url) + String.valueOf(serviceId)),
                                new BusRoutesAndMarkersLoader(), "56,");
                        busRoutesOptionsArray.add(busRoutesOption);
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

                Pattern pattern = Pattern.compile("([0-9]{2}.[0-9]+)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    double lat, lng;
                    lat = Double.parseDouble(matcher.group());
                    if (matcher.find()) {
                        lng = Double.parseDouble(matcher.group());
                    } else {
                        throw new Exception("parseBusRoutes problem");
                    }
                    busRoutesOption.add(new LatLng(lat, lng));
                } else {
                    throw new Exception("parseBusRoutes problem");
                }

                Pattern pattern2 = Pattern.compile("([0-9]{2}.[0-9]+)\\s+([0-9]{2}.[0-9]+)\\s+(.+)\\s+([0-9]+)");
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
