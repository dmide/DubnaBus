package ru.ratadubna.dubnabus;

import android.view.View;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Created by dmide on 09/03/14.
 */
public abstract class BaseActivity extends SherlockFragmentActivity {
    public <T extends View> T viewById(int id){
        return (T) findViewById(id);
    }

    public static <T extends View> T viewById(View parent, int id) {
        return (T) parent.findViewById(id);
    }
}
