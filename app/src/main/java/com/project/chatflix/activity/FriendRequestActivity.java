package com.project.chatflix.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.project.chatflix.R;

public class FriendRequestActivity extends AppCompatActivity {

    private Toolbar tb;
    private RecyclerView rvRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);
        initView();
    }

    private void initView() {
        tb = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rvRequest = (RecyclerView) findViewById(R.id.rv_friend_request);
    }
}
