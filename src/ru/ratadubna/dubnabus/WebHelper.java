package ru.ratadubna.dubnabus;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by dmide on 07/01/14.
 */
public class WebHelper {

    public static String getPage(URL url) throws Exception {
        StringBuilder buf;
        BufferedReader reader;
        int i = 0;
        do {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setReadTimeout(15000);
            c.connect();
            reader = new BufferedReader(new InputStreamReader(
                    c.getInputStream()));
            buf = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }
        } while (buf.toString().equals("\n") && (++i < 5)); // check for empty page
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("ModelFragment loadPage",
                        "Exception closing HUC reader", e);
            }
        }

        return buf.toString();
    }

    public static void loadContent(URL url, Parser parser, String checkString)
            throws Exception {
        int i = 0;
        BufferedReader reader;
        boolean loaded = false;
        do {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setReadTimeout(15000);
            c.connect();
            reader = new BufferedReader(new InputStreamReader(
                    c.getInputStream()));
            String line = reader.readLine();
            if ((line != null) && (!line.isEmpty())
                    && (line.contains(checkString))) {
                do {
                    parser.parse(line);
                } while ((line = reader.readLine()) != null);
            } else {
                continue;
            }
            loaded = true;
        } while (!loaded && (++i < 3)); // check for empty page
        if (!loaded)
            throw new Exception("Problem loading content");
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("ModelFragment loadContent",
                        "Exception closing HUC reader", e);
            }
        }
    }

    public interface Parser {
        void parse(String line) throws Exception;
    }
}
