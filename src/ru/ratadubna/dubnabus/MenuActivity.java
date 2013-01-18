package ru.ratadubna.dubnabus;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

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
	public class MenuItemsAdapter extends ArrayAdapter<BusRoutes> {
		private ArrayList<BusRoutes> items;
		private Context context;

		public MenuItemsAdapter(Context context, int textViewResourceId,
				ArrayList<BusRoutes> items) {
			super(context, textViewResourceId, items);
			this.context = context;
			this.items = items;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.row, null);
			}

			BusRoutes item = items.get(position);
			if (item != null) {
				CheckedTextView itemView = (CheckedTextView) view
						.findViewById(R.id.checkView);
				if (itemView != null) {
					itemView.setText(item.GetDesc());
					itemView.setChecked(true);
				}
			}

			return view;
		}

		public int getCount() {
			return items.size();
		}


	}
}