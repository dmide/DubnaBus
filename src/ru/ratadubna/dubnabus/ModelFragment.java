package ru.ratadubna.dubnabus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;

public class ModelFragment extends SherlockFragment {
	private ContentsLoadTask contentsTask = null;
	private SharedPreferences prefs = null;
	private PrefsLoadTask prefsTask = null;
	private boolean routesLoaded = false;
	private static final String ROUTES_URL = "http://www.ratadubna.ru/nav/d.php?o=1";

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		deliverModel();
	}

	synchronized private void deliverModel() {
		if (prefs != null && !BusRoutes.GetRoutes().isEmpty()) {
			((DubnaBusActivity) getActivity()).setupData(prefs);
		} else {
			if (prefs == null && prefsTask == null) {
				prefsTask = new PrefsLoadTask();
				executeAsyncTask(prefsTask, getActivity()
						.getApplicationContext());
			} else if (BusRoutes.GetRoutes().isEmpty() && contentsTask == null) {
				contentsTask = new ContentsLoadTask();
				executeAsyncTask(contentsTask, getActivity()
						.getApplicationContext());
			}
		}
	}

	@TargetApi(11)
	static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task,
			T... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		} else {
			task.execute(params);
		}
	}

	private class ContentsLoadTask extends AsyncTask<Context, Void, Void> {
		private Exception e = null;

		@Override
		protected Void doInBackground(Context... ctxt) {
			BufferedReader reader = null;
			try {
				URL url = new URL(ROUTES_URL);
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
				ParseRoutes(buf.toString());
				if (!routesLoaded) throw new Exception();
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
				deliverModel();
			} else {
				Log.e(getClass().getSimpleName(), "Exception loading contents",
						e);
			}
		}
	}

	private void ParseRoutes(String page) {
		Pattern pattern = Pattern.compile("<li(.*)</li>"), pattern2 = Pattern
				.compile("route-menu-item([0-9]+)"), pattern3 = Pattern
				.compile("title=\"Маршрут (.*)\" name");
		Matcher matcher = pattern.matcher(page);
		Matcher matcher2;
		String line;
		int id;
		while (matcher.find()) {
			line = matcher.group();
			matcher2 = pattern2.matcher(line);
			if (matcher2.find()) 
				id = Integer.parseInt(matcher2.group(1));
			else return;
			matcher2 = pattern3.matcher(line);
			if (matcher2.find())
				BusRoutes.Add(id, matcher2.group(1));
			else return;
		}
		routesLoaded = true;
	}

	private class PrefsLoadTask extends AsyncTask<Context, Void, Void> {
		SharedPreferences localPrefs = null;

		@Override
		protected Void doInBackground(Context... ctxt) {
			localPrefs = PreferenceManager.getDefaultSharedPreferences(ctxt[0]);
			localPrefs.getAll();
			return (null);
		}

		@Override
		public void onPostExecute(Void arg0) {
			ModelFragment.this.prefs = localPrefs;
			ModelFragment.this.prefsTask = null;
			deliverModel();
		}
	}
	
}