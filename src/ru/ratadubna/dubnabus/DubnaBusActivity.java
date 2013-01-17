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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class DubnaBusActivity extends SherlockFragmentActivity {
	private GoogleMap mMap;
	private ModelFragment model = null;
	private static final String MODEL = "model";
	private SharedPreferences prefs = null;

	@Override
	public void onResume() {
		super.onResume();
		setUpMapIfNeeded();
		if (getIntent().hasExtra("idList")) {
			ArrayList<Integer> idArray = getIntent().getIntegerArrayListExtra(
					"idList");
			DrawRoutes(idArray);
		}
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
		if (mMap != null) {
			// The Map is verified. It is now safe to manipulate the map.

		}
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

	void setupData(SharedPreferences prefs) {
		this.prefs = prefs;
	}

	void DrawRoutes(ArrayList<Integer> idArray) {
		for (Integer id : idArray) {
			GetRouteMapTask getRouteMapTask = new GetRouteMapTask(id);
			ModelFragment.executeAsyncTask(getRouteMapTask, this
					.getApplicationContext());
		}
	}

	private class GetRouteMapTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;
		private int id;
		private PolylineOptions mapRoute;

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
				mapRoute = ParseMapRoute(buf.toString());
				if (mapRoute == null)
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
			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private PolylineOptions ParseMapRoute(String page) {
		page = page.replaceAll(",", ".");
		Pattern pattern = Pattern.compile("([0-9]{2}.[0-9]+)"), pattern2 = Pattern
				.compile("route-menu-item([0-9]+)");
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
		matcher = pattern.matcher(page);
		return mapRoute;
	}
}
