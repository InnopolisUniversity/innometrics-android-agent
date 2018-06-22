package com.example.innometrics.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.innometrics.LoginActivity;
import com.example.innometrics.R;
import com.example.innometrics.utils.ConnectionUtils;
import com.example.innometrics.utils.ApplicationUtils;

import java.util.ArrayList;

/**
 * One of the fragments of MainActivity.
 * Displays information about a user.
 * ListView contains actions for user:
 * Settings - no functionality,
 * Logout.
 * ImageView is supposed to display a user's avatar (no functionality)
 * TextView is for username.
 * @date: 19.06.2018
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private ImageView mProfileHeaderImage;
    private TextView mProfileHeaderUsername;
    private ListView mProfileListView;
    private ArrayAdapter<String> mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        setUI(view);
        setListView();
        setHeader();
        return view;
    }

    private void setUI(View parentView){
        mProfileHeaderUsername = parentView.findViewById(R.id.profile_header_username);
        mProfileHeaderImage = parentView.findViewById(R.id.profile_header_image);
        mProfileListView = parentView.findViewById(R.id.profile_list_view);
    }

    private void setHeader(){
        SharedPreferences prefs = getContext().getSharedPreferences(ConnectionUtils.PREFS_USER, Context.MODE_PRIVATE);
        mProfileHeaderUsername.setText(prefs.getString(ConnectionUtils.PREFS_USER_USERNAME,"username"));
    }

    private void setListView(){
        ArrayList<String> options = new ArrayList<>();
        options.add("Settings");
        options.add("Logout");
        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<>(getContext(),
                    R.layout.custom_item,
                    R.id.task_content,
                    options);
            mProfileListView.setAdapter(mAdapter);
        } else {
            mAdapter.clear();
            mAdapter.addAll(options);
            mAdapter.notifyDataSetChanged();
        }
        //Options
        mProfileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                if (position == 0) {
                    //here will be intent to user settings if they exist in the server
                }
                if (position == 1) {
                    logout();
                }
            }
        });
    }

    public void logout(){
        ApplicationUtils.clearPreferences(getContext());
        Intent loginPlease = new Intent(getContext(), LoginActivity.class);
        startActivity(loginPlease);
    }
}
