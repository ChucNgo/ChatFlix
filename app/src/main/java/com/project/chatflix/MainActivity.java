package com.project.chatflix;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.project.chatflix.activity.FriendRequestActivity;
import com.project.chatflix.activity.LoginActivity;
import com.project.chatflix.adapter.SectionsPagerAdapter;
import com.project.chatflix.fragment.ChatFragment;
import com.project.chatflix.fragment.GroupFragment;
import com.project.chatflix.fragment.InfoFragment;
import com.project.chatflix.utils.StaticConfig;

public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        addEvents();
    }

    private void initView() {
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
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            mUserRef = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users))
                    .child(mAuth.getCurrentUser().getUid());
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

//        if (getSinchServiceInterface().isStarted()) {
//
//            Toast.makeText(MainActivity.this,"Service's still running!",Toast.LENGTH_SHORT).show();
//        }

//        Toast.makeText(MainActivity.this,"Stop friend chat service",Toast.LENGTH_SHORT).show();
    }

    private void sendToStart() {
        Intent startIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (getSinchServiceInterface() != null) {
//            getSinchServiceInterface().stopClient();
//            Toast.makeText(MainActivity.this,"Stop Service Sinch",Toast.LENGTH_SHORT).show();
//        }
        FirebaseDatabase.getInstance().getReference().child(getString(R.string.users))
                .child(StaticConfig.UID)
                .child(getString(R.string.online)).setValue(ServerValue.TIMESTAMP);

    }
//    @Override
//    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
//        if (SinchService.class.getName().equals(componentName.getClassName())) {
//            mSinchServiceInterface = (SinchService.SinchServiceInterface) iBinder;
//            onServiceConnected();
//        }
//    }

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

}
