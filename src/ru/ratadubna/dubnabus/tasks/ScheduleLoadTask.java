package ru.ratadubna.dubnabus.tasks;

import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.maps.model.Marker;
import ru.ratadubna.dubnabus.ModelFragment;
import ru.ratadubna.dubnabus.R;
import ru.ratadubna.dubnabus.WebHelper;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dmide on 07/01/14.
 */

public class ScheduleLoadTask extends AsyncTask<Void, Void, Void> {
    private final static String NO_NUMBER = "<b>" + ModelFragment.NUMBER_SYMBOL + "</b> -&nbsp;";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([" + ModelFragment.NUMBER_SYMBOL + ":\\d]+)");

    private StringBuilder result = new StringBuilder();
    private Exception e;
    private final Marker marker;
    private final ModelFragment modelFragment;
    private final int id;

    public ScheduleLoadTask(ModelFragment modelFragment, int id, Marker marker) {
        this.modelFragment = modelFragment;
        this.id = id;
        this.marker = marker;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            WebHelper.loadContent(new URL(modelFragment.getString(R.string.schedule_url) + String.valueOf(id)),
                    new ScheduleLoader(), "<");
        } catch (Exception e) {
            this.e = e;
            Log.e(getClass().getSimpleName(),
                    "Exception retrieving bus schedule content", e);
        }
        return (null);
    }

    @Override
    public void onPostExecute(Void params) {
        if (e == null) {
            String strResult = result.toString();
            if ((strResult == null) || (strResult.isEmpty())
                    || (strResult.equals(NO_NUMBER))) {
                modelFragment.lastBusSchedule = "";
            } else {
                modelFragment.lastBusSchedule = strResult;
            }
            marker.setSnippet(modelFragment.lastBusSchedule);
            marker.showInfoWindow();
        } else {
            modelFragment.showProblemToast();
        }
    }

    private class ScheduleLoader implements WebHelper.Parser {
        @Override
        public void parse(String line) {
            String tmp;
            line = line.replaceAll(",", ".");
            Matcher matcher = NUMBER_PATTERN.matcher(line);
            if (matcher.find()) {
                if ((tmp = matcher.group()).length() == 3) {
                    tmp += "&nbsp;&nbsp;";
                }
                result.append("<b>" + tmp + "</b> -&nbsp;");
                if (matcher.find()) {
                    result.append("<font  color=\"green\">"
                            + matcher.group() + "</font>");
                    while (matcher.find()) {
                        result.append(" " + matcher.group());
                    }
                    result.append("<br />");
                }
            }
        }
    }
}
