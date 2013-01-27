package ru.ratadubna.dubnabus;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

class ScheduleSnippetAdapter implements InfoWindowAdapter {
	LayoutInflater inflater = null;

	ScheduleSnippetAdapter(LayoutInflater inflater) {
		this.inflater = inflater;
	}

	@Override
	public View getInfoWindow(Marker marker) {
		return (null);
	}

	@Override
	public View getInfoContents(Marker marker) {
		View infoWindow = inflater.inflate(R.layout.schedulesnippet, null);

		TextView tv = (TextView) infoWindow.findViewById(R.id.title);
		tv.setText(marker.getTitle());
		tv = (TextView) infoWindow.findViewById(R.id.snippet);
		String snippetText;
		if ((snippetText = marker.getSnippet()) != null)
			tv.setText(Html.fromHtml(snippetText));
		return (infoWindow);
	}
}