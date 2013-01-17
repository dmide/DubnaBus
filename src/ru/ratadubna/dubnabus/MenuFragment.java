package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class MenuFragment extends SherlockFragment implements
		android.widget.AdapterView.OnItemClickListener,
		android.widget.AdapterView.OnClickListener {
	private ListView lv;
	private boolean MapRouteLoaded = false;
	private Button but;
	private ArrayList<Integer> idArray;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View result = inflater.inflate(R.layout.menu, container, false);
		lv = (ListView) result.findViewById(R.id.listView);
		lv.setAdapter(new MenuItemsAdapter(getActivity(),
				android.R.layout.simple_list_item_multiple_choice, BusRoutes
						.GetRoutes()));
		lv.setOnItemClickListener(this);
		but = (Button) result.findViewById(R.id.but);
		but.setOnClickListener(this);
		return (result);
	}

	@Override
	public void onItemClick(android.widget.AdapterView<?> parent, View v,
			int position, long id) {
		CheckedTextView tv = (CheckedTextView) v.findViewById(R.id.checkView);
		toggle(tv);
	}

	public void toggle(CheckedTextView v) {
		if (v.isChecked()) {
			v.setChecked(false);
		} else {
			v.setChecked(true);
		}
	}

	public void onClick(View v) {
		SparseBooleanArray sba = lv.getCheckedItemPositions();
		for (int i = 0; i < sba.size(); i++) {
			if (sba.get(i)) {
				idArray.add(BusRoutes.GetRoutes().get(i).GetId());
				GetRouteMapTask getRouteMapTask = new GetRouteMapTask(BusRoutes
						.GetRoutes().get(i).GetId());
				ModelFragment.executeAsyncTask(getRouteMapTask, getActivity()
						.getApplicationContext());
			}

		}
		Intent i = new Intent(getActivity(), DubnaBusActivity.class);
		i.putExtra("idList", idArray);
		startActivity(i);
	}

	private class GetRouteMapTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;
		private int id;

		public GetRouteMapTask(int id) {
			this.id = id;
		}

		@Override
		protected Void doInBackground(Context... ctxt) {
			BufferedReader reader = null;
			try {
				URL url = new URL("http://ratadubna.ru/nav/d.php?o=2&m="
						+ String.valueOf(id));
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
				ParseMapRoute(buf.toString());
				if (!MapRouteLoaded)
					throw new Exception();
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(),
						"Exception retrieving bus routes content", e);
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
			return (null);
		}

		@Override
		public void onPostExecute(Void arg0) {
			if (e == null) {

			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private void ParseMapRoute(String page) {
		Pattern pattern = Pattern.compile("([0-9]{2}.[0-9]+)"), pattern2 = Pattern
				.compile("route-menu-item([0-9]+)");
		Matcher matcher = pattern.matcher(page);
		PolylineOptions mapRoute = new PolylineOptions();
		int lat, lng;
		while (matcher.find()) {
			lat = Integer.parseInt(matcher.group());
			if (matcher.find())
				lng = Integer.parseInt(matcher.group());
			else
				return;
			mapRoute.add(new LatLng(lat, lng));
		}
		mapRoute.color(Color.RED);
		matcher = pattern.matcher(page);

		MapRouteLoaded = true;
	}

}