package com.project.chatflix;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.project.chatflix.activity.LoginActivity;
import com.project.chatflix.adapter.SectionsPagerAdapter;
import com.project.chatflix.fragment.ChatFragment;
import com.project.chatflix.fragment.GroupFragment;
import com.project.chatflix.fragment.InfoFragment;
import com.project.chatflix.service.ServiceUtils;

public class MainActivity extends AppCompatActivity {

    private Toolbar tb;
    private ViewPager viewPager;

    private SectionsPagerAdapter sectionsPagerAdapter;
    private TabLayout tabLayout;
    private FloatingActionButton floatButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tb = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(tb);
        getSupportActionBar().setTitle("");

        // Tab Layout
        viewPager = (ViewPager) findViewById(R.id.viewPagerMain);
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(sectionsPagerAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tabMain);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.animate();
        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.tab_color));
        tabLayout.setSelectedTabIndicatorHeight(3);

        floatButton = (FloatingActionButton) findViewById(R.id.fab);

        setupViewPager(viewPager);
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            mUserRef = FirebaseDatabase.getInstance().getReference().child("Users")
                    .child(mAuth.getCurrentUser().getUid());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.about) {
            // Log out account
            Toast.makeText(MainActivity.this, "ChatFlix version 1.0", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            sendToStart();
        } else {
            mUserRef.child("online").setValue("true");
        }
        ServiceUtils.stopServiceFriendChat(getApplicationContext(), false);

//        if (getSinchServiceInterface().isStarted()) {
//
//            Toast.makeText(MainActivity.this,"Service's still running!",Toast.LENGTH_SHORT).show();
//        }

//        Toast.makeText(MainActivity.this,"Stop friend chat service",Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onStop() {
        super.onStop();
//        Toast.makeText(MainActivity.this,"Start friend chat service",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        ServiceUtils.startServiceFriendChat(getApplicationContext());
//        Toast.makeText(this,"Start friend chat service",Toast.LENGTH_SHORT).show();
        super.onPause();
    }

//    @Override
//    protected void onDestroy() {
//        if (getSinchServiceInterface() != null) {
//            getSinchServiceInterface().stopClient();
//        }
//        super.onDestroy();
//    }

    private void setupViewPager(ViewPager viewPager) {
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        sectionsPagerAdapter.addFrag(new ChatFragment(), getString(R.string.chat));
        sectionsPagerAdapter.addFrag(new GroupFragment(), getString(R.string.group));
        sectionsPagerAdapter.addFrag(new InfoFragment(), getString(R.string.info));
        floatButton.setOnClickListener(((ChatFragment) sectionsPagerAdapter.getItem(0)).onClickFloatButton.getInstance(this));
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ServiceUtils.stopServiceFriendChat(MainActivity.this.getApplicationContext(), false);
                if (sectionsPagerAdapter.getItem(position) instanceof ChatFragment) {
                    floatButton.setVisibility(View.VISIBLE);
                    floatButton.setOnClickListener(((ChatFragment) sectionsPagerAdapter.getItem(position)).onClickFloatButton.getInstance(MainActivity.this));
                    floatButton.setImageResource(R.drawable.ic_fr);
                } else if (sectionsPagerAdapter.getItem(position) instanceof GroupFragment) {
                    floatButton.setVisibility(View.VISIBLE);
//                    floatButton.setOnClickListener(((GroupFragment) sectionsPagerAdapter.getItem(position)).onClickFloatButton.getInstance(MainActivity.this));
                    floatButton.setImageResource(R.drawable.ic_gr);
                } else {
                    floatButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void sendToStart() {
        Intent startIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public void onDestroy() {
//        if (getSinchServiceInterface() != null) {
//            getSinchServiceInterface().stopClient();
//            Toast.makeText(MainActivity.this,"Stop Service Sinch",Toast.LENGTH_SHORT).show();
//        }
        if (mAuth.getCurrentUser() != null) {
            FirebaseDatabase.getInstance().getReference().child("Users")
                    .child(mAuth.getCurrentUser().getUid())
                    .child("online").setValue(ServerValue.TIMESTAMP);
        }


        super.onDestroy();
    }
//    @Override
//    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
//        if (SinchService.class.getName().equals(componentName.getClassName())) {
//            mSinchServiceInterface = (SinchService.SinchServiceInterface) iBinder;
//            onServiceConnected();
//        }
//    }
}
