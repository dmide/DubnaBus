package ru.ratadubna.dubnabus;

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
		Intent locServiceIntent = new Intent(ctxt, BusLocationService.class);
		int tmp = i.getIntExtra("id", 0);
		locServiceIntent.putExtra("id", i.getIntExtra("id", 0));
		WakefulIntentService.sendWakefulWork(ctxt, locServiceIntent);
	}

	static void scheduleAlarm(Context ctxt, int id) {
		AlarmManager mgr = (AlarmManager) ctxt
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(ctxt, BusLocationReceiver.class);
		i.putExtra("id", id);
		PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, 0);
		mgr.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(),
				5000, pi);
	}
}
