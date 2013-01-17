package ru.ratadubna.dubnabus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

public class DubnaBusActivity extends SherlockFragmentActivity {
	private GoogleMap mMap;
	private ModelFragment model = null;
	private static final String MODEL = "model";
	private SharedPreferences prefs = null;

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

}
