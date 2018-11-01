package com.project.chatflix.fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.adapter.ListFriendsAdapter;
import com.project.chatflix.database.FriendDB;
import com.project.chatflix.object.Friend;
import com.project.chatflix.object.ListFriend;
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
public class ChatFragment extends Fragment {

    private FirebaseAuth mAuth;
    private RecyclerView recyclerListFrends;
    private ListFriendsAdapter adapter;
    public FragFriendClickFloatButton onClickFloatButton;
    private ListFriend dataListFriend = null;
    private ArrayList<String> listFriendID = null;
    private LovelyProgressDialog dialogFindAllFriend;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    public static int ACTION_START_CHAT = 1;

    public static final String ACTION_DELETE_FRIEND = "fpoly.com.chatflix.DELETE_FRIEND";

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
        Log.e("test", "fragment created");

        mAuth = FirebaseAuth.getInstance();

        if (dataListFriend == null) {
            dataListFriend = FriendDB.getInstance(getContext()).getListFriend();
            if (dataListFriend.getListFriend().size() > 0) {
                listFriendID = new ArrayList<>();
                for (Friend friend : dataListFriend.getListFriend()) {
                    listFriendID.add(friend.id);
                }

            }
        }
        View layout = inflater.inflate(R.layout.fragment_chat, container, false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        recyclerListFrends = (RecyclerView) layout.findViewById(R.id.recycleListFriend);
        recyclerListFrends.setLayoutManager(linearLayoutManager);
        mSwipeRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            listFriendID.clear();
            dataListFriend.getListFriend().clear();
            adapter.notifyDataSetChanged();
            FriendDB.getInstance(getContext()).dropDB();
            getListFriendUId();
        });


        adapter = new ListFriendsAdapter(getContext(), dataListFriend, this);
        recyclerListFrends.setAdapter(adapter);

        dialogFindAllFriend = new LovelyProgressDialog(getContext());
        /** thằng này gây ra lỗi refresh sau khi add hoặc xóa friend */
        dataListFriend.getListFriend().clear();

        listFriendID = new ArrayList<>();
        dialogFindAllFriend.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle("Get all friend....")
                .setTopColorRes(R.color.colorPrimary)
                .show();
        getListFriendUId();
        String listfriend = "";
        for (int i = 0; i < dataListFriend.getListFriend().size(); i++) {
            Log.e("loop", "bắt đầu lấy list friend thứ" + (i + 1));
            listfriend += dataListFriend.getListFriend().get(i).getName() + "\n";
        }
        Log.e("Listfriend", listfriend);

        deleteFriendReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String idDeleted = intent.getExtras().getString("idFriend");
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

        return layout;
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
            ListFriendsAdapter.mapMark.put(data.getStringExtra("idFriend"), false);
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
                    .setTitle("Add friend")
                    .setMessage("Enter friend email")
                    .setIcon(R.drawable.ic_add_friend)
                    .setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    .setInputFilter("Email not found", text -> {
                        Pattern VALID_EMAIL_ADDRESS_REGEX =
                                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
                        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(text);
                        return matcher.find();
                    })
                    .setConfirmButton(android.R.string.ok, text -> {
                        //Tim id user id
                        findIDEmail(text);
                        //Check xem da ton tai ban ghi friend chua
                        //Ghi them 1 ban ghi
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
                    .setTitle("Finding friend....")
                    .setTopColorRes(R.color.colorPrimary)
                    .show();
            FirebaseDatabase.getInstance().getReference().child("Users").orderByChild("email")
                    .equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            dialogWait.dismiss();

//                    Map<String, Object> allUserbyId = (Map<String, Object>) dataSnapshot.getValue();
//                    List<String> values = allUserbyId.values();
                            if (dataSnapshot.getValue() == null) {
                                //email not found
                                new LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setIcon(R.drawable.ic_add_friend)
                                        .setTitle("Fail")
                                        .setMessage("Email not found")
                                        .show();
                            } else {

                                for (DataSnapshot emailSnapshot : dataSnapshot.getChildren()) {
                                    String id = emailSnapshot.child("user_id").getValue().toString();

                                    if (id.equals("")) {
                                        new LovelyInfoDialog(context)
                                                .setTopColorRes(R.color.colorAccent)
                                                .setIcon(R.drawable.ic_add_friend)
                                                .setTitle("Fail")
                                                .setMessage("Email not valid")
                                                .show();
                                    } else {
                                        Log.e("Found Email", "found email " + email);
                                        HashMap userMap = (HashMap) ((HashMap) dataSnapshot.getValue())
                                                .get(id);
                                        Friend user = new Friend();
                                        user.name = (String) userMap.get("name");
                                        user.email = (String) userMap.get("email");
                                        user.avatar = (String) userMap.get("avatar");
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
                    .setTitle("Add friend....")
                    .setTopColorRes(R.color.colorPrimary)
                    .show();
            // Thêm thử 2 thằng gây ra lỗi null
//            listFriendID = new ArrayList<>();
//            dataListFriend = FriendDB.getInstance(getContext()).getListFriend();
//            adapter = new ListFriendsAdapter(getContext(), dataListFriend, this);

            //Check xem da ton tai id trong danh sach id chua
            if (listFriendID.contains(idFriend)) {
                dialogWait.dismiss();
                new LovelyInfoDialog(context)
                        .setTopColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_add_friend)
                        .setTitle("Friend")
                        .setMessage("User " + userInfo.email + " was in your list friend!")
                        .show();
            } else {
                addFriend(idFriend, true);

                listFriendID.add(idFriend);
                dataListFriend.getListFriend().add(userInfo);

                FriendDB.getInstance(getContext()).addFriend(userInfo);
                Log.e("add", "chạy " + "adapter.notifyDataSetChanged();");
//                listFriendID.clear();
//                dataListFriend.getListFriend().clear();

//                listFriendID = new ArrayList<>();
//                dataListFriend = new ListFriend();

                String listfriend = "";
                for (int i = 0; i < dataListFriend.getListFriend().size(); i++) {
                    listfriend += dataListFriend.getListFriend().get(i).getName() + "\n";
                }
//                Log.e("Listfriend",listfriend);

                adapter.notifyDataSetChanged();
//                Log.e("add","chạy " +"adapter.notifyDataSetChanged();");
//                FriendDB.getInstance(getContext()).dropDB();
//                getListFriendUId();
//            }
            }
        }

        /**
         * Add friend
         *
         * @param idFriend
         */
        private void addFriend(final String idFriend, boolean isIdFriend) {
            if (idFriend != null) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (isIdFriend) {


                    FirebaseDatabase.getInstance().getReference().child("Users")
                            .child(currentUser.getUid())
                            .child("friend").push().setValue(idFriend)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    addFriend(idFriend, false);
                                }
                            })
                            .addOnFailureListener(e -> {
                                dialogWait.dismiss();
                                new LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setIcon(R.drawable.ic_add_friend)
                                        .setTitle("False")
                                        .setMessage("False to add friend success")
                                        .show();
                            });
                } else {
                    FirebaseDatabase.getInstance().getReference().child("Users").child(idFriend)
                            .child("friend").push().setValue(currentUser.getUid())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    addFriend(null, false);
                                }
                            })
                            .addOnFailureListener(e -> {
                                dialogWait.dismiss();
                                new LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setIcon(R.drawable.ic_add_friend)
                                        .setTitle("False")
                                        .setMessage("False to add friend success")
                                        .show();
                            });
                }
            } else {
                dialogWait.dismiss();
                new LovelyInfoDialog(context)
                        .setTopColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_add_friend)
                        .setTitle("Success")
                        .setMessage("Add friend success")
                        .show();
            }
        }

    }

    /**
     * Lay danh sach ban be tren server
     */
    private void getListFriendUId() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        FirebaseDatabase.getInstance().getReference().child("Users")
                .child(currentUser.getUid()).child("friend")
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
            Log.e("run", "đã refresh được!");
            dialogFindAllFriend.dismiss();
            mSwipeRefreshLayout.setRefreshing(false);
//            detectFriendOnline.start();
        } else {
            final String id = listFriendID.get(index);
            FirebaseDatabase.getInstance().getReference().child("Users/" + id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                Friend user = new Friend();
                                HashMap mapUserInfo = (HashMap) dataSnapshot.getValue();
                                user.name = (String) mapUserInfo.get("name");
                                user.email = (String) mapUserInfo.get("email");
                                user.avatar = (String) mapUserInfo.get("avatar");
                                user.id = id;
                                // Một thuật toán khá là khó hiểu =)))
                                user.idRoom = (StaticConfig.UID + id).hashCode() + "";
//                        user.idRoom = "123";
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
}


