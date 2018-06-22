package com.example.innometrics;

import android.app.ProgressDialog;
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

public class RegisterActivity extends BasicActivity {
    public static final String TAG = "RegisterActivity";
    private static final boolean DEBUG = ApplicationUtils.DEBUG;

    private EditText mInputLogin;
    private EditText mInputEmail;
    private EditText mInputPassword;
    private Button mRegisterButton;
    private TextView mLinkLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setUI();
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (networkAvailable(true)){
                    register();
                }
            }
        });

        mLinkLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setUI(){
        mInputEmail = findViewById(R.id.input_email_in_register_activity);
        mInputLogin = findViewById(R.id.input_username_in_register_activity);
        mInputPassword = findViewById(R.id.input_password_in_register_activity);
        mRegisterButton = findViewById(R.id.button_register);
        mLinkLogin = findViewById(R.id.link_login);
    }

    private void register(){
        if (DEBUG) Log.d(TAG, "register");
        mRegisterButton.setEnabled(false);
        //closing keyboard and using handler to show ProgressDialog just like in LoginActivity
        closeKeyboard();
        final ProgressDialog progressDialog = new ProgressDialog(this, R.style.AppBaseDialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        final String login = mInputLogin.getText().toString();
        final String email = mInputEmail.getText().toString();
        final String password = mInputPassword.getText().toString();
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.accumulate(ConnectionUtils.REGISTER_USERNAME, login);
                            jsonObject.accumulate(ConnectionUtils.REGISTER_PASSWORD, password);
                            jsonObject.accumulate(ConnectionUtils.REGISTER_EMAIL, email);
                            ResponseObject answer = ConnectionUtils.request(new ServerRequestItem(ConnectionUtils.URL_REGISTER, null, jsonObject.toString()));
                            if (answer.getResponseCode() == HttpURLConnection.HTTP_OK || answer.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                                onRegisterSuccess(answer, login, email, password);
                            } else {
                                onRegisterFailed(answer);
                                progressDialog.dismiss();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);
    }

    /**
     * Put user data into SharedPreferences
     * Go back to LoginActivity
     */
    private void onRegisterSuccess(ResponseObject answer, String login, String email, String password) throws JSONException {
        SharedPreferences.Editor editor = getSharedPreferences(ConnectionUtils.PREFS_USER, MODE_PRIVATE).edit();
        String token = answer.getResponse().getString(ConnectionUtils.REGISTER_TOKEN);
        editor.putString(ConnectionUtils.PREFS_USER_TOKEN, token);
        editor.putString(ConnectionUtils.PREFS_USER_USERNAME, login);
        editor.putString(ConnectionUtils.PREFS_USER_EMAIL, email);
        editor.putString(ConnectionUtils.PREFS_USER_PASSWORD, password);
        editor.apply();
        mRegisterButton.setEnabled(true);
        //finish() will lead user to LoginActivity, because only from LoginActivity we could come here
        finish();
    }

    /**
     * @param answer contains errors made by user, put them right into edit fields.
     * Note: only one error per edit field is shown, though server sends them as arrays (with one element)
     */
    private void onRegisterFailed(ResponseObject answer){
        JSONObject responseBody = answer.getResponse();
        try {
            if (responseBody.has(ConnectionUtils.REGISTER_USERNAME)){
                String usernameErrors = ((JSONArray) responseBody.get(ConnectionUtils.REGISTER_USERNAME)).getString(0);
                mInputLogin.setError(usernameErrors);
            }
            if (responseBody.has(ConnectionUtils.REGISTER_EMAIL)) {
                String emailErrors = ((JSONArray) responseBody.get(ConnectionUtils.REGISTER_EMAIL)).getString(0);
                mInputEmail.setError(emailErrors);
            }
            if (responseBody.has(ConnectionUtils.REGISTER_PASSWORD)) {
                String passwordErrors = ((JSONArray) responseBody.get(ConnectionUtils.REGISTER_PASSWORD)).getString(0);
                mInputPassword.setError(passwordErrors);
            }
            if (responseBody.has(ConnectionUtils.REGISTER_NON_FIELD_ERRORS)){
                Toast.makeText(getBaseContext(), ((JSONArray) responseBody.get(ConnectionUtils.REGISTER_NON_FIELD_ERRORS)).getString(0), Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mRegisterButton.setEnabled(true);
    }


}
