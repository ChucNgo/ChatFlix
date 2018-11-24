package com.project.chatflix.fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.adapter.ListFriendsAdapter;
import com.project.chatflix.database.FriendDB;
import com.project.chatflix.database.SharedPreferenceHelper;
import com.project.chatflix.object.Friend;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.object.User;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private FirebaseAuth mAuth;
    private RecyclerView recyclerListFrends;
    private ListFriendsAdapter adapter;
    public FragFriendClickFloatButton onClickFloatButton;
    private ListFriend dataListFriend = null;
    private ArrayList<String> listFriendID = null;
    private LovelyProgressDialog dialogFindAllFriend;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private DatabaseReference mDatabaseRef;
    SharedPreferenceHelper prefHelper;
    public static int ACTION_START_CHAT = 1;

    public static final String ACTION_DELETE_FRIEND = "com.project.chatflix.DELETE_FRIEND";

    private BroadcastReceiver deleteFriendReceiver;

    public ChatFragment() {
        onClickFloatButton = new FragFriendClickFloatButton();
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

        recyclerListFrends = layout.findViewById(R.id.recycleListFriend);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        recyclerListFrends.setLayoutManager(linearLayoutManager);
        mSwipeRefreshLayout = layout.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefresh);
        prefHelper = SharedPreferenceHelper.getInstance(layout.getContext());
        mAuth = FirebaseAuth.getInstance();
        listFriendID = new ArrayList<>();

        if (dataListFriend == null) {
            dataListFriend = FriendDB.getInstance(getContext()).getListFriend();
            if (dataListFriend.getListFriend().size() > 0) {
                listFriendID = new ArrayList<>();
                for (Friend friend : dataListFriend.getListFriend()) {
                    listFriendID.add(friend.id);
                }
            }
        }

        adapter = new ListFriendsAdapter(getContext(), dataListFriend, this);
        recyclerListFrends.setAdapter(adapter);

        dialogFindAllFriend = new LovelyProgressDialog(getContext());
        dataListFriend.getListFriend().clear();

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
//                listFriendID = new ArrayList<>();
//                dataListFriend = new ListFriend();
                adapter.notifyDataSetChanged();
//                FriendDB.getInstance(getContext()).dropDB();
//                getListFriendUId();
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
        listFriendID.clear();
        dataListFriend.getListFriend().clear();
        adapter.notifyDataSetChanged();
        FriendDB.getInstance(getContext()).dropDB();
        getListFriendUId();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getContext().unregisterReceiver(deleteFriendReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ACTION_START_CHAT == requestCode && data != null && ListFriendsAdapter.mapMark != null) {
            ListFriendsAdapter.mapMark.put(data.getStringExtra(getActivity().getString(R.string.id_friend)), false);
        }
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
                            dialogFindAllFriend.dismiss();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    /**
     * Truy cap bang user lay thong tin id nguoi dung
     */
    private void getAllFriendInfo(final int index) {
        if (index == listFriendID.size()) {
            //save list friend
            adapter.notifyDataSetChanged();
            dialogFindAllFriend.dismiss();
            mSwipeRefreshLayout.setRefreshing(false);
        } else {
            final String id = listFriendID.get(index);
            mDatabaseRef.child(getActivity().getString(R.string.users) + "/" + id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                Friend user = new Friend();
                                HashMap mapUserInfo = (HashMap) dataSnapshot.getValue();
                                user.name = (String) mapUserInfo.get(getActivity().getString(R.string.name_field));
                                user.email = (String) mapUserInfo.get(getActivity().getString(R.string.email));
                                user.avatar = (String) mapUserInfo.get(getActivity().getString(R.string.avatar_field));
                                user.id = id;
                                user.idRoom = id.compareTo(StaticConfig.UID) > 0 ? (StaticConfig.UID + id).hashCode() + "" : "" + (id + StaticConfig.UID).hashCode();
                                dataListFriend.getListFriend().add(user);
                                FriendDB.getInstance(getContext()).addFriend(user);
                            }
                            getAllFriendInfo(index + 1);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }
    }

    public class FragFriendClickFloatButton implements View.OnClickListener {
        Context context;
        LovelyProgressDialog dialogWait;

        public FragFriendClickFloatButton() {
        }

        public FragFriendClickFloatButton getInstance(Context context) {
            this.context = context;
            dialogWait = new LovelyProgressDialog(context);
            return this;
        }

        @Override
        public void onClick(final View view) {
            new LovelyTextInputDialog(view.getContext(), R.style.EditTextTintTheme)
                    .setTopColorRes(R.color.colorPrimary)
                    .setTitle(getActivity().getString(R.string.add_friend))
                    .setMessage(getActivity().getString(R.string.enter_friend_email))
                    .setIcon(R.drawable.ic_add_friend)
                    .setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    .setInputFilter(getActivity().getString(R.string.email_not_found), text -> {
                        Pattern VALID_EMAIL_ADDRESS_REGEX =
                                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
                        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(text);
                        return matcher.find();
                    })
                    .setConfirmButton(android.R.string.ok, text -> {
                        findIDEmail(text);
                    })
                    .show();
        }

        /**
         * TIm id cua email tren server
         *
         * @param email
         */
        private void findIDEmail(final String email) {
            dialogWait.setCancelable(false)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle(getActivity().getString(R.string.finding_friend))
                    .setTopColorRes(R.color.colorPrimary)
                    .show();
            mDatabaseRef.child(getActivity().getString(R.string.users))
                    .orderByChild(getActivity().getString(R.string.email))
                    .equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            dialogWait.dismiss();
                            if (dataSnapshot.getValue() == null) {
                                new LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setIcon(R.drawable.ic_add_friend)
                                        .setTitle(getActivity().getString(R.string.failed))
                                        .setMessage(getActivity().getString(R.string.email_not_found))
                                        .show();
                            } else {

                                for (DataSnapshot emailSnapshot : dataSnapshot.getChildren()) {
                                    String id = emailSnapshot.child(getActivity().getString(R.string.user_id)).getValue().toString();

                                    if (TextUtils.isEmpty(id)) {
                                        new LovelyInfoDialog(context)
                                                .setTopColorRes(R.color.colorAccent)
                                                .setIcon(R.drawable.ic_add_friend)
                                                .setTitle(getActivity().getString(R.string.failed))
                                                .setMessage(getActivity().getString(R.string.invalid_email))
                                                .show();
                                    } else {
                                        HashMap userMap = (HashMap) ((HashMap) dataSnapshot.getValue())
                                                .get(id);
                                        Friend user = new Friend();
                                        user.name = (String) userMap.get(getActivity().getString(R.string.name_field));
                                        user.email = (String) userMap.get(getActivity().getString(R.string.email));
                                        user.avatar = (String) userMap.get(getActivity().getString(R.string.avatar_field));
                                        user.id = id;
                                        user.idRoom = id.compareTo(StaticConfig.UID) > 0 ?
                                                (StaticConfig.UID + id).hashCode() + "" : "" + (id + StaticConfig.UID)
                                                .hashCode();

                                        checkBeforAddFriend(id, user);
                                    }
                                }

                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        /**
         * Lay danh sach friend cua một UID
         */
        private void checkBeforAddFriend(final String idFriend, Friend userInfo) {
            dialogWait.setCancelable(false)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle(getActivity().getString(R.string.adding_friend))
                    .setTopColorRes(R.color.colorPrimary)
                    .show();

            if (listFriendID.contains(idFriend)) {
                dialogWait.dismiss();
                new LovelyInfoDialog(context)
                        .setTopColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_add_friend)
                        .setTitle(getActivity().getString(R.string.friend_title))
                        .setMessage("User " + userInfo.email + " was in your list friend!")
                        .show();
            } else {
                sendRequest(idFriend);
//                listFriendID.add(idFriend);
//                dataListFriend.getListFriend().add(userInfo);
//                FriendDB.getInstance(getContext()).addFriend(userInfo);
//                adapter.notifyDataSetChanged();
//            }
            }
        }

        private void sendRequest(String idFriend) {

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
                            new LovelyInfoDialog(context)
                                    .setTopColorRes(R.color.colorPrimary)
                                    .setIcon(R.drawable.ic_add_friend)
                                    .setTitle(getActivity().getString(R.string.success))
                                    .setMessage(getActivity().getString(R.string.send_request_friend_success))
                                    .show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        dialogWait.dismiss();
                        new LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_add_friend)
                                .setTitle(getActivity().getString(R.string.failed))
                                .setMessage(getActivity().getString(R.string.add_friend_failed))
                                .show();
                    });
        }
    }
}


