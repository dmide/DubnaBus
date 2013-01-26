package ru.ratadubna.dubnabus;

import java.util.Random;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class DubnaBusActivity extends SherlockFragmentActivity {
	private GoogleMap mMap;
	private ModelFragment model = null;
	static final String MODEL = "model";

	@Override
	public void onResume() {
		super.onResume();
		setUpMapIfNeeded();
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
		if (mMap == null) {
			SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map);
			mapFrag.setRetainInstance(true);
			mMap = mapFrag.getMap();
			if (!model.mapRoutesLoaded) {
				drawRoutes();
			}
		}
	}

	void drawRoutes() {
		if (model.mapRoutesLoaded) {
			for (PolylineOptions mapRoute : model.getMapRoutes()) {
				mapRoute.color(-(new Random().nextInt(2147483647)));
				mMap.addPolyline(mapRoute);
			}
			SparseArray<MarkerOptions> markers = model.getMarkers();
			for (int i = 0; i < markers.size(); i++) {
				mMap.addMarker(markers.valueAt(i));
			}
		} else {
			model.loadMapRoutes();
		}
	}
}
