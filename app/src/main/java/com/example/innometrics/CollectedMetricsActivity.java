package com.example.innometrics;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.innometrics.fragments.ForegroundAppsFragment;
import com.example.innometrics.fragments.LocationsFragment;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

/**
 * Activity which shows local collected data.
 * Handles bottom navigation.
 * @see ForegroundAppsFragment
 * @see LocationsFragment
 */
public class CollectedMetricsActivity extends AppCompatActivity {
    private BottomNavigationViewEx metricNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collected_metrics);
        metricNavigation = findViewById(R.id.collected_metrics_bottom_navigation);
        metricNavigation.setOnNavigationItemSelectedListener(navListener);
        metricNavigation.enableAnimation(false);
        metricNavigation.enableShiftingMode(false);
        metricNavigation.setIconVisibility(false);
        metricNavigation.setTextSize(18f);
        metricNavigation.setScrollBarSize(60);
        metricNavigation.setIconsMarginTop(15);
        if (savedInstanceState == null){
            getSupportFragmentManager().beginTransaction().replace(R.id.collected_metrics_fragment_container,
                    new ForegroundAppsFragment()).commit();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    switch (item.getItemId()) {
                        case R.id.nav_apps:
                            selectedFragment = new ForegroundAppsFragment();
                            break;
                        case R.id.nav_locations:
                            selectedFragment = new LocationsFragment();
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.collected_metrics_fragment_container,
                            selectedFragment).commit();
                    return true;
                }
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.local_data_action_bar_menu, menu);
        return true;
    }
}
