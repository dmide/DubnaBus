package ru.ratadubna.dubnabus;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class MenuActivity extends SherlockFragmentActivity {
	private static final String MENU = "menu";
	private MenuFragment menu;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getSupportFragmentManager().findFragmentByTag(MENU) == null) {
			menu = new MenuFragment();
			getSupportFragmentManager().beginTransaction()
					.add(android.R.id.content, menu, MENU).commit();
		} else {
			menu = (MenuFragment) getSupportFragmentManager()
					.findFragmentByTag(MENU);
			getSupportFragmentManager().beginTransaction()
					.add(android.R.id.content, menu, MENU).commit();
		}
	}
}