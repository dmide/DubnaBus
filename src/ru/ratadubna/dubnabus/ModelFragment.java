package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
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
import com.sun.xml.internal.ws.util.StringUtils;

public class ModelFragment extends SherlockFragment {
	private ContentsLoadTask contentsTask = null;
	private boolean routesLoaded = false;
	boolean mapRoutesLoaded = false;
	private static final String ROUTES_URL = "http://www.ratadubna.ru/nav/d.php?o=1";
	private static final String MAP_ROUTES_URL = "http://ratadubna.ru/nav/d.php?o=2&m=";
	private static final String SCHEDULE_URL = "http://ratadubna.ru/nav/d.php?o=5&s=";
	static final String ROUTES_ARRAY_SIZE = "routes_array_size";
	private SharedPreferences prefs = null;
	private HashMap<String, Integer> descArray = new HashMap<String, Integer>();

	void loadMapRoutes() {
		GetRouteMapTask getRouteMapTask;
		for (Integer i = 0; i < prefs.getInt(ROUTES_ARRAY_SIZE, 0); i++) {
			if (prefs.getBoolean(i.toString(), false)) {
				getRouteMapTask = new GetRouteMapTask(prefs.getInt(
						"id_at_" + i.toString(), 0));
				executeAsyncTask(getRouteMapTask, getActivity()
						.getApplicationContext());
			}
		}
	}

	void loadSchedule(Marker marker) {
		int id = descArray.get(marker.getTitle());
		GetScheduleTask getScheduleTask = new GetScheduleTask(id, marker);
		executeAsyncTask(getScheduleTask, getActivity().getApplicationContext());
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

	private class ContentsLoadTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;

		@Override
		protected Void doInBackground(Context... ctxt) {
			BufferedReader reader = null;
			try {
				URL url = new URL(ROUTES_URL);
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				c.setRequestMethod("GET");
				c.setReadTimeout(15000);
				c.connect();
				reader = new BufferedReader(new InputStreamReader(
						c.getInputStream()));
				StringBuilder buf = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					buf.append(line + "\n");
				}
				ParseRoutes(buf.toString());
				if (!routesLoaded)
					throw new Exception();
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(),
						"Exception retrieving bus routes content", e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.e(getClass().getSimpleName(),
								"Exception closing HUC reader", e);
					}
				}
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
				.compile("route-menu-item([0-9]+).+title=\"Ìàðøðóò (.*)\" name");
		Matcher matcher = pattern.matcher(page);
		Matcher matcher2;
		int id;
		while (matcher.find()) {
			matcher2 = pattern2.matcher(matcher.group());
			if (matcher2.find()) {
				id = Integer.parseInt(matcher2.group(1));
				BusRoutes.Add(id, matcher2.group(2));
			} else
				return;
		}
		routesLoaded = true;
	}

	private class GetRouteMapTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;
		private int id;
		String page = "";

		public GetRouteMapTask(int id) {
			this.id = id;
		}

		@Override
		protected Void doInBackground(Context... ctxt) {
			BufferedReader reader = null;
			try {
				URL url = new URL(MAP_ROUTES_URL + String.valueOf(id));
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				c.setRequestMethod("GET");
				c.setReadTimeout(15000);
				c.connect();
				reader = new BufferedReader(new InputStreamReader(
						c.getInputStream()));
				StringBuilder buf = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					buf.append(line + "\n");
				}
				page = buf.toString();
				page = page.replaceAll(",", ".");
				if (!page.matches("(.+[à-ÿÀ-ß()]\\s[0-9]+)"))
					throw new Exception("Connection problem");
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(),
						"Exception retrieving bus routes content", e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.e(getClass().getSimpleName(),
								"Exception closing HUC reader", e);
					}
				}
			}
			return (null);
		}

		@Override
		public void onPostExecute(Void arg0) {
			if (e == null) {
				parseMapMarkers(page);
				parseMapRoute(page,id);
				mapRoutesLoaded = true;
			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private void parseMapRoute(String page,int id) {
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
		((DubnaBusActivity) getActivity()).addRoute(mapRoute,id);
	}

	private void parseMapMarkers(String page) {
		Pattern pattern = Pattern.compile("(.+[à-ÿÀ-ß()]\\s[0-9]+)");
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
			if (!descArray.containsValue(id)) {
				((DubnaBusActivity) getActivity())
						.addMarker(new MarkerOptions().position(
								new LatLng(lat, lng)).title(desc));
				descArray.put(desc, id);
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
			BufferedReader reader = null;
			try {
				URL url = new URL(SCHEDULE_URL + String.valueOf(id));
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				c.setRequestMethod("GET");
				c.setReadTimeout(15000);
				c.connect();
				reader = new BufferedReader(new InputStreamReader(
						c.getInputStream()));
				StringBuilder buf = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					buf.append(line + "\n");
				}
				page = buf.toString();
				page = page.replaceAll(",", ".");
				if (!page.matches("(<li>.+</li>)"))
					throw new Exception("Connection problem");
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(),
						"Exception retrieving bus schedule content", e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.e(getClass().getSimpleName(),
								"Exception closing HUC reader", e);
					}
				}
			}
			return (null);
		}

		@Override
		public void onPostExecute(Void arg0) {
			if (e == null) {
				((DubnaBusActivity) getActivity()).addSchedule(
						parseSchedule(page), marker);
			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private String parseSchedule(String page) {
		Pattern pattern = Pattern.compile("(<li>.+</li>)");
		Pattern pattern2 = Pattern.compile("([¹:\\d]+)");
		Matcher matcher = pattern.matcher(page);
		Matcher matcher2;
		StringBuffer result = new StringBuffer();
		String tmp;
		while (matcher.find()) {
			matcher2 = pattern2.matcher(matcher.group());
			matcher2.find();
			if ((tmp = matcher2.group()).length() == 3)
				tmp+="&nbsp;&nbsp;";
			result.append("<b>" + tmp + "</b> -");
			matcher2.find();
			result.append("<font  color=\"green\">" + matcher2.group() + "</font>");
			while (matcher2.find()) {
				result.append(" " + matcher2.group());
			}
			result.append("<br />");
		}
		return result.toString();
	}
}