package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class BusLocationService extends WakefulIntentService {
	private static final String BUS_LOCATION_URL = "http://ratadubna.ru/nav/d.php?o=3&m=";
	private int id;

	public BusLocationService() {
		super("BusLocationService");
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		BufferedReader reader = null;
		id = intent.getIntExtra("id", 0);
		try {
			URL url = new URL(BUS_LOCATION_URL);
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
			//checkDownloadInfo(buf.toString());
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
}
