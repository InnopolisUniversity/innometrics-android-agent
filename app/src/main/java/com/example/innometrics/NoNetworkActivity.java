package com.example.innometrics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.innometrics.utils.ConnectionUtils;


/**
 * Allow to go to the main activity only
 * after pressing retry button and if network is available
 */
public class NoNetworkActivity extends BasicActivity {
    private Button mRetryButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_network);
        mRetryButton = findViewById(R.id.no_network_button_retry);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectionUtils.networkAvailable(NoNetworkActivity.this, false)){
                    Intent startAgain = new Intent(NoNetworkActivity.this, MainActivity.class);
                    startAgain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(startAgain);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
