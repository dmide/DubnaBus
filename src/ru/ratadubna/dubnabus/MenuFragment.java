package ru.ratadubna.dubnabus;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
		toggle(tv);
	}

	public void toggle(CheckedTextView v) {
		v.setChecked(!v.isChecked());
	}

	public void onClick(View v) {
		SparseBooleanArray sba = lv.getCheckedItemPositions();
		for (int i = 0; i < sba.size(); i++) {
			if (sba.valueAt(i)) {
				idArray.add(BusRoutes.GetRoutes().get(sba.keyAt(i)).GetId());
			}
		}
		Intent i = new Intent(getActivity(), DubnaBusActivity.class);
		i.putExtra("idList", idArray);
		startActivity(i);
	}

}