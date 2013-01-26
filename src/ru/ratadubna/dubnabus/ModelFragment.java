package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ModelFragment extends SherlockFragment {
	private ContentsLoadTask contentsTask = null;
	private boolean routesLoaded = false;
	boolean mapRoutesLoaded = false;
	private static final String ROUTES_URL = "http://www.ratadubna.ru/nav/d.php?o=1";
	static final String ROUTES_ARRAY_SIZE = "routes_array_size";
	private SharedPreferences prefs = null;
	private ArrayList<PolylineOptions> mapRoutes = new ArrayList<PolylineOptions>();
	private ArrayList<MarkerOptions> markers;

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

	ArrayList<MarkerOptions> getMarkers() {
		return markers;
	}

	ArrayList<PolylineOptions> getMapRoutes() {
		return mapRoutes;
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
				.compile("route-menu-item([0-9]+).+title=\"Маршрут (.*)\" name");
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

		public GetRouteMapTask(int id) {
			this.id = id;
		}

		@Override
		protected Void doInBackground(Context... ctxt) {
			BufferedReader reader = null;
			try {
				URL url = new URL("http://ratadubna.ru/nav/d.php?o=2&m="
						+ String.valueOf(id));
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
				String page = buf.toString();
				page = page.replaceAll(",", ".");
				PolylineOptions mapRoute = ParseMapRoute(page);
				mapRoutes.add(mapRoute);
				markers = ParseMapMarkers(page);
				if (mapRoute == null || markers == null)
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
				mapRoutesLoaded = true;
				((DubnaBusActivity)getActivity()).drawRoutes();
			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private PolylineOptions ParseMapRoute(String page) {
		Pattern pattern = Pattern.compile("([0-9]{2}.[0-9]+)");
		Matcher matcher = pattern.matcher(page);
		PolylineOptions mapRoute = new PolylineOptions();
		double lat, lng;
		while (matcher.find()) {
			lat = Double.parseDouble(matcher.group());
			if (matcher.find())
				lng = Double.parseDouble(matcher.group());
			else
				return null;
			mapRoute.add(new LatLng(lat, lng));
		}
		return mapRoute;
	}

	private ArrayList<MarkerOptions> ParseMapMarkers(String page) {
		Pattern pattern = Pattern.compile("(.+[а-яА-Я()])");
		Pattern pattern2 = Pattern
				.compile("([0-9]{2}.[0-9]+)\\s([0-9]{2}.[0-9]+)\\s(.+)");
		Matcher matcher = pattern.matcher(page);
		Matcher matcher2;
		ArrayList<MarkerOptions> markers = new ArrayList<MarkerOptions>();
		double lat, lng;
		String desc;
		while (matcher.find()) {
			matcher2 = pattern2.matcher(matcher.group());
			if (matcher2.find()) {
				lat = Double.parseDouble(matcher2.group(1));
				lng = Double.parseDouble(matcher2.group(2));
				desc = matcher2.group(3);
			} else
				return null;
			markers.add(new MarkerOptions().position(new LatLng(lat, lng))
					.title(desc));
		}
		return markers;
	}

}