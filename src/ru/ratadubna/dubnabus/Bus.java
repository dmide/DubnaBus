package ru.ratadubna.dubnabus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

import android.graphics.Point;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;

public class Bus {
	private String id;
	private int speed;
	private int type;
	private int route;
	private static Date time;
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
				.position(position, 400).bearing(bearing).zIndex(100);
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
	
	static void setTime(String sTime){
		SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
		try {
			time = format.parse(sTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	static Date getTime(){
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

	static boolean isActive(String id) {
		return activeBuses.contains(id);
	}

	static void updateBus(String id, LatLng position, int speed, int bearing) {
		for (Bus bus : busList) {
			if (bus.getId().equals(id)) {
				bus.options.position(position, 400);
				bus.speed = speed;
				bus.options.bearing(bearing);
			}
		}
	}

	static void redrawOnZoomChange(Projection projection) {
		double span = projection.fromScreenLocation(new Point(100,0)).longitude - projection.fromScreenLocation(new Point(0,0)).longitude;
		for (Bus bus : busList) {
			if (bus.isActive()) {
				float tmp = (float)(35000*span);
				bus.overlay.setDimensions(tmp);
			}
		}
	}

}
