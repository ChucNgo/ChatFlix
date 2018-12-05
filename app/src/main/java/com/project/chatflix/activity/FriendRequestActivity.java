package com.project.chatflix.activity;

import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.adapter.ListFriendRequestAdapter;
import com.project.chatflix.object.User;
import com.project.chatflix.utils.StaticConfig;


public class FriendRequestActivity extends AppCompatActivity {

    private Toolbar tb;
    private RecyclerView rvRequest;
    private LinearLayoutManager layoutManager;
    private ListFriendRequestAdapter adapterRequest;

    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);
        initView();
    }

    private void initView() {
        try {
            tb = findViewById(R.id.toolbar);
            setSupportActionBar(tb);
            getSupportActionBar().setTitle(null);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            rvRequest = findViewById(R.id.rv_friend_request);
            rvRequest.setLayoutManager(layoutManager);

            mDatabaseRef = FirebaseDatabase.getInstance().getReference();

            Query query = mDatabaseRef.child(getString(R.string.request_table))
                    .child(StaticConfig.UID);
            FirebaseRecyclerOptions<User> options = new FirebaseRecyclerOptions.Builder<User>()
                    .setQuery(query, User.class).build();

            adapterRequest = new ListFriendRequestAdapter(this, options);
            rvRequest.setAdapter(adapterRequest);

            NotificationManagerCompat.from(this).cancel(getString(R.string.app_name), 001);
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    @Override
    protected void onStart() {
        adapterRequest.startListening();
        super.onStart();
    }

    @Override
    protected void onStop() {
        adapterRequest.stopListening();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
