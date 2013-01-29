package ru.ratadubna.dubnabus;

import java.util.ArrayList;
import java.util.HashSet;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;

public class Bus {
	private String id;
	private int speed;
	private int type;
	private int route;
	private static ArrayList<Bus> busList = new ArrayList<Bus>();
	private static HashSet<String> activeBuses = new HashSet<String>();
	private BitmapDescriptor image = BitmapDescriptorFactory
			.fromAsset("bus180.gif");
	private GroundOverlayOptions options;
	private GroundOverlay overlay = null;

	Bus(String id, LatLng position, int speed, int bearing, int type, int route) {
		this.id = id;
		this.speed = speed;
		this.type = type;
		this.route = route;
		options = new GroundOverlayOptions().image(image)
				.position(position, 412).bearing(bearing).zIndex(100);
	}

	LatLng getPosition() {
		return options.getLocation();
	}

	String getId() {
		return id;
	}

	int getRoute() {
		return route;
	}

	float getBearing() {
		return options.getBearing();
	}

	GroundOverlayOptions getOptions() {
		return options;
	}

	void setOverlay(GroundOverlay overlay) {
		this.overlay = overlay;
		activeBuses.add(id);
	}

	void updateOverlay() {
		overlay.setPosition(options.getLocation());
		overlay.setBearing(options.getBearing());
	}

	boolean isActive() {
		return (overlay != null);
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

	static boolean isActive(String id) {
		return activeBuses.contains(id);
	}

	static void updateBus(String id, LatLng position, int speed, int bearing) {
		for (Bus bus : busList) {
			if (bus.getId().equals(id)) {
				bus.options.position(position, 412);
				bus.speed = speed;
				bus.options.bearing(bearing);
			}
		}
	}


	static void redrawOnZoomChange(VisibleRegion region) {
		double span = region.farRight.longitude - region.farLeft.longitude;
		for (Bus bus : busList) {
			if (bus.isActive()) {
				float tmp = (float)(7500*span);
				bus.overlay.setDimensions(tmp);
			}
		}
	}

}
