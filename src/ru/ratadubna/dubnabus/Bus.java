package ru.ratadubna.dubnabus;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Fragment;
import android.graphics.Point;
import android.location.Location;
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
	private int speed, type, route, routeNum;
	private GroundOverlayOptions groundOverlayOptions;
	private GroundOverlay overlay = null;
	private MarkerOptions markerOptions;
	private Marker marker = null;
	private String time = "";
	private static ArrayList<Bus> busList = new ArrayList<Bus>();
	private static HashSet<String> activeBuses = new HashSet<String>();
	private static float dimensions = 196;
	private static final SparseArray<String> busTypes = new SparseArray<String>();
	private static final BitmapDescriptor image = BitmapDescriptorFactory
			.fromAsset("bus_arrow.png");

	static {
		busTypes.put(59, "busType59.png");
		busTypes.put(91, "busType91.png");
	}

	Bus(String id, LatLng position, int speed, int bearing, int type,
			int route, String time, int routeNum) {
        this.routeNum = routeNum;
		this.time = time;
		this.id = id;
		this.speed = speed;
		this.type = type;
		this.route = route;
		groundOverlayOptions = new GroundOverlayOptions().image(image)
				.position(position, dimensions).bearing(bearing).zIndex(1);
		String title = ModelFragment.NUMBER_SYMBOL + routeNum;
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
		this.overlay.setDimensions(dimensions);
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

	String getTime() {
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

	static void updateBus(String id, LatLng position, int speed, int bearing,
			String time) {
		for (Bus bus : busList) {
			if (bus.getId().equals(id)) {
				bus.time = time;
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
