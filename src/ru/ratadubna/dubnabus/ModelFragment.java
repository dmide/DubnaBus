package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
import android.content.Context;
import android.content.Intent;
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

public class ModelFragment extends SherlockFragment {

    public static final String ROUTES_ARRAY_SIZE = "routes_array_size";

    String lastBusSchedule;
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

    boolean isMapLoaded() {
        return (!descStopIdMap.isEmpty());
    }

    void loadMapRoutes() {
        if (!busRoutesAndMarkersTaskMutex) {
            executeAsyncTask(new GetBusRoutesAndMarkersTask(), getActivity());
        }
    }

    void processMarker(Marker marker) {
        if ((descStopIdMap != null) && (marker.getTitle() != null)) {
            Integer id = descStopIdMap.get(marker.getTitle());
            GetScheduleTask getScheduleTask = new GetScheduleTask(id, marker);
            executeAsyncTask(getScheduleTask, getActivity());
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
                    if ((timeDelay = getTimeDelay(targetDelay, routeRealId)) != 0)
                        delays.put(timeDelay, routeRealId);
                } catch (ParseException e) {
                    Log.e(getClass().getSimpleName(),
                            "Exception parsing time from string", e);
                }
            }
        }
        return delays.firstEntry();
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

    synchronized private void deliverModel() {
        if (BusRoutes.GetRoutes().isEmpty() && contentsTask == null) {
            contentsTask = new ContentsLoadTask();
            executeAsyncTask(contentsTask, getActivity());
        }
    }

    String getPage(URL url) throws Exception {
        StringBuilder buf;
        BufferedReader reader;
        int i = 0;
        do {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setReadTimeout(15000);
            c.connect();
            reader = new BufferedReader(new InputStreamReader(
                    c.getInputStream()));
            buf = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }
        } while (buf.toString().equals("\n") && (++i < 5)); // check for empty
        // page
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("ModelFragment loadPage",
                        "Exception closing HUC reader", e);
            }
        }

        return buf.toString();
    }

    void loadTaxiPage(String url) {
        GetTaxiPhonesTask taxiTask = new GetTaxiPhonesTask(url);
        ModelFragment.executeAsyncTask(taxiTask, getActivity());
    }

    void startLoadingBusLocations() {
        if (!currentRoutesIds.isEmpty() && !busLoadingTimerMutex) {
            busLoadingTimerMutex = true;
            busLoadingTimer = new Timer();
            busLoadingTimer.schedule(new GetBusLocationsTask(), 0);
        }
    }

    void continueLoadingBusLocations() {
        if (busLoadingTimerMutex)
            busLoadingTimer.schedule(new GetBusLocationsTask(), 5000);
    }

    void stopLoadingBusLocations() {
        if (busLoadingTimer != null) {
            busLoadingTimer.cancel();
            busLoadingTimerMutex = false;
        }
    }

    private void loadContent(URL url, Parser parser, String checkString)
            throws Exception {
        int i = 0;
        BufferedReader reader = null;
        boolean loaded = false;
        do {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setReadTimeout(15000);
            c.connect();
            reader = new BufferedReader(new InputStreamReader(
                    c.getInputStream()));
            String line = reader.readLine();
            if ((line != null) && (!line.isEmpty())
                    && (line.contains(checkString))) {
                do {
                    parser.parse(line);
                } while ((line = reader.readLine()) != null);
            } else {
                continue;
            }
            loaded = true;
        } while (!loaded && (++i < 3)); // check for empty page
        if (!loaded)
            throw new Exception("Problem loading content");
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("ModelFragment loadContent",
                        "Exception closing HUC reader", e);
            }
        }
    }

    private Integer getColor(int i) {
        if (i > 9)
            i -= 10; // in case of new routes appearing
        Integer color = 0x6A000000; // AARRGGBB
        color += COLORS[i];
        return color;
    }

    private void showProblemToast() {
        try {
            Toast.makeText(getActivity(), getString(R.string.problem),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
        }
    }

    private int getTimeDelay(int targetDelay, Integer route)
            throws ParseException {
        Pattern pattern = Pattern.compile("¹" + route.toString()
                + "[<&](.+?)<br"), pattern2;
        Matcher matcher = pattern.matcher(lastBusSchedule), matcher2;
        String scheduleTime;
        boolean suitableTimeFound = false;
        Calendar calendarNow = new GregorianCalendar();
        if (matcher.find()) {
            // resultTime variable is here because server returns arrival times
            // in wrong order after 00:00
            Date resultTime = new Date(new Date().getTime() + 300 * 60 * 1000), targetTime = new Date(
                    new Date().getTime() + targetDelay * 60000);
            pattern2 = Pattern.compile("(\\d+:\\d+)");
            matcher2 = pattern2.matcher(matcher.group());
            calendarNow.setTime(new Date());
            while (matcher2.find()) {
                Calendar calendarSchedule = new GregorianCalendar();
                scheduleTime = matcher2.group(1);
                calendarSchedule.set(Calendar.HOUR_OF_DAY,
                        Integer.parseInt(scheduleTime.substring(0, 2)));
                calendarSchedule.set(Calendar.MINUTE,
                        Integer.parseInt(scheduleTime.substring(3, 5)));
                // in case of day change, but not in case of ratadubna.ru sent
                // outdated data. (data is usually outdated for a couple of
                // minutes)
                if ((calendarNow.getTimeInMillis() - calendarSchedule
                        .getTimeInMillis()) > 120 * 60000)
                    calendarSchedule.add(Calendar.DATE, 1);
                if (targetTime.before(calendarSchedule.getTime())
                        && resultTime.after(calendarSchedule.getTime())) {
                    resultTime = calendarSchedule.getTime();
                    suitableTimeFound = true;
                }
            }
            if (suitableTimeFound)
                return (int) (resultTime.getTime() - targetTime.getTime());
        }
        return 0;
    }

    private class ContentsLoadTask extends AsyncTask<Context, Void, Void> {
        private Exception e;

        @Override
        protected Void doInBackground(Context... ctxt) {
            try {
                loadContent(new URL(getString(R.string.routes_url)),
                        new BusRoutesListLoader(), "¹");
            } catch (Exception e) {
                this.e = e;
                Log.e(getClass().getSimpleName(),
                        "Exception retrieving bus routes content", e);
            }
            return (null);
        }

        @Override
        public void onPostExecute(Void arg0) {
            if (e != null) {
                showProblemToast();
            }
        }

        private class BusRoutesListLoader implements Parser {
            @Override
            public void parse(String line) throws Exception {
                String[] contents = line.split("\t");
                if (contents.length > 1) {
                    BusRoutes.add(Integer.parseInt(contents[0]), "¹" + contents[1] + " - " + contents[3], Integer.parseInt(contents[1]));
                } else {
                    throw new Exception("Problem in parseBusRoutesList");
                }
            }
        }
    }

    private class GetTaxiPhonesTask extends AsyncTask<Context, Void, Void> {
        private Exception e = null;
        String page = "";

        GetTaxiPhonesTask(String page) {
            this.page = page;
        }

        @Override
        protected Void doInBackground(Context... ctxt) {
            try {
                page = getPage(new URL(page));
                if (page.equals("\n"))
                    throw new Exception("Connection problem");
            } catch (Exception e) {
                this.e = e;
                Log.e(getClass().getSimpleName(),
                        "Exception retrieving taxi phones content", e);
            }
            return (null);
        }

        @Override
        public void onPostExecute(Void arg0) {
            String result = parsePhoneNumbersPage(page);
            if (e == null) {
                String data = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                        + "<style type=\"text/css\">td {white-space: nowrap;}</style>"
                        + result;
                Intent i = new Intent(getActivity(), SimpleContentActivity.class);
                i.putExtra(SimpleContentActivity.EXTRA_DATA, data);
                startActivity(i);
            }
        }

        private String parsePhoneNumbersPage(String page) {
            Pattern pattern = Pattern
                    .compile("<table border=0 width=\"100%\">([\\s\\S]+?)</table>"), pattern2 = Pattern
                    .compile("(8[\\d\\(\\)\\s-]{14,15})");
            Matcher matcher = pattern.matcher(page);
            if (matcher.find()) {
                String phone, result = matcher
                        .group()
                        .replaceAll("(21[\\d-]{7}<br[\\s>/]+)", "")
                        .replaceAll("(\\(49621\\)[\\d\\s-]+<br[\\s>/]+)", "")
                        .replaceAll("\\(9", "8(9")
                        .replaceAll("[\\(\\)]\\s*", "-")
                        .replaceAll("<tr>\\s+<td colspan=5><hr></td>\\s+</tr>",
                                "").replace("lightgrey", "lightblue");
                matcher = pattern2.matcher(result);
                while (matcher.find()) {
                    phone = matcher.group();
                    result = result.replaceAll(phone, "<a href=\"tel:" + phone
                            + "\">" + phone + "</a>");
                }
                return result;
            } else {
                showProblemToast();
                return getString(R.string.connection_problem);
            }
        }

    }

    private class GetBusLocationsTask extends TimerTask {
        private int routeId;

        public void run() {
            for (int id : currentRoutesIds) {
                if (busLoadingTimerMutex) {
                    routeId = id;
                    try {
                        loadContent(
                                new URL(getString(R.string.bus_location_url) + String.valueOf(id)),
                                new BusLocationLoader(), "");
                    } catch (Exception e) {
                    }
                } else {
                    return;
                }
            }
            busLocationUpdateHandler.obtainMessage(1).sendToTarget();
        }

        private class BusLocationLoader implements Parser {
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
                } else
                    throw new Exception("parseBusLocs problem");

            }
        }
    }

    private class GetScheduleTask extends AsyncTask<Context, Void, Void> {
        private Exception e = null;
        private int id;
        StringBuilder result = new StringBuilder();
        Marker marker;

        public GetScheduleTask(int id, Marker marker) {
            this.id = id;
            this.marker = marker;
        }

        @Override
        protected Void doInBackground(Context... ctxt) {
            try {
                loadContent(new URL(getString(R.string.schedule_url) + String.valueOf(id)),
                        new ScheduleLoader(), "<");
            } catch (Exception e) {
                this.e = e;
                Log.e(getClass().getSimpleName(),
                        "Exception retrieving bus schedule content", e);
            }
            return (null);
        }

        @Override
        public void onPostExecute(Void arg0) {
            if (e == null) {
                String strResult = result.toString();
                if ((strResult == null) || (strResult.isEmpty())
                        || (strResult.equals("<b>¹</b> -&nbsp;")))
                    lastBusSchedule = "";
                else
                    lastBusSchedule = strResult;
                marker.setSnippet(lastBusSchedule);
                marker.showInfoWindow();
            } else {
                showProblemToast();
            }
        }

        private class ScheduleLoader implements Parser {

            @Override
            public void parse(String line) throws Exception {
                String tmp;
                line = line.replaceAll(",", ".");
                Pattern pattern = Pattern.compile("([¹:\\d]+)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if ((tmp = matcher.group()).length() == 3)
                        tmp += "&nbsp;&nbsp;";
                    result.append("<b>" + tmp + "</b> -&nbsp;");
                    if (matcher.find()) {
                        result.append("<font  color=\"green\">"
                                + matcher.group() + "</font>");
                        while (matcher.find()) {
                            result.append(" " + matcher.group());
                        }
                        result.append("<br />");
                    }
                }
            }

        }
    }

    private class GetBusRoutesAndMarkersTask extends
            AsyncTask<Context, Void, Void> {
        private Exception e = null;
        PolylineOptions busRoutesOption;
        ArrayList<PolylineOptions> busRoutesOptionsArray = new ArrayList<PolylineOptions>();
        ArrayList<MarkerOptions> busMarkerOptionsArray = new ArrayList<MarkerOptions>();

        @Override
        protected Void doInBackground(Context... ctxt) {
            try {
                busRoutesAndMarkersTaskMutex = true;
                descStopIdMap.clear();
                currentRoutesIds.clear();
                for (Integer i = 0; i < prefs.getInt(ROUTES_ARRAY_SIZE, 0); i++) {
                    if (prefs.getBoolean(i.toString(), false)) {
                        int id = prefs.getInt("id_at_" + i.toString(), 0);
                        busRoutesOption = new PolylineOptions();
                        loadContent(
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

        private class BusRoutesAndMarkersLoader implements Parser {

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

    private interface Parser {
        void parse(String line) throws Exception;
    }
}
