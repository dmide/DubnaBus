package ru.ratadubna.dubnabus;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

public class MenuFragment extends SherlockFragment implements
		android.widget.AdapterView.OnItemClickListener,
		android.widget.AdapterView.OnClickListener {
	private ListView lv;
	private Button but;
	private ArrayList<Integer> idArray = new ArrayList<Integer>();
	private SparseBooleanArray positionHide = new SparseBooleanArray();

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View result = inflater.inflate(R.layout.menu, container, false);
		lv = (ListView) result.findViewById(R.id.listView);
		lv.setAdapter(new MenuItemsAdapter(getActivity(),
				android.R.layout.simple_list_item_multiple_choice, BusRoutes
						.GetRoutes()));
		lv.setOnItemClickListener(this);
		but = (Button) result.findViewById(R.id.but);
		but.setOnClickListener(this);
		return (result);
	}

	@Override
	public void onItemClick(android.widget.AdapterView<?> parent, View v,
			int position, long id) {
		CheckedTextView tv = (CheckedTextView) v.findViewById(R.id.checkView);
		positionHide.put(position, !tv.isChecked());
		tv.setChecked(!tv.isChecked());
	}

	public void onClick(View v) {
		for (int i = 0; i < positionHide.size(); i++) {
			if (positionHide.valueAt(i)) {
				idArray.add(BusRoutes.GetRoutes().get(positionHide.keyAt(i)).GetId());
			}
		}
		Intent i = new Intent(getActivity(), DubnaBusActivity.class);
		i.putExtra("idArray", idArray);
		startActivity(i);
	}

	public void onPause() {

		super.onPause();
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		SharedPreferences.Editor editor = sharedPreferences.edit();
		for (Integer i = 0; i < BusRoutes.GetRoutes().size(); i++) {
			editor.putBoolean(i.toString(), positionHide.get(i, false));
		}
		editor.commit();
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		for (Integer i = 0; i < BusRoutes.GetRoutes().size(); i++) {
			positionHide.put(i,
					sharedPreferences.getBoolean(i.toString(), false));
		}
	}

	private class MenuItemsAdapter extends ArrayAdapter<BusRoutes> {
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
					Boolean chk = positionHide.get(position);
					if (chk != null)
						itemView.setChecked(chk);
				}
			}

			return view;
		}

		public int getCount() {
			return items.size();
		}

	}

}