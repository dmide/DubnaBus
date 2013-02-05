package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ModelFragment extends SherlockFragment {
	private ContentsLoadTask contentsTask = null;
	boolean mapRoutesLoaded = false;
	private static final String ROUTES_URL = "http://www.ratadubna.ru/nav/d.php?o=1";
	private static final String MAP_ROUTES_URL = "http://ratadubna.ru/nav/d.php?o=2&m=";
	private static final String SCHEDULE_URL = "http://ratadubna.ru/nav/d.php?o=5&s=";
	static final String ROUTES_ARRAY_SIZE = "routes_array_size";
	private SharedPreferences prefs = null;
	private HashMap<String, Integer> descStopIdMap = new HashMap<String, Integer>();
	private String lastBusSchedule;

	void loadMapRoutes() {
		GetRouteMapTask getRouteMapTask;
		getRouteMapTask = new GetRouteMapTask();
		executeAsyncTask(getRouteMapTask, getActivity().getApplicationContext());
	}

	void processMarker(Marker marker) {
		Integer id;
		if ((id = descStopIdMap.get(marker.getTitle())) != null) {
			GetScheduleTask getScheduleTask = new GetScheduleTask(id, marker);
			executeAsyncTask(getScheduleTask, getActivity()
					.getApplicationContext());
		} 
	}

	Date getTimeDelay(int targetDelay, Integer route) throws ParseException {
		Pattern pattern = Pattern.compile("№" + route.toString()
				+ "[<&](.+?)<br"), pattern2;
		Matcher matcher = pattern.matcher(lastBusSchedule), matcher2;
		// resultTime variable is here because server returns arrival times in
		// wrong order after 00:00
		if (matcher.find()) {
			Date newTime, resultTime = new Date(
					Bus.getTime().getTime() + 3600000), targetTime = new Date(
					Bus.getTime().getTime() + targetDelay * 60000);
			pattern2 = Pattern.compile("(\\d+:\\d+)");
			matcher2 = pattern2.matcher(matcher.group());
			SimpleDateFormat format = new SimpleDateFormat("HH:mm",
					Locale.getDefault());
			while (matcher2.find()) {
				newTime = format.parse(matcher2.group(1));
				if (targetTime.before(newTime) && resultTime.after(newTime))
					resultTime = newTime;
			}
			return new Date(resultTime.getTime() - targetTime.getTime());
		}
		return null;
	}

	Entry<Date, Integer> observeBusStop(int targetDelay) {
		TreeMap<Date, Integer> delays = new TreeMap<Date, Integer>();
		Date timeDelay;
		int routeRealId;
		for (Integer i = 0; i < BusRoutes.GetRoutes().size(); i++) {
			if (prefs.getBoolean(i.toString(), false)) {
				try {
					routeRealId = BusRoutes.GetRoutes().get(i).getRouteRealId();
					if ((timeDelay = getTimeDelay(targetDelay, routeRealId)) != null)
						delays.put(timeDelay, routeRealId);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return delays.firstEntry();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (prefs == null) {
			prefs = PreferenceManager
					.getDefaultSharedPreferences(getActivity());
		}
		setRetainInstance(true);
		deliverModel();
	}

	synchronized private void deliverModel() {
		if (BusRoutes.GetRoutes().isEmpty() && contentsTask == null) {
			contentsTask = new ContentsLoadTask();
			executeAsyncTask(contentsTask, getActivity()
					.getApplicationContext());
		}
	}

	@TargetApi(11)
	static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task,
			T... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		} else {
			task.execute(params);
		}
	}

	private String loadPage(URL url) throws Exception {
		BufferedReader reader = null;
		HttpURLConnection c = (HttpURLConnection) url.openConnection();
		c.setRequestMethod("GET");
		c.setReadTimeout(15000);
		c.connect();
		reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
		StringBuilder buf = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			buf.append(line + "\n");
		}
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(),
						"Exception closing HUC reader", e);
			}
		}
		return buf.toString();
	}

	private class ContentsLoadTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;

		@Override
		protected Void doInBackground(Context... ctxt) {
			try {
				String page = loadPage(new URL(ROUTES_URL));
				if (!page.contains("<li"))
					throw new Exception("Connection problem");
				ParseRoutes(page);
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(),
						"Exception retrieving bus routes content", e);
			}
			return (null);
		}

		@Override
		public void onPostExecute(Void arg0) {
			if (e == null) {
				deliverModel();
			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private void ParseRoutes(String page) {
		Pattern pattern = Pattern.compile("<li(.*)</li>"), pattern2 = Pattern
				.compile("route-menu-item([0-9]+).+title=\"Маршрут (.*)\" name"), pattern3 = Pattern
				.compile("№(\\d+)");
		Matcher matcher = pattern.matcher(page);
		Matcher matcher2;
		int id;
		String text;
		while (matcher.find()) {
			matcher2 = pattern2.matcher(matcher.group());
			if (matcher2.find()) {
				id = Integer.parseInt(matcher2.group(1));
				text = matcher2.group(2);
				matcher2 = pattern3.matcher(text);
				matcher2.find();
				BusRoutes.add(id, text, Integer.parseInt(matcher2.group(1)));
			} else
				return;
		}
	}

	private class GetRouteMapTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;
		ArrayList<String> pages = new ArrayList<String>();
		ArrayList<Integer> ids = new ArrayList<Integer>();

		@Override
		protected Void doInBackground(Context... ctxt) {
			try {
				for (Integer i = 0; i < prefs.getInt(ROUTES_ARRAY_SIZE, 0); i++) {
					if (prefs.getBoolean(i.toString(), false)) {
						int id = prefs.getInt("id_at_" + i.toString(), 0);
						String page = loadPage(
								new URL(MAP_ROUTES_URL + String.valueOf(id)))
								.replaceAll(",", ".");
						pages.add(page);
						ids.add(id);
						if (!page.contains("56."))
							throw new Exception("Connection problem");
					}
				}
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(),
						"Exception retrieving bus routes content", e);
			}
			return (null);
		}

		@Override
		public void onPostExecute(Void arg0) {
			if (e == null) {
				for (int i = 0; i < pages.size(); i++) {
					parseMapMarkers(pages.get(i), ids.get(i));
					parseMapRoute(pages.get(i));
				}
				mapRoutesLoaded = true;
				if (!ids.isEmpty()) {
					BusLocationReceiver.scheduleAlarm(getActivity()
							.getApplicationContext(), ids);
				}
			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private void parseMapRoute(String page) {
		Pattern pattern = Pattern.compile("([0-9]{2}.[0-9]+)");
		Matcher matcher = pattern.matcher(page);
		PolylineOptions mapRoute = new PolylineOptions();
		double lat, lng;
		while (matcher.find()) {
			lat = Double.parseDouble(matcher.group());
			if (matcher.find())
				lng = Double.parseDouble(matcher.group());
			else
				return;
			mapRoute.add(new LatLng(lat, lng));
		}
		((DubnaBusActivity) getActivity()).addRoute(mapRoute);
	}

	private void parseMapMarkers(String page, int routeId) {
		Pattern pattern = Pattern.compile("(.+\\s\\w+\\s)");
		Pattern pattern2 = Pattern
				.compile("([0-9]{2}.[0-9]+)\\s+([0-9]{2}.[0-9]+)\\s+(.+)\\s+([0-9]+)");
		Matcher matcher = pattern.matcher(page);
		Matcher matcher2;
		double lat, lng;
		int id;
		String desc;
		while (matcher.find()) {
			matcher2 = pattern2.matcher(matcher.group());
			if (matcher2.find()) {
				lat = Double.parseDouble(matcher2.group(1));
				lng = Double.parseDouble(matcher2.group(2));
				desc = matcher2.group(3);
				id = Integer.parseInt(matcher2.group(4));
			} else
				return;
			if (!descStopIdMap.containsValue(id)) {
				((DubnaBusActivity) getActivity())
						.addMarker(new MarkerOptions().position(
								new LatLng(lat, lng)).title(desc));
				descStopIdMap.put(desc, id);
			}
		}
	}

	private class GetScheduleTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;
		private int id;
		String page = "";
		Marker marker;

		public GetScheduleTask(int id, Marker marker) {
			this.id = id;
			this.marker = marker;
		}

		@Override
		protected Void doInBackground(Context... ctxt) {
			try {
				page = loadPage(new URL(SCHEDULE_URL + String.valueOf(id)))
						.replaceAll(",", ".");
				if (!page.contains("<li"))
					throw new Exception("Connection problem");
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(),
						"Exception retrieving bus schedule content", e);
			}
			return (null);
		}

		@Override
		public void onPostExecute(Void arg0) {
			if (e == null) {
				lastBusSchedule = parseSchedule(page);
				((DubnaBusActivity) getActivity()).setBusStopSnippet(lastBusSchedule,
						marker);
			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private String parseSchedule(String page) {
		Pattern pattern = Pattern.compile("(<li>.+</li>)");
		Pattern pattern2 = Pattern.compile("([№:\\d]+)");
		Matcher matcher = pattern.matcher(page);
		Matcher matcher2;
		StringBuffer result = new StringBuffer();
		String tmp;
		while (matcher.find()) {
			matcher2 = pattern2.matcher(matcher.group());
			if (matcher2.find()) {
				if ((tmp = matcher2.group()).length() == 3)
					tmp += "&nbsp;&nbsp;";
				result.append("<b>" + tmp + "</b> -");
				if (matcher2.find()) {
					result.append("<font  color=\"green\">" + matcher2.group()
							+ "</font>");
					while (matcher2.find()) {
						result.append(" " + matcher2.group());
					}
					result.append("<br />");
				}
			}
		}
		String strResult = result.toString();
		if (strResult.equals("<b>№</b> -"))
			return "";
		else
			return strResult;
	}
}