package ru.ratadubna.dubnabus;

import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
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
		setRetainInstance(true);
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
			Entry<Integer, Integer> actualDelay = ((DubnaBusActivity) getActivity())
					.getModel().observeBusStop(delay);
			if (actualDelay != null) {
				Toast.makeText(
						getActivity(),
						getString(R.string.min_before_alarm)
								+ String.valueOf(actualDelay.getKey()/60000)
								+ getString(R.string.awaiting_bus)
								+ actualDelay.getValue().toString(),
						Toast.LENGTH_LONG).show();
				// Toast.makeText(getActivity(),
				// "Минут до оповещения: 10. Ожидается автобус №42",
				// Toast.LENGTH_LONG).show();
				NotificationReceiver.scheduleAlarm(getActivity(),
						actualDelay.getKey(), delay,
						actualDelay.getValue().toString());
				// NotificationReceiver.scheduleAlarm(getActivity(), 10000, 10,
				// "42");
			} else {
				Toast.makeText(getActivity(), getString(R.string.no_bus),
						Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(getActivity(),
					getString(R.string.waiting_time_limit), Toast.LENGTH_LONG)
					.show();
		}
	}

	@Override
	public void onDismiss(DialogInterface unused) {
		super.onDismiss(unused);
	}

	@Override
	public void onCancel(DialogInterface unused) {
		super.onCancel(unused);
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}
	
	@Override
	public int show(FragmentTransaction transaction, String tag) {
	    return super.show(transaction, tag);
	}
}