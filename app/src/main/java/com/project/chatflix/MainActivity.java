package com.project.chatflix;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.esafirm.imagepicker.features.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.activity.FriendRequestActivity;
import com.project.chatflix.activity.LoginActivity;
import com.project.chatflix.activity.Sinch_Calling.BaseActivity;
import com.project.chatflix.adapter.SectionsPagerAdapter;
import com.project.chatflix.fragment.ChatFragment;
import com.project.chatflix.fragment.GroupFragment;
import com.project.chatflix.fragment.InfoFragment;
import com.project.chatflix.object.User;
import com.project.chatflix.service.SinchService;
import com.project.chatflix.utils.StaticConfig;
import com.sinch.android.rtc.SinchError;

public class MainActivity extends BaseActivity implements SinchService.StartFailedListener{

    private Toolbar tb;
    private ViewPager viewPager;

    private SectionsPagerAdapter sectionsPagerAdapter;
    private TabLayout tabLayout;
    private FloatingActionButton floatButton;
    private LinearLayout layoutRequestFriend;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;
    private InfoFragment fragmentInfo;
    private ChatFragment fragmentChat;
    private GroupFragment fragmentGroup;
    private DatabaseReference mDatabaseRef;
    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        addEvents();
    }

    private void initView() {
        try {
            tb = findViewById(R.id.toolbarMain);
            setSupportActionBar(tb);
            getSupportActionBar().setTitle("");
            layoutRequestFriend = findViewById(R.id.layout_request_friend);

            fragmentInfo = new InfoFragment();
            fragmentChat = new ChatFragment();
            fragmentGroup = new GroupFragment();

            viewPager = findViewById(R.id.viewPagerMain);
            sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
            viewPager.setAdapter(sectionsPagerAdapter);

            tabLayout = findViewById(R.id.tabMain);
            tabLayout.setupWithViewPager(viewPager);
            tabLayout.animate();
            tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.off_white));
            tabLayout.setSelectedTabIndicatorHeight(3);

            floatButton = findViewById(R.id.fab);

            setupViewPager(viewPager);

            mDatabaseRef = FirebaseDatabase.getInstance().getReference();
            mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() != null) {
                mUserRef = mDatabaseRef.child(getString(R.string.users))
                        .child(mAuth.getCurrentUser().getUid());
            }

            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        if (dataSnapshot.getChildrenCount() != 0 && dataSnapshot.getValue() != null) {
                            String name = "";
                            int i = 0;
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                if (i == dataSnapshot.getChildrenCount() - 1) {
                                    User user = snapshot.getValue(User.class);
                                    int count = (int) (dataSnapshot.getChildrenCount() - 1);

                                    if (count == 0) {
                                        name = user.name + " " +  getString(R.string.sent_you_request);
                                    } else {
                                        name = user.name + " " + getString(R.string.and) + count + getString(R.string.people_sent_request);
                                    }
                                }else {
                                    i++;
                                }
                            }
                            i = 0;
                            putNotiFriendRequest(name);
                        }
                    } catch (Exception e) {
                        Log.e(getClass().getName(), e.toString());
                        Crashlytics.logException(e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            mDatabaseRef.child(getString(R.string.request_table)).child(StaticConfig.UID).addValueEventListener(valueEventListener);
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        sectionsPagerAdapter.addFrag(fragmentChat, getString(R.string.chat));
        sectionsPagerAdapter.addFrag(fragmentGroup, getString(R.string.group));
        sectionsPagerAdapter.addFrag(fragmentInfo, getString(R.string.info));
        floatButton.setOnClickListener(fragmentChat.onClickFloatButton.getInstance(this));
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(3);
    }

    private void addEvents() {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                try {
                    if (sectionsPagerAdapter.getItem(position) instanceof ChatFragment) {
                        floatButton.show();
                        floatButton.setOnClickListener(fragmentChat.onClickFloatButton.getInstance(MainActivity.this));
                        floatButton.setImageResource(R.drawable.ic_fr);
                    } else if (sectionsPagerAdapter.getItem(position) instanceof GroupFragment) {
                        floatButton.show();
                        floatButton.setOnClickListener(fragmentGroup.onClickFloatButton.getInstance(MainActivity.this));
                        floatButton.setImageResource(R.drawable.ic_gr);
                    } else {
                        floatButton.hide();
                    }
                } catch (Exception e) {
                    Log.e(getClass().getName(), e.toString());
                    Crashlytics.logException(e);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        layoutRequestFriend.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FriendRequestActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            sendToStart();
        } else {
            StaticConfig.UID = currentUser.getUid();
            mUserRef.child(getString(R.string.online)).setValue(getString(R.string.true_field));
        }
    }

    private void sendToStart() {
        Intent startIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(startIntent);
        finish();
    }

    private void putNotiFriendRequest(String name) {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this);

            //Create the intent that’ll fire when the user taps the notification//
            Intent intent = new Intent(this, FriendRequestActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(pendingIntent);

            mBuilder.setSmallIcon(R.drawable.logo_1);
            mBuilder.setContentTitle(getString(R.string.friend_request));
            mBuilder.setContentText(name);

            NotificationManager mNotificationManager =

                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(getString(R.string.app_name), 001, mBuilder.build());
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getSinchServiceInterface() != null) {
            getSinchServiceInterface().stopClient();
        }
        mDatabaseRef.child(getString(R.string.users))
                .child(StaticConfig.UID)
                .child(getString(R.string.online)).setValue(ServerValue.TIMESTAMP);
        mDatabaseRef.child(getString(R.string.request_table)).child(StaticConfig.UID)
                .removeEventListener(valueEventListener);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            if (data == null) {
                Toast.makeText(this, getString(R.string.error_occured_please_try_again), Toast.LENGTH_LONG).show();
                return;
            }
            fragmentInfo.handleImageUpload(data);
        }
    }

    @Override
    protected void onServiceConnected() {
        getSinchServiceInterface().setStartListener(this);

        if (!getSinchServiceInterface().isStarted()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user != null){
                getSinchServiceInterface().startClient(user.getEmail());
            }
        }
    }

    @Override
    public void onStartFailed(SinchError error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStarted() {

    }
}
