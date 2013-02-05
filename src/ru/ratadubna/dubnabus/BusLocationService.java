package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.gms.maps.model.LatLng;

public class BusLocationService extends WakefulIntentService {
	private static final String BUS_LOCATION_URL = "http://ratadubna.ru/nav/d.php?o=3&m=";
	public static final String ACTION_BUS_LOCATION = "ru.ratadubna.dubnabus.action.BUS_LOCATION";
	public static final String ACTION_BUS_LOADED = "ru.ratadubna.dubnabus.action.BUS_LOADED";

	public BusLocationService() {
		super("BusLocationService");
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		BufferedReader reader = null;
		ArrayList<Integer> ids = intent.getIntegerArrayListExtra("ids");
		try {
			for (int id : ids) {
				URL url = new URL(BUS_LOCATION_URL + String.valueOf(id));
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
				parseBusLocs(buf.toString().replaceAll(",", "."), id);
			}
			intent = new Intent(ACTION_BUS_LOADED);
			intent.setPackage(getPackageName());
			sendBroadcast(intent);
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(),
					"Exception retrieving update info", e);
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
	}

	private void parseBusLocs(String page, int id) {
		String[] strings = page.split("\n");
		String time = "";
		for (String str : strings) {
			String[] contents = str.split("\\s");
			if (contents.length == 8) {
				if (Bus.isActive(contents[1])) {
					Bus.updateBus(
							contents[1],
							new LatLng(Double.parseDouble(contents[4]), Double
									.parseDouble(contents[5])), Integer
									.parseInt(contents[6]), Integer
									.parseInt(contents[7]));
				} else {
					Bus.addToList(new Bus(contents[1], new LatLng(Double
							.parseDouble(contents[4]), Double
							.parseDouble(contents[5])), Integer
							.parseInt(contents[6]), Integer
							.parseInt(contents[7]), Integer
							.parseInt(contents[0]), id));
				}
			} else
				return;
		}
		int tmp = page.indexOf(":");
		if (tmp > 0) {
			time = page.substring(tmp - 2, tmp + 3);
		}
		Bus.setTime(time);
	}
}