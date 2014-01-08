package ru.ratadubna.dubnabus;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

class SnippetAdapter implements InfoWindowAdapter {
    private final LayoutInflater inflater;
    private final Context context;

    SnippetAdapter(LayoutInflater inflater, Context context) {
        this.inflater = inflater;
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View infoWindow = inflater.inflate(R.layout.schedulesnippet, null);

        TextView tv = (TextView) infoWindow.findViewById(R.id.title);
        tv.setText(marker.getTitle());
        tv = (TextView) infoWindow.findViewById(R.id.snippet);
        String snippetText;
        if ((snippetText = marker.getSnippet()) != null) {
            tv.setText(Html.fromHtml(snippetText, new ImageGetter(), null));
        }
        return (infoWindow);
    }

    private class ImageGetter implements Html.ImageGetter {
        public Drawable getDrawable(String source) {
            int id;

            if (source.equals(context.getString(R.string.bus_type_59_remote_file))) {
                id = R.drawable.bustype59;
            } else if (source.equals(context.getString(R.string.bus_type_91_remote_file))) {
                id = R.drawable.bustype91;
            } else {
                return null;
            }

            Drawable d = context.getResources().getDrawable(id);
            d.setBounds(0, 0, 70, 45);
            return d;
        }
    }
}