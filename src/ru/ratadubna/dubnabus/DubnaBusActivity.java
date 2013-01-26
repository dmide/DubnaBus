package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class DubnaBusActivity extends SherlockFragmentActivity {
	private GoogleMap mMap;
	private ModelFragment model = null;
	static final String MODEL = "model";
	static final String ROUTES_ARRAY_SIZE = "routes_array_size";
	private SharedPreferences prefs = null;

	@Override
	public void onResume() {
		super.onResume();
		setUpMapIfNeeded();
		DrawRoutes();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getSupportFragmentManager().findFragmentByTag(MODEL) == null) {
			model = new ModelFragment();
			getSupportFragmentManager().beginTransaction().add(model, MODEL)
					.commit();
		} else {
			model = (ModelFragment) getSupportFragmentManager()
					.findFragmentByTag(MODEL);
		}
		setContentView(R.layout.main);
		setUpMapIfNeeded();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		new MenuInflater(this).inflate(R.menu.main, menu);
		return (super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent i = new Intent(this, MenuActivity.class);
			startActivity(i);
			return (true);
		}
		return (super.onOptionsItemSelected(item));
	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
		}
	}

	void DrawRoutes() {
		GetRouteMapTask getRouteMapTask;
		for (Integer i = 0; i < prefs.getInt(ROUTES_ARRAY_SIZE, 0); i++) {
			if (prefs.getBoolean(i.toString(), false)) {
				getRouteMapTask = new GetRouteMapTask(prefs.getInt(
						"id_at_" + i.toString(), 0));
				ModelFragment.executeAsyncTask(getRouteMapTask,
						this.getApplicationContext());
			}
		}
	}

	private class GetRouteMapTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;
		private int id;
		private PolylineOptions mapRoute;
		private ArrayList<MarkerOptions> markers;

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
				mapRoute = ParseMapRoute(page);
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
				mapRoute.color(-(new Random().nextInt(2147483647)));
				mMap.addPolyline(mapRoute);
				for (MarkerOptions marker : markers) {
					mMap.addMarker(marker);
				}
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
		Pattern pattern = Pattern.compile("(.+[à-ÿÀ-ß()])");
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
