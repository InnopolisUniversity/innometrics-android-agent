package com.example.innometrics;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.example.innometrics.utils.ConnectionUtils;
import com.example.innometrics.server.ResponseObject;
import com.example.innometrics.server.ServerRequestItem;
import com.example.innometrics.utils.ApplicationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;


public class LoginActivity extends BasicActivity {
    public static final String TAG = "LoginActivity";
    private static final boolean DEBUG = ApplicationUtils.DEBUG;

    private EditText mInputLogin;
    private EditText mInputPassword;
    private Button mLoginButton;
    private TextView mLinkRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setUI();
        //for buttons to interact
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectionUtils.networkAvailable(LoginActivity.this, true)) login();
            }
        });
        mLinkRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectionUtils.networkAvailable(LoginActivity.this, true)){
                    Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    private void setUI(){
        mInputLogin = findViewById(R.id.input_username_in_login_activity);
        mInputPassword = findViewById(R.id.input_password_in_login_activity);
        mLoginButton = findViewById(R.id.button_login);
        mLinkRegister = findViewById(R.id.link_register);
    }

    private void login(){
        if (DEBUG) Log.d(TAG, "login");
        //while we login, we can't press login again
        mLoginButton.setEnabled(false);
        //after pressing login keyboard doesn't close, which is annoying
        //so it is down manually
        closeKeyboard();
        //this time to show ProgressDialog Handler is used
        final ProgressDialog progressDialog = new ProgressDialog(this, R.style.AppBaseDialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getResources().getString(R.string.authentication_progress_dialog));
        progressDialog.show();


        final String login = mInputLogin.getText().toString();
        final String password = mInputPassword.getText().toString();
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.accumulate(ConnectionUtils.LOGIN_USERNAME, login);
                            jsonObject.accumulate(ConnectionUtils.LOGIN_PASSWORD, password);
                            jsonObject.accumulate(ConnectionUtils.LOGIN_PROJECT, "parasha");
                            ResponseObject answer = ConnectionUtils.request(new ServerRequestItem(ConnectionUtils.URL_LOGIN_TOKEN, null, jsonObject.toString()));
                            if (answer == null)
                            {
                                onLoginFailed(null);
                                progressDialog.dismiss();
                                return;
                            }
                            if (answer.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                onLoginSuccess(answer, login, password);
                            } else {
                                onLoginFailed(answer);
                            }
                            progressDialog.dismiss();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);
    }

    /**
     * Put user data into SharedPreferences
     * start MainActivity
     */
    private void onLoginSuccess(ResponseObject answer, String login, String password) throws JSONException {
        SharedPreferences.Editor editor = getSharedPreferences(ConnectionUtils.PREFS_USER, MODE_PRIVATE).edit();
        String token = answer.getResponse().getString("token");
        editor.putString(ConnectionUtils.PREFS_USER_TOKEN, token);
        editor.putString(ConnectionUtils.PREFS_USER_USERNAME, login);
        editor.putString(ConnectionUtils.PREFS_USER_PASSWORD, password);
        editor.apply(); //or commit
        mLoginButton.setEnabled(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * @param answer contains errors made by user, put them right into edit fields.
     * Note: only one error per edit field is shown, though server sends them as arrays (with one element)
     */
    private void onLoginFailed(ResponseObject answer) {
        if (answer == null)
        {
            Toast.makeText(getBaseContext(), "There is no connection to server", Toast.LENGTH_LONG).show();
            mLoginButton.setEnabled(true);
            return;
        }
        //TODO: response errors can't be translated like other strings
        JSONObject responseBody = answer.getResponse();
        try {
            if (responseBody.has(ConnectionUtils.LOGIN_USERNAME)){
                String usernameErrors = ((JSONArray) responseBody.get(ConnectionUtils.LOGIN_USERNAME)).getString(0);
                mInputLogin.setError(usernameErrors);
            }
            if (responseBody.has(ConnectionUtils.LOGIN_PASSWORD)) {
                String passwordErrors = ((JSONArray) responseBody.get(ConnectionUtils.LOGIN_PASSWORD)).getString(0);
                mInputPassword.setError(passwordErrors);
            }
            //if (responseBody.has(ConnectionUtils.LOGIN_NON_FIELD_ERRORS)){
            //    Toast.makeText(getBaseContext(), ((JSONArray) responseBody.get(ConnectionUtils.LOGIN_NON_FIELD_ERRORS)).getString(0), Toast.LENGTH_LONG).show();
            //}
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mLoginButton.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity, we just logged out.
        moveTaskToBack(true);
    }
}
