package ru.ratadubna.dubnabus;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {
	private static final String ACTION_NOTIFICATION = "ru.ratadubna.dubnabus.action.NOTIFICATION";
	private static final int NOTIFY_ID = 1337;

	@Override
	public void onReceive(Context ctxt, Intent i) {
		if (i.getAction().equals(ACTION_NOTIFICATION)) {
			NotificationCompat.Builder builder = new NotificationCompat.Builder(
					ctxt);
			Intent toLaunch = new Intent(ctxt, DubnaBusActivity.class);
			PendingIntent pi = PendingIntent.getActivity(ctxt, 0, toLaunch, 0);
			builder.setAutoCancel(true)
					.setContentIntent(pi)
					.setContentTitle(
							"Автобус №" + i.getStringExtra("bus_number"))
					.setContentText(
							"Прибудет через "
									+ String.valueOf(i.getIntExtra("delay", 0))
									+ " мин.")
					.setSmallIcon(R.drawable.ic_stat_example)
					.setTicker("Автобус").setWhen(System.currentTimeMillis());
			NotificationManager mgr = ((NotificationManager) ctxt
					.getSystemService(Context.NOTIFICATION_SERVICE));
			mgr.notify(NOTIFY_ID, builder.getNotification());
		}
	}

	static void scheduleAlarm(Context ctxt, int actual_delay, int delay,
			String bus_number) {
		AlarmManager mgr = (AlarmManager) ctxt
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(ACTION_NOTIFICATION);
		i.putExtra("delay", delay);
		i.putExtra("bus_number", bus_number);
		PendingIntent pi = PendingIntent.getBroadcast(ctxt, 31337, i,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ actual_delay, pi);
	}
}
