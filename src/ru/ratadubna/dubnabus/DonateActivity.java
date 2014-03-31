package ru.ratadubna.dubnabus;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.sufficientlysecure.donations.DonationsFragment;

public class DonateActivity extends SherlockFragmentActivity {
    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAif5i4DlVrmy1/breLbTA3X17Su2819taOLSsh8AZauTS/N6lwVAIi0vWVQPeWx4iEzgXc0h8gGBMoK/x56KR4gs4pUWfw6ZyeZdn1MOaePqCxKHKvUa5tigCJQVgpRYeCdR58shTGDYOttd2gmnKmRxeZcGa65h9r91UfcrVLWVTl3k67Sae0zEbZt8tZZTjNG3zzGKBoZtxmYZU8kgbAuKMtCSKUlUK7FaG+/hZxpCzcT7tmEXrRx4AAE9phlfdR9ebYdK9oJMJbfCUoQfnmdtbtGzotNqCKgCgxNK9ChiSL0TpUv+wJ2LkkdlBivHv1BakqpJCwe3tawABah2XYwIDAQAB";
    private static final String[] GOOGLE_CATALOG = new String[]{"ntpsync.donation.1",
            "ntpsync.donation.2", "ntpsync.donation.3", "ntpsync.donation.5", "ntpsync.donation.10"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        if (savedInstanceState == null) {
            DonationsFragment donationsFragment = DonationsFragment.newInstance(false, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
                    getResources().getStringArray(R.array.donation_google_catalog_values), false, null, null,
                    null, false, null, null);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, donationsFragment)
                    .commit();
        }
    }
}
