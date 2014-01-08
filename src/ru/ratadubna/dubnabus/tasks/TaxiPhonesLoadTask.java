package ru.ratadubna.dubnabus.tasks;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import ru.ratadubna.dubnabus.ModelFragment;
import ru.ratadubna.dubnabus.R;
import ru.ratadubna.dubnabus.SimpleContentActivity;
import ru.ratadubna.dubnabus.WebHelper;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dmide on 07/01/14.
 */

public class TaxiPhonesLoadTask extends AsyncTask<Void, Void, Void> {
    private static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
            + "<style type=\"text/css\">td {white-space: nowrap;}</style>";

    private Exception e = null;
    private String page = "";
    private final ModelFragment modelFragment;
    private final String url;

    public TaxiPhonesLoadTask(ModelFragment modelFragment, String url) {
        this.modelFragment = modelFragment;
        this.url = url;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            page = WebHelper.getPage(new URL(url));
            if (page.equals("\n")) {
                throw new Exception("Connection problem");
            }
        } catch (Exception e) {
            this.e = e;
            Log.e(getClass().getSimpleName(),
                    "Exception retrieving taxi phones content", e);
        }
        return (null);
    }

    @Override
    public void onPostExecute(Void params) {
        String result = parsePhoneNumbersPage(page);
        if (e == null) {
            String data = HEADER + result;
            Intent i = new Intent(modelFragment.getActivity(), SimpleContentActivity.class);
            i.putExtra(SimpleContentActivity.EXTRA_DATA, data);
            modelFragment.startActivity(i);
        }
    }

    private String parsePhoneNumbersPage(String page) {
        Pattern pattern = Pattern
                .compile("<table border=0 width=\"100%\">([\\s\\S]+?)</table>"), pattern2 = Pattern
                .compile("(8[\\d\\(\\)\\s-]{14,15})");
        Matcher matcher = pattern.matcher(page);
        if (matcher.find()) {
            String phone, result = matcher
                    .group()
                    .replaceAll("(21[\\d-]{7}<br[\\s>/]+)", "")
                    .replaceAll("(\\(49621\\)[\\d\\s-]+<br[\\s>/]+)", "")
                    .replaceAll("\\(9", "8(9")
                    .replaceAll("[\\(\\)]\\s*", "-")
                    .replaceAll("<tr>\\s+<td colspan=5><hr></td>\\s+</tr>",
                            "").replace("lightgrey", "lightblue");
            matcher = pattern2.matcher(result);
            while (matcher.find()) {
                phone = matcher.group();
                result = result.replaceAll(phone, "<a href=\"tel:" + phone
                        + "\">" + phone + "</a>");
            }
            return result;
        } else {
            modelFragment.showProblemToast();
            return modelFragment.getString(R.string.connection_problem);
        }
    }
}
