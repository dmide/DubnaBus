package ru.ratadubna.dubnabus;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

class SnippetAdapter implements InfoWindowAdapter {
	LayoutInflater inflater = null;

	SnippetAdapter(LayoutInflater inflater) {
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
			tv.setText(Html.fromHtml(snippetText, new ImageGetter(), null));
		return (infoWindow);
	}

	private class ImageGetter implements Html.ImageGetter {

		public Drawable getDrawable(String source) {
			int id;

			if (source.equals("file:///android_res/drawable/busType59.png")) {
				id = R.drawable.bustype59;
			} else if (source
					.equals("file:///android_res/drawable/busType91.png")) {
				id = R.drawable.bustype91;
			} else {
				return null;
			}

			Drawable d = ModelFragment.getCtxt().getResources()
					.getDrawable(id);
			d.setBounds(0, 0, 70, 45);
			return d;
		}
	};
}