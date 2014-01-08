package ru.ratadubna.dubnabus.tasks;

import android.os.AsyncTask;
import android.util.Log;
import ru.ratadubna.dubnabus.BusRoute;
import ru.ratadubna.dubnabus.ModelFragment;
import ru.ratadubna.dubnabus.R;
import ru.ratadubna.dubnabus.WebHelper;

import java.net.URL;

/**
* Created by dmide on 07/01/14.
*/

public class BusRoutesLoadTask extends AsyncTask<Void, Void, Void> {
    private Exception e;
    private final ModelFragment modelFragment;

    public BusRoutesLoadTask(ModelFragment modelFragment){
        this.modelFragment = modelFragment;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            WebHelper.loadContent(new URL(modelFragment.getString(R.string.routes_url)),
                    new BusRoutesListLoader(), ModelFragment.NUMBER_SYMBOL);
        } catch (Exception e) {
            this.e = e;
            Log.e(getClass().getSimpleName(),
                    "Exception retrieving bus routes content", e);
        }
        return (null);
    }

    @Override
    public void onPostExecute(Void arg0) {
        if (e != null) {
            modelFragment.showProblemToast();
        }
    }

    private class BusRoutesListLoader implements WebHelper.Parser {
        @Override
        public void parse(String line) throws Exception {
            String[] contents = line.split("\t");
            if (contents.length > 1) {
                BusRoute.addRouteToArray(Integer.parseInt(contents[0]), ModelFragment.NUMBER_SYMBOL + contents[1] + " - " + contents[3], Integer.parseInt(contents[1]));
            } else {
                throw new Exception("Problem in parseBusRoutesList");
            }
        }
    }
}