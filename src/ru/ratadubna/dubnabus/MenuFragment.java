package ru.ratadubna.dubnabus;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

public class MenuFragment extends SherlockFragment implements
        android.widget.AdapterView.OnItemClickListener {
    private SparseBooleanArray activeRoutes = new SparseBooleanArray();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.menu, container, false);
        ListView lv = (ListView) result.findViewById(R.id.listView);
        lv.setAdapter(new MenuItemsAdapter(getActivity(),
                android.R.layout.simple_list_item_multiple_choice, BusRoute
                .getRoutesArray()));
        lv.setOnItemClickListener(this);
        return (result);
    }

    @Override
    public void onItemClick(android.widget.AdapterView<?> parent, View v,
                            int position, long id) {
        CheckedTextView tv = (CheckedTextView) v.findViewById(R.id.checkView);
        activeRoutes.put(position, !tv.isChecked());
        tv.setChecked(!tv.isChecked());
    }

    @Override
    public void onResume() {
        super.onResume();
        for (int i = 0; i < BusRoute.getRoutesArraySize(); i++) {
            activeRoutes.put(i, BusRoute.getRoute(i).isActive());
        }
    }

    void saveSelection() {
        for (int i = 0; i < BusRoute.getRoutesArraySize(); i++) {
            BusRoute.getRoute(i).setActive(activeRoutes.get(i, false));
        }
    }

    private class MenuItemsAdapter extends ArrayAdapter<BusRoute> {
        private final ArrayList<BusRoute> items;
        private final Context context;

        public MenuItemsAdapter(Context context, int textViewResourceId,
                                ArrayList<BusRoute> items) {
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

            BusRoute item = items.get(position);
            if (item != null) {
                CheckedTextView itemView = (CheckedTextView) view
                        .findViewById(R.id.checkView);
                if (itemView != null) {
                    itemView.setText(item.getDesc());
                    itemView.setChecked(activeRoutes.get(position));
                }
            }
            return view;
        }

        public int getCount() {
            return items.size();
        }
    }

}