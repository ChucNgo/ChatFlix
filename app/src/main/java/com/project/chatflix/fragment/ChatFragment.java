package com.project.chatflix.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.MainActivity;
import com.project.chatflix.R;
import com.project.chatflix.adapter.ListFriendsAdapter;
import com.project.chatflix.database.SharedPreferenceHelper;
import com.project.chatflix.mycustom.FragFriendClickFloatButton;
import com.project.chatflix.object.Friend;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.object.User;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ChatFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView recyclerListFrends;
    private ListFriendsAdapter adapter;
    public FragFriendClickFloatButton onClickFloatButton;
    public static ListFriend dataListFriend = null;
    private ArrayList<String> listFriendID = null;
    private LovelyProgressDialog dialogFindAllFriend;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private DatabaseReference mDatabaseRef;
    private SharedPreferenceHelper prefHelper;
    public static int ACTION_START_CHAT = 1;
    public static boolean firstLoad = true;

    public static final String ACTION_DELETE_FRIEND = "com.project.chatflix.DELETE_FRIEND";

    private BroadcastReceiver deleteFriendReceiver;

    public ChatFragment() {
        onClickFloatButton = new FragFriendClickFloatButton(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_chat, container, false);
        initView(layout);
        return layout;
    }

    private void initView(View layout) {
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();

        prefHelper = SharedPreferenceHelper.getInstance(layout.getContext());
        recyclerListFrends = layout.findViewById(R.id.recycleListFriend);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        recyclerListFrends.setLayoutManager(linearLayoutManager);
        mSwipeRefreshLayout = layout.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefresh);
        listFriendID = new ArrayList<>();
        dataListFriend = new ListFriend();

        adapter = new ListFriendsAdapter(getContext(), dataListFriend, this);
        recyclerListFrends.setAdapter(adapter);

        dialogFindAllFriend = new LovelyProgressDialog(getContext());

        deleteFriendReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String idDeleted = intent.getExtras().getString(getActivity().getString(R.string.id_friend));
                for (Friend friend : dataListFriend.getListFriend()) {
                    if (idDeleted.equals(friend.id)) {
                        ArrayList<Friend> friends = dataListFriend.getListFriend();
                        friends.remove(friend);
                        break;
                    }
                }
                adapter.notifyDataSetChanged();
            }
        };

        IntentFilter intentFilter = new IntentFilter(ACTION_DELETE_FRIEND);
        getContext().registerReceiver(deleteFriendReceiver, intentFilter);

        dialogFindAllFriend.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle(getString(R.string.get_all_friend))
                .setTopColorRes(R.color.colorPrimary)
                .show();
        getListFriendUId();
    }

    @Override
    public void onRefresh() {
        firstLoad = true;
        listFriendID.clear();
        dataListFriend.getListFriend().clear();
        adapter.notifyDataSetChanged();
        getListFriendUId();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getContext().unregisterReceiver(deleteFriendReceiver);
    }

    /**
     * Lay danh sach ban be tren server
     */
    private void getListFriendUId() {

        mDatabaseRef.child(getActivity().getString(R.string.users))
                .child(StaticConfig.UID).child(getActivity().getString(R.string.friend_field))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            HashMap mapRecord = (HashMap) dataSnapshot.getValue();
                            Iterator listKey = mapRecord.keySet().iterator();
                            while (listKey.hasNext()) {
                                String key = listKey.next().toString();
                                listFriendID.add(mapRecord.get(key).toString());
                            }
                            getAllFriendInfo(0);
                        } else {
                            dataListFriend.getListFriend().clear();
                            adapter.notifyDataSetChanged();
                            dialogFindAllFriend.dismiss();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        dialogFindAllFriend.dismiss();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void getAllFriendInfo(final int index) {
        try {
            if (index == listFriendID.size()) {
                //save list friend
                adapter.notifyDataSetChanged();
                dialogFindAllFriend.dismiss();
                mSwipeRefreshLayout.setRefreshing(false);

                Handler handler = new Handler();

                handler.postDelayed(() -> {
                    firstLoad = false;
                }, 2000);

            } else {
                final String id = listFriendID.get(index);
                mDatabaseRef.child(getString(R.string.users) + "/" + id)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getValue() != null) {
                                    Friend user = new Friend();
                                    HashMap mapUserInfo = (HashMap) dataSnapshot.getValue();
                                    user.name = (String) mapUserInfo.get(getString(R.string.name_field));
                                    user.email = (String) mapUserInfo.get(getString(R.string.email));
                                    user.avatar = (String) mapUserInfo.get(getString(R.string.avatar_field));
                                    user.id = id;
                                    user.idRoom = id.compareTo(StaticConfig.UID) > 0 ? (StaticConfig.UID + id).hashCode() + "" : "" + (id + StaticConfig.UID).hashCode();
                                    dataListFriend.getListFriend().add(user);
                                }
                                getAllFriendInfo(index + 1);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.toString());
            Crashlytics.logException(e);
            dialogFindAllFriend.dismiss();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    public void checkBeforAddFriend(final String idFriend, Friend userInfo, LovelyProgressDialog dialogWait) {
        dialogWait.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle(getActivity().getString(R.string.adding_friend))
                .setTopColorRes(R.color.colorPrimary)
                .show();

        if (listFriendID.contains(idFriend)) {
            dialogWait.dismiss();
            new LovelyInfoDialog(getActivity())
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle(getActivity().getString(R.string.friend_title))
                    .setMessage(getString(R.string.User) + " " + userInfo.email + " " + getString(R.string.was_in_your_list_friend))
                    .show();
        } else {
            checkExistRequestSentToFriend(idFriend, userInfo.email, dialogWait);
        }
    }

    private void checkExistRequestSentToFriend(String idFriend, String emailFriend, LovelyProgressDialog dialogWait) {
        mDatabaseRef.child(getActivity().getString(R.string.request_table))
                .child(idFriend).orderByChild(getString(R.string.user_id)).equalTo(StaticConfig.UID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getChildrenCount() != 0) {
                            dialogWait.dismiss();
                            new LovelyInfoDialog(getActivity())
                                    .setTopColorRes(R.color.colorPrimary)
                                    .setIcon(R.drawable.ic_add_friend)
                                    .setTitle(getActivity().getString(R.string.friend_title))
                                    .setMessage(getString(R.string.you_have_sent_to) + " " + emailFriend
                                    + " " + getString(R.string.a_friend_request))
                                    .show();
                        } else {
                            sendRequest(idFriend, dialogWait);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void sendRequest(String idFriend, LovelyProgressDialog dialogWait) {

        HashMap<String, String> mapUser = new HashMap<>();
        User user = prefHelper.getUserInfo();
        mapUser.put(getActivity().getString(R.string.name_field), user.getName());
        mapUser.put(getActivity().getString(R.string.user_id), StaticConfig.UID);
        mapUser.put(getActivity().getString(R.string.avatar_field), user.getAvatar());

        mDatabaseRef.child(getActivity().getString(R.string.request_table))
                .child(idFriend).push().setValue(mapUser)
                .addOnCompleteListener(task -> {
                    dialogWait.dismiss();
                    if (task.isSuccessful() && task.isComplete()) {
                        new LovelyInfoDialog(getActivity())
                                .setTopColorRes(R.color.colorPrimary)
                                .setIcon(R.drawable.ic_add_friend)
                                .setTitle(getActivity().getString(R.string.success))
                                .setMessage(getActivity().getString(R.string.send_request_friend_success))
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    dialogWait.dismiss();
                    new LovelyInfoDialog(getActivity())
                            .setTopColorRes(R.color.colorAccent)
                            .setIcon(R.drawable.ic_add_friend)
                            .setTitle(getActivity().getString(R.string.failed))
                            .setMessage(getActivity().getString(R.string.add_friend_failed))
                            .show();
                });
    }

}


