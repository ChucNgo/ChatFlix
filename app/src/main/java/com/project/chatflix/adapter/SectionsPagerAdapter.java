package com.project.chatflix.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 11/15/2017.
 */

public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private final List<Fragment> mFragmentList = new ArrayList<>();

    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

//        switch (position){
//            case 0:
//                ChatFragment chatFragment = new ChatFragment();
//                return chatFragment;
//            case 1:
//                GroupFragment groupFragment = new GroupFragment();
//                return groupFragment;
//            case 2:
//                InfoFragment infoFragment = new InfoFragment();
//                return infoFragment;
//            default:
//                return null;
//        }

        return mFragmentList.get(position);

    }

    public void addFrag(Fragment fragment, String title) {
        mFragmentList.add(fragment);
    }

    @Override
    public int getCount() {
        return 3;
    }

    public CharSequence getPageTitle(int position){

        switch (position){
            case 0:
                return "CHATS";
            case 1:
                return "GROUP";
            case 2:
                return "INFO";
            default:
                return null;
        }

    }

}
