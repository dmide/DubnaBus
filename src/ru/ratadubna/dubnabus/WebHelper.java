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
        BufferedReader reader = null;
        int i = 0;
        try {
            do {
                reader = connectAndGetReader(url);
                buf = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line + "\n");
                }
            } while (buf.toString().equals("\n") && (++i < 5)); // trying to receive page 5 times
        } finally {
            closeReader(reader, "ModelFragment loadPage");
        }
        return buf.toString();
    }

    public static void loadContent(URL url, Parser parser, String checkString)
            throws Exception {
        int i = 0;
        boolean loaded = false;
        BufferedReader reader = null;
        try {
            do {
                reader = connectAndGetReader(url);
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
            } while (!loaded && (++i < 3)); // trying to receive page 3 times
        } finally {
            closeReader(reader, "ModelFragment loadContent");
        }
        if (!loaded) {
            throw new Exception("Problem loading content");
        }
    }

    private static BufferedReader connectAndGetReader(URL url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setReadTimeout(15000);
        c.connect();
        return new BufferedReader(new InputStreamReader(c.getInputStream()));
    }

    private static void closeReader(BufferedReader reader, String errorMsg) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e(errorMsg, "Exception closing HUC reader", e);
            }
        }
    }

    public interface Parser {
        void parse(String line) throws Exception;
    }
}
