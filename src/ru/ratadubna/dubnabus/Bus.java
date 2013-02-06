package ru.ratadubna.dubnabus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

import android.graphics.Point;
import android.location.Location;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Bus {
	private String id;
	private int speed;
	private int type;
	private int route;
	private static Date time;
	private static ArrayList<Bus> busList = new ArrayList<Bus>();
	private static HashSet<String> activeBuses = new HashSet<String>();
	private static final SparseArray<String> busTypes = new SparseArray<String>();
	private BitmapDescriptor image = BitmapDescriptorFactory
			.fromAsset("bus_arrow.png");
	private GroundOverlayOptions groundOverlayOptions;
	private GroundOverlay overlay = null;
	private MarkerOptions markerOptions;
	private Marker marker = null;
	private static float dimensions = 196;

	static {
		busTypes.put(59, "busType59.png");
		busTypes.put(91, "busType91.png");
	}

	Bus(String id, LatLng position, int speed, int bearing, int type, int route) {
		this.id = id;
		this.speed = speed;
		this.type = type;
		this.route = route;
		groundOverlayOptions = new GroundOverlayOptions().image(image)
				.position(position, dimensions).bearing(bearing).zIndex(100);
		String title = "¹" + String.valueOf(BusRoutes.realIdByServiceId(route));
		markerOptions = new MarkerOptions().position(position).title(title)
				.icon(BitmapDescriptorFactory.fromAsset("blank.png"));
	}

	LatLng getPosition() {
		return groundOverlayOptions.getLocation();
	}

	String getId() {
		return id;
	}

	int getRoute() {
		return route;
	}

	float getBearing() {
		return groundOverlayOptions.getBearing();
	}

	GroundOverlayOptions getGroundOverlayOptions() {
		return groundOverlayOptions;
	}

	MarkerOptions getMarkerOptions() {
		return markerOptions;
	}

	void setOverlay(GroundOverlay overlay) {
		this.overlay = overlay;
		activeBuses.add(id);
	}

	void setMarker(Marker marker) {
		this.marker = marker;
	}

	void updateOverlay() {
		overlay.setPosition(groundOverlayOptions.getLocation());
		overlay.setBearing(groundOverlayOptions.getBearing());
	}

	void updateMarker() {
		marker.setPosition(groundOverlayOptions.getLocation());
	}

	boolean isActive() {
		return (overlay != null);
	}

	String getPic() {
		return busTypes.get(type);
	}

	int getSpeed() {
		return speed;
	}

	static void setTime(String sTime) {
		SimpleDateFormat format = new SimpleDateFormat("HH:mm",
				Locale.getDefault());
		try {
			time = format.parse(sTime);
		} catch (ParseException e) {
			Log.e("Bus class", "Exception parsing time from string", e);
		}
	}

	static Date getTime() {
		if (time == null)
			time = new Date();
		return time;
	}

	static void addToList(Bus bus) {
		busList.add(bus);
	}

	static void clearList() {
		busList.clear();
		activeBuses.clear();
	}

	static ArrayList<Bus> getList() {
		return busList;
	}

	static Bus getBusByMarker(Marker marker) {
		for (Bus bus : busList) {
			if (bus.marker.equals(marker))
				return bus;
		}
		return null;
	}

	static boolean isActive(String id) {
		return activeBuses.contains(id);
	}

	static void updateBus(String id, LatLng position, int speed, int bearing) {
		for (Bus bus : busList) {
			if (bus.getId().equals(id)) {
				bus.groundOverlayOptions.position(position, dimensions);
				bus.speed = speed;
				bus.groundOverlayOptions.bearing(bearing);
			}
		}
	}

	static void redrawOnZoomChange(Projection projection) {
		// 28 pix approximately corresponds 196 meters at default zoom.
		// 196 meters, in turn, is just value at which bus-arrow looks fine
		LatLng ll1 = projection.fromScreenLocation(new Point(28, 0)), ll2 = projection
				.fromScreenLocation(new Point(0, 0));
		float[] results = new float[3];
		Location.distanceBetween(ll1.latitude, ll1.longitude, ll2.latitude,
				ll2.longitude, results);
		dimensions = results[0];
		for (Bus bus : busList) {
			if (bus.isActive()) {
				bus.overlay.setDimensions(dimensions);
			}
		}
	}
}
