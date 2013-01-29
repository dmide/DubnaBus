package ru.ratadubna.dubnabus;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class BusLocationReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context ctxt, Intent i) {
		if (i.getAction() == BusLocationService.ACTION_BUS_LOCATION) {
			Intent locServiceIntent = new Intent(ctxt, BusLocationService.class);
			locServiceIntent.putExtra("ids", i.getIntegerArrayListExtra("ids"));
			WakefulIntentService.sendWakefulWork(ctxt, locServiceIntent);
		} else if (i.getAction() == BusLocationService.ACTION_BUS_LOADED) {
			((DubnaBusActivity)ctxt).addBuses();
		}
	}

	static void scheduleAlarm(Context ctxt, ArrayList<Integer> ids) {
		AlarmManager mgr = (AlarmManager) ctxt
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(BusLocationService.ACTION_BUS_LOCATION);
		i.putExtra("ids", ids);
		PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mgr.setRepeating(AlarmManager.RTC_WAKEUP,
				SystemClock.elapsedRealtime(), 5000, pi);
	}
}
