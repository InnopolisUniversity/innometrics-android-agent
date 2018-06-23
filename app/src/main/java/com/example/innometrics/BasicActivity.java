package com.example.innometrics;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.innometrics.utils.ConnectionUtils;
import com.example.innometrics.utils.ApplicationUtils;

/**
 * BasicActivity has basic functionality to use throughout the application by activities
 */
public class BasicActivity extends AppCompatActivity {
    /**
     * Checks if a user has to log in and starts
     */
    public boolean loginRequired(){
        if (!ConnectionUtils.userIsLoggedIn(getSharedPreferences(ConnectionUtils.PREFS_USER, MODE_PRIVATE))){
            Intent pleaseLogin = new Intent(this, LoginActivity.class);
            //to make sure task (stack of activities) is cleared.
            pleaseLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ApplicationUtils.clearPreferences(this);
            startActivity(pleaseLogin);
            return true;
        }
        return false;
    }


    public void closeKeyboard() {
        //User expects to keyboard to be closed after some actions (e.g. pressing enter)
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
