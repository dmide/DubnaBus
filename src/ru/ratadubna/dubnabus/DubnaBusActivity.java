package ru.ratadubna.dubnabus;

import java.util.Random;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class DubnaBusActivity extends SherlockFragmentActivity implements OnMarkerClickListener {
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
			mMap.setInfoWindowAdapter(new ScheduleSnippetAdapter(getLayoutInflater()));
			mMap.setOnMarkerClickListener(this);
			if (!model.mapRoutesLoaded) {
				model.loadMapRoutes();
			}
		}
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		model.loadSchedule(marker);
		return false;
	}
	
	void addSchedule(String content, Marker marker){
		marker.setSnippet(content);
		marker.showInfoWindow();
	}
	
	void addRoute(PolylineOptions mapRoute,int id){
		mapRoute.color(-(new Random().nextInt(2147483647)));
		mMap.addPolyline(mapRoute);
		BusLocationReceiver.scheduleAlarm(getApplicationContext(),id);
	}
	
	Marker addMarker(MarkerOptions maropt){
		return mMap.addMarker(maropt);
	}
}
