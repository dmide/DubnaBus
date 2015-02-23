package ru.ratadubna.dubnabus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SimpleContentActivity extends SherlockFragmentActivity {
    public static final String EXTRA_DATA = "data";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple);
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            String file = getIntent().getStringExtra(EXTRA_DATA);
            Fragment f = SimpleContentFragment.newInstance(file);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.webview, f).commit();
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(ModelFragment.TAXI_DIALOG_SHOWED, false) &&
                !DubnaBusActivity.isPackageInstalled(getResources().getString(R.string.taxi_app_package), this)) {
            ModelFragment.showPromoDialog(this, false);
        }
    }
}