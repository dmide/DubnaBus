package ru.ratadubna.dubnabus;

import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class DubnaBusActivity extends SherlockFragmentActivity implements
		OnMarkerClickListener, OnCameraChangeListener,
		OnInfoWindowClickListener {
	private GoogleMap mMap;
	private ModelFragment model = null;
	private BusStopObserverDialogFragment dialog = null;
	private BusLocationReceiver receiver = new BusLocationReceiver();
	private boolean noSchedule = false;
	static final String MODEL = "model";
	static final String DIALOG = "dialog";
	private static Context context;
	private final String trainsDMurl = "http://m.rasp.yandex.ru/search?toName=Москва&fromName=Дубна&search_type=suburban&fromId=c215";
	private final String trainsMDurl = "http://m.rasp.yandex.ru/search?toName=Дубна&toId=c215&fromName=Москва&search_type=suburban";
	private final String busesDMurl = "http://m.rasp.yandex.ru/search?toName=Москва&fromName=Дубна&search_type=bus&fromId=c215";
	private final String busesMDurl = "http://m.rasp.yandex.ru/search?toName=Дубна&toId=c215&fromName=Москва&search_type=bus";
	public static boolean reloadOverlays;
	private Random random;

	static Context getCtxt() {
		return context;
	}

	@Override
	public void onResume() {
		super.onResume();
		setUpMapIfNeeded();
		IntentFilter f = new IntentFilter(
				BusLocationService.ACTION_BUS_LOCATION);
		f.addAction(BusLocationService.ACTION_BUS_LOADED);
		f.setPriority(1000);
		registerReceiver(receiver, f);
	}

	@Override
	public void onPause() {
		unregisterReceiver(receiver);
		AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(BusLocationService.ACTION_BUS_LOCATION);
		PendingIntent pi = PendingIntent.getBroadcast(this, 1337, i,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mgr.cancel(pi);
		super.onPause();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		random = new Random();
		context = getApplicationContext();
		setTheme(R.style.Theme_Sherlock_Light);
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
		case R.id.route_selection:
			Intent i = new Intent(this, MenuActivity.class);
			startActivity(i);
			return (true);
		case R.id.trainsDM:
			i = new Intent(this, SimpleContentActivity.class);
			i.putExtra(SimpleContentActivity.EXTRA_DATA, trainsDMurl);
			startActivity(i);
			return (true);
		case R.id.trainsMD:
			i = new Intent(this, SimpleContentActivity.class);
			i.putExtra(SimpleContentActivity.EXTRA_DATA, trainsMDurl);
			startActivity(i);
			return (true);
		case R.id.busesDM:
			i = new Intent(this, SimpleContentActivity.class);
			i.putExtra(SimpleContentActivity.EXTRA_DATA, busesDMurl);
			startActivity(i);
			return (true);
		case R.id.busesMD:
			i = new Intent(this, SimpleContentActivity.class);
			i.putExtra(SimpleContentActivity.EXTRA_DATA, busesMDurl);
			startActivity(i);
			return (true);
		}
		return (super.onOptionsItemSelected(item));
	}

	/*
	 * @Override public boolean onKeyDown(int keycode, KeyEvent e) { switch
	 * (keycode) { case KeyEvent.KEYCODE_MENU: Intent i = new Intent(this,
	 * MenuActivity.class); startActivity(i); return (true); } return
	 * super.onKeyDown(keycode, e); }
	 */

	private void setUpMapIfNeeded() {
		if (mMap == null) {
			reloadOverlays = true; // to redraw buses in case of activity reopen
									// (buses instances are in memory, mMap is
									// null)
			SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map);
			mapFrag.setRetainInstance(true);
			mMap = mapFrag.getMap();
			mMap.setInfoWindowAdapter(new ScheduleSnippetAdapter(
					getLayoutInflater()));
			mMap.setOnMarkerClickListener(this);
			mMap.setOnCameraChangeListener(this);
			mMap.setOnInfoWindowClickListener(this);
			model.loadMapRoutes();
		} else if (reloadOverlays) {
			mMap.clear();
			model.loadMapRoutes();
		}
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		if (marker.getTitle().matches("(№\\d{1,3})")) {
			setBusSnippet(marker);
		} else {
			model.processMarker(marker);
		}
		return false;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		if (!marker.getTitle().matches("(№\\d{1,3})") && !noSchedule) {
			if (getSupportFragmentManager().findFragmentByTag(DIALOG) == null) {
				dialog = new BusStopObserverDialogFragment();
				getSupportFragmentManager().beginTransaction()
						.add(dialog, DIALOG).commit();
			} else {
				dialog = (BusStopObserverDialogFragment) getSupportFragmentManager()
						.findFragmentByTag(DIALOG);
			}
		}
	}

	ModelFragment getModel() {
		return model;
	}

	void setBusStopSnippet(String schedule, Marker marker) {
		noSchedule = schedule.isEmpty();
		marker.setSnippet(schedule);
		marker.showInfoWindow();
	}

	void setBusSnippet(Marker marker) {
		Bus bus = Bus.getBusByMarker(marker);
		marker.setSnippet("<img src=\"file:///android_res/drawable/"
				+ bus.getPic() + "\">" + String.valueOf(bus.getSpeed())
				+ getString(R.string.kmph));
		marker.showInfoWindow();
	}

	void addRoute(PolylineOptions mapRoute) {
		int color = 0x6F000000; //AARRGGBB
		for (int i = 0; i<3; i++){
			color+= (random.nextInt(150)+105) << (i*8);
		}
		//color += new Random().nextInt(8388608);
		mapRoute.color(color);
		mMap.addPolyline(mapRoute);
	}

	Marker addMarker(MarkerOptions maropt) {
		return mMap.addMarker(maropt);
	}

	void addBuses() {
		for (Bus bus : Bus.getList()) {
			if (bus.isActive() && !reloadOverlays) {
				bus.updateOverlay();
				bus.updateMarker();
			} else {
				bus.setOverlay(mMap.addGroundOverlay(bus
						.getGroundOverlayOptions()));
				Marker marker = mMap.addMarker(bus.getMarkerOptions());
				marker.setVisible(false);
				bus.setMarker(marker);
			}
		}
		reloadOverlays = false;
	}

	@Override
	public void onCameraChange(CameraPosition position) {
		Bus.redrawOnZoomChange(mMap.getProjection());
	}

}
