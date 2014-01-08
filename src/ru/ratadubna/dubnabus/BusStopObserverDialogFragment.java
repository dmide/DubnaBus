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
import android.widget.Toast;

public class BusStopObserverDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {
    private static final String NOTIFICATION_DELAY = "notificationDelay";
    private static final String KEY_TITLE = "title";
    private SharedPreferences prefs;
    private View form;

    public static BusStopObserverDialogFragment newInstance(String title) {
        BusStopObserverDialogFragment f = new BusStopObserverDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        f.setArguments(args);
        return (f);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (prefs == null) {
            prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
        }
        setRetainInstance(true);
        int delay = prefs.getInt(NOTIFICATION_DELAY, 10);
        form = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
        NumberPicker nb = (NumberPicker) form.findViewById(R.id.numberpicker);
        nb.setCurrent(delay);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return (builder.setTitle(R.string.dlg_title).setView(form)
                .setPositiveButton(R.string.dlg_yes, this)
                .setNegativeButton(R.string.dlg_no, null).create());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int delay = ((NumberPicker) form.findViewById(R.id.numberpicker)).getCurrent();
        prefs.edit().putInt(NOTIFICATION_DELAY, delay).apply();
        Entry<Integer, Integer> actualDelay = ((DubnaBusActivity) getActivity())
                .getModel().getNearestBusDelay(delay);
        if (actualDelay != null) {
            Toast.makeText(
                    getActivity(),
                    getString(R.string.min_before_alarm)
                            + String.valueOf(actualDelay.getKey() / 60000)
                            + getString(R.string.awaiting_bus)
                            + actualDelay.getValue().toString(),
                    Toast.LENGTH_LONG).show();
            NotificationReceiver.scheduleAlarm(getActivity(), actualDelay
                    .getKey(), delay, actualDelay.getValue().toString(), getArguments().getString(KEY_TITLE));
        } else {
            Toast.makeText(getActivity(), getString(R.string.no_bus),
                    Toast.LENGTH_LONG).show();
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
    public void onResume() {
        super.onResume();
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        return super.show(transaction, tag);
    }
}