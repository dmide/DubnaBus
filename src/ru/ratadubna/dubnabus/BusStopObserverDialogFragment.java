package ru.ratadubna.dubnabus;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class BusStopObserverDialogFragment extends DialogFragment implements
		DialogInterface.OnClickListener {
	private SharedPreferences prefs = null;
	private View form = null;
	private EditText et = null;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (prefs == null) {
			prefs = PreferenceManager
					.getDefaultSharedPreferences(getActivity());
		}
		int delay = prefs.getInt("notificationDelay", 10);
		form = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
		et = (EditText) form.findViewById(R.id.value);
		et.setText(String.valueOf(delay));
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		return (builder.setTitle(R.string.dlg_title).setView(form)
				.setPositiveButton(R.string.dlg_yes, this)
				.setNegativeButton(R.string.dlg_no, null).create());
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		int delay = Integer.parseInt(((EditText) form.findViewById(R.id.value))
				.getText().toString());
		if (delay < 60) {
			prefs.edit().putInt("notificationDelay", delay).apply();
			Entry<Date, Integer> actualDelay = ((DubnaBusActivity) getActivity())
					.getModel().observeBusStop(delay);
			if (actualDelay != null) {
				Calendar actualDelayCal = new GregorianCalendar();
				actualDelayCal.setTime(actualDelay.getKey());
				Toast.makeText(
						getActivity(),
						R.string.min_before_alarm
								+ String.valueOf(actualDelayCal
										.get(Calendar.MINUTE))
								+ R.string.awaiting_bus
								+ actualDelay.getValue().toString(),
						Toast.LENGTH_LONG).show();
				// Toast.makeText(getActivity(),
				// "Минут до оповещения: 10. Ожидается автобус №42",
				// Toast.LENGTH_LONG).show();
				NotificationReceiver.scheduleAlarm(getActivity(),
						actualDelayCal.get(Calendar.MINUTE) * 60000, delay,
						actualDelay.getValue().toString());
				// NotificationReceiver.scheduleAlarm(getActivity(), 10000, 10,
				// "42");
			} else {
				Toast.makeText(
						getActivity(),
						R.string.no_bus,
						Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(getActivity(),
					R.string.waiting_time_limit,
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onDismiss(DialogInterface unused) {
		super.onDismiss(unused);
		Log.d(getClass().getSimpleName(), "Goodbye!");
	}

	@Override
	public void onCancel(DialogInterface unused) {
		super.onCancel(unused);
		Toast.makeText(getActivity(), "cancel pressed", Toast.LENGTH_LONG)
				.show();
	}
}