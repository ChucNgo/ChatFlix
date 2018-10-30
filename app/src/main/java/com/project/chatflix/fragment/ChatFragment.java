package com.project.chatflix.fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.activity.ChatActivity;
import com.project.chatflix.database.FriendDB;
import com.project.chatflix.object.Friend;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

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
        Log.e("test","fragment created");

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
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                listFriendID.clear();
                Log.e("refresh","chạy " +"listFriendID.clear();");
                dataListFriend.getListFriend().clear();
                Log.e("refresh","chạy " +"dataListFriend.getListFriend().clear();");
                adapter.notifyDataSetChanged();
                Log.e("refresh","chạy " +"adapter.notifyDataSetChanged();");
                FriendDB.getInstance(getContext()).dropDB();
                Log.e("refresh","chạy " +"FriendDB.getInstance(getContext()).dropDB();");
//                detectFriendOnline.cancel();
                getListFriendUId();
                Log.e("refresh","chạy " +"getListFriendUId();");
                // Kiem tra
//                String listfriend = "";
//                for(int i = 0; i < dataListFriend.getListFriend().size();i++){
//                    Log.e("loop","bắt đầu lấy list friend thứ" + (i+1));
//                    listfriend += dataListFriend.getListFriend().get(i).getName() + "\n";
//                }
//                Log.e("Listfriend",listfriend);
            }
        });


        adapter = new ListFriendsAdapter(getContext(), dataListFriend, this);
        recyclerListFrends.setAdapter(adapter);

        dialogFindAllFriend = new LovelyProgressDialog(getContext());


        // Vào phát là reload friend list luôn, nếu để như trước trường hợp firsttimeaccess
//        if (listFriendID == null) {
        // Thêm vào để xóa hoàn toàn dataListFriend cũ
//        listFriendID.clear();
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
        for(int i = 0; i < dataListFriend.getListFriend().size();i++){
            Log.e("loop","bắt đầu lấy list friend thứ" + (i+1));
            listfriend += dataListFriend.getListFriend().get(i).getName() + "\n";
        }
        Log.e("Listfriend",listfriend);
//        }

        deleteFriendReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String idDeleted = intent.getExtras().getString("idFriend");
                for (Friend friend : dataListFriend.getListFriend()) {
                    if(idDeleted.equals(friend.id)){
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
    public void onDestroyView (){
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
                    .setInputFilter("Email not found", new LovelyTextInputDialog.TextFilter() {
                        @Override
                        public boolean check(String text) {
                            Pattern VALID_EMAIL_ADDRESS_REGEX =
                                    Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
                            Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(text);
                            return matcher.find();
                        }
                    })
                    .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                        @Override
                        public void onTextInputConfirmed(String text) {
                            //Tim id user id
                            findIDEmail(text);
                            //Check xem da ton tai ban ghi friend chua
                            //Ghi them 1 ban ghi
                        }
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
                    }else {

                        for (DataSnapshot emailSnapshot : dataSnapshot.getChildren()){
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
                Log.e("add","chạy " +"adapter.notifyDataSetChanged();");
//                listFriendID.clear();
//                dataListFriend.getListFriend().clear();

//                listFriendID = new ArrayList<>();
//                dataListFriend = new ListFriend();

                String listfriend = "";
                for(int i = 0; i < dataListFriend.getListFriend().size();i++){
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
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        addFriend(idFriend, false);
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    dialogWait.dismiss();
                                    new LovelyInfoDialog(context)
                                            .setTopColorRes(R.color.colorAccent)
                                            .setIcon(R.drawable.ic_add_friend)
                                            .setTitle("False")
                                            .setMessage("False to add friend success")
                                            .show();
                                }
                            });
                } else {
                    FirebaseDatabase.getInstance().getReference().child("Users").child(idFriend)
                            .child("friend").push().setValue(currentUser.getUid())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                addFriend(null, false);
                            }
                        }
                    })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    dialogWait.dismiss();
                                    new LovelyInfoDialog(context)
                                            .setTopColorRes(R.color.colorAccent)
                                            .setIcon(R.drawable.ic_add_friend)
                                            .setTitle("False")
                                            .setMessage("False to add friend success")
                                            .show();
                                }
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
            Log.e("run","đã refresh được!");
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

class ListFriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ListFriend listFriend;
    private Context context;
    public static Map<String, Query> mapQuery;
    public static Map<String, DatabaseReference> mapQueryOnline;
    public static Map<String, ChildEventListener> mapChildListener;
    public static Map<String, ChildEventListener> mapChildListenerOnline;
    public static Map<String, Boolean> mapMark;
    private ChatFragment fragment;
    LovelyProgressDialog dialogWaitDeleting;

    public ListFriendsAdapter(Context context, ListFriend listFriend, ChatFragment fragment) {
        this.listFriend = listFriend;
        this.context = context;
        mapQuery = new HashMap<>();
        mapChildListener = new HashMap<>();
        mapMark = new HashMap<>();
        mapChildListenerOnline = new HashMap<>();
        mapQueryOnline = new HashMap<>();
        this.fragment = fragment;
        dialogWaitDeleting = new LovelyProgressDialog(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_friend, parent, false);
        return new ItemFriendViewHolder(context, view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final String name = listFriend.getListFriend().get(position).name;
        final String id = listFriend.getListFriend().get(position).id;
        final String idRoom = listFriend.getListFriend().get(position).idRoom;
        final String avatar = listFriend.getListFriend().get(position).avatar;
        ((ItemFriendViewHolder) holder).txtName.setText(name);


        ((View) ((ItemFriendViewHolder) holder).txtName.getParent().getParent().getParent())
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((ItemFriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT);
                        ((ItemFriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT);
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, name);
                        intent.putExtra("Kind Of Chat","FriendChat");
                        ArrayList<CharSequence> idFriend = new ArrayList<CharSequence>();
                        idFriend.add(id);
                        intent.putCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID, idFriend);
                        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, idRoom);
                        ChatActivity.bitmapAvataFriend = new HashMap<>();

                        if (!avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                            byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                            ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                        } else {
                            ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avatar));
                        }

                        mapMark.put(id, null);
                        fragment.startActivityForResult(intent, ChatFragment.ACTION_START_CHAT);
                    }
                });

        //nhấn giữ để xóa bạn
        ((View) ((ItemFriendViewHolder) holder).txtName.getParent().getParent().getParent())
                .setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        String friendName = (String)((ItemFriendViewHolder) holder).txtName.getText();

                        new AlertDialog.Builder(context)
                                .setTitle("Delete Friend")
                                .setMessage("Are you sure want to delete "+friendName+ "?")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        final String idFriendRemoval = listFriend.getListFriend()
                                                                        .get(position).id;
                                        dialogWaitDeleting.setTitle("Deleting...")
                                                .setCancelable(false)
                                                .setTopColorRes(R.color.colorAccent)
                                                .show();
                                        deleteFriend(idFriendRemoval);
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                }).show();

                        return true;
                    }
                });


        if (listFriend.getListFriend().get(position).message.text.length() > 0) {

            ((ItemFriendViewHolder) holder).txtMessage.setVisibility(View.VISIBLE);
            ((ItemFriendViewHolder) holder).txtTime.setVisibility(View.VISIBLE);

            if (!listFriend.getListFriend().get(position).message.text.startsWith(id)) {
                ((ItemFriendViewHolder) holder).txtMessage.setText(listFriend.getListFriend()
                        .get(position).message.text);
                ((ItemFriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT);
                ((ItemFriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT);
            } else {
                ((ItemFriendViewHolder) holder).txtMessage.setText(listFriend.getListFriend()
                        .get(position).message.text.substring((id + "").length()));
                ((ItemFriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT_BOLD);
                ((ItemFriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT_BOLD);
            }

            String time = new SimpleDateFormat("EEE, d MMM yyyy").format(new Date(listFriend.getListFriend().get(position).message.timestamp));
            String today = new SimpleDateFormat("EEE, d MMM yyyy").format(new Date(System.currentTimeMillis()));
            if (today.equals(time)) {
                ((ItemFriendViewHolder) holder).txtTime.setText(new SimpleDateFormat("HH:mm")
                        .format(new Date(listFriend.getListFriend().get(position).message.timestamp)));
            } else {
                ((ItemFriendViewHolder) holder).txtTime.setText(new SimpleDateFormat("MMM d")
                        .format(new Date(listFriend.getListFriend().get(position).message.timestamp)));
            }

        } else {
            ((ItemFriendViewHolder) holder).txtMessage.setVisibility(View.GONE);
            ((ItemFriendViewHolder) holder).txtTime.setVisibility(View.GONE);
            if (mapQuery.get(id) == null && mapChildListener.get(id) == null) {
                mapQuery.put(id, FirebaseDatabase.getInstance().getReference()
                        .child("message/" + idRoom)
                        .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode()))
                        .limitToLast(1));
                try {
                    mapChildListener.put(id, new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                            if (mapMark.get(id) != null) {
                                if (!mapMark.get(id)) {
                                    listFriend.getListFriend()
                                            .get(position).message.text = id + mapMessage.get("text");
                                } else {
                                    listFriend.getListFriend()
                                            .get(position).message.text = (String) mapMessage.get("text");
                                }
                                notifyDataSetChanged();
                                mapMark.put(id, false);
                            } else {
                                listFriend.getListFriend().get(position).message.text = (String) mapMessage.get("text");
                                notifyDataSetChanged();
                            }
                            listFriend.getListFriend().get(position).message.timestamp = (long) mapMessage.get("timestamp");
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }


                mapQuery.get(id).addChildEventListener(mapChildListener.get(id));
                mapMark.put(id, true);
            } else {
                mapQuery.get(id).removeEventListener(mapChildListener.get(id));
                mapQuery.get(id).addChildEventListener(mapChildListener.get(id));
                mapMark.put(id, true);
            }
        }

        if (listFriend.getListFriend().get(position).avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            ((ItemFriendViewHolder) holder).avata.setImageResource(R.drawable.default_avatar);
        } else {
            byte[] decodedString = Base64.decode(listFriend.getListFriend().get(position).avatar, Base64.DEFAULT);
            Bitmap src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ((ItemFriendViewHolder) holder).avata.setImageBitmap(src);
        }


        if (mapQueryOnline.get(id) == null && mapChildListenerOnline.get(id) == null) {
            mapQueryOnline.put(id, FirebaseDatabase.getInstance().getReference().child("Users/" + id+"/status"));
            mapChildListenerOnline.put(id, new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    if(dataSnapshot.getValue() != null && dataSnapshot.getKey().equals("isOnline")) {
                        Log.d("ChatFragment add " + id,  (boolean)dataSnapshot.getValue() +"");
                        listFriend.getListFriend().get(position).status.isOnline = (boolean)dataSnapshot.getValue();
                        notifyDataSetChanged();
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    if(dataSnapshot.getValue() != null&& dataSnapshot.getKey().equals("isOnline")) {
                        Log.d("ChatFragment change " + id,  (boolean)dataSnapshot.getValue() +"");
                        listFriend.getListFriend().get(position).status.isOnline = (boolean)dataSnapshot.getValue();
                        notifyDataSetChanged();
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            mapQueryOnline.get(id).addChildEventListener(mapChildListenerOnline.get(id));
        }

        if (listFriend.getListFriend().get(position).status.isOnline) {
            ((ItemFriendViewHolder) holder).avata.setBorderWidth(6);
        } else {
            ((ItemFriendViewHolder) holder).avata.setBorderWidth(0);
        }

        FirebaseDatabase.getInstance().getReference().child("Users").child(id)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                if(dataSnapshot.hasChild("online")) {

                    String userOnline = dataSnapshot.child("online").getValue().toString();

                    if(userOnline.equals("true")){

                        ((ItemFriendViewHolder) holder).imgOnline.setVisibility(View.VISIBLE);

                    } else {

                        ((ItemFriendViewHolder) holder).imgOnline.setVisibility(View.INVISIBLE);

                    }

                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return listFriend.getListFriend() != null ? listFriend.getListFriend().size() : 0;
    }

    /**
     * Delete friend
     *
     * @param idFriend
     */
    private void deleteFriend(final String idFriend) {
        if (idFriend != null) {
            Log.e("id","idFriend : " + idFriend);
            final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser.getUid())
                    .child("friend").child("")
                    .orderByValue().equalTo(idFriend).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (dataSnapshot.getValue() == null) {
                        //email not found
                        dialogWaitDeleting.dismiss();
                        new LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorAccent)
                                .setTitle("Error")
                                .setMessage("Error occurred during deleting friend")
                                .show();
                    } else {
                        String idRemoval = ((HashMap) dataSnapshot.getValue()).keySet().iterator()
                                .next().toString();
                        FirebaseDatabase.getInstance().getReference().child("Users")
                                .child(currentUser.getUid()).child("friend").child("")
                                .child(idRemoval).removeValue()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        dialogWaitDeleting.dismiss();

                                        new LovelyInfoDialog(context)
                                                .setTopColorRes(R.color.colorAccent)
                                                .setTitle("Success")
                                                .setMessage("Friend deleting successfully")
                                                .show();

                                        Intent intentDeleted = new Intent(ChatFragment.ACTION_DELETE_FRIEND);
                                        intentDeleted.putExtra("idFriend", idFriend);
                                        context.sendBroadcast(intentDeleted);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        dialogWaitDeleting.dismiss();
                                        new LovelyInfoDialog(context)
                                                .setTopColorRes(R.color.colorAccent)
                                                .setTitle("Error")
                                                .setMessage("Error occurred during deleting friend")
                                                .show();
                                    }
                                });
                        FirebaseDatabase.getInstance().getReference().child("Users").child(idFriend).child("friend")
                                .child("")
                                .orderByValue().equalTo(currentUser.getUid())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String idRemoval = ((HashMap) dataSnapshot.getValue()).keySet().iterator().next().toString();
                                FirebaseDatabase.getInstance().getReference().child("Users").child(idFriend)
                                        .child("friend").child("")
                                        .child(idRemoval).removeValue();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        FirebaseDatabase.getInstance().getReference().child("message")
                                .child(String.valueOf(idFriend.hashCode()))
                                .child("")
                                .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode()))
                                .removeValue();

                        FirebaseDatabase.getInstance().getReference().child("message")
                                .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode()))
                                .child("")
                                .child(String.valueOf(idFriend.hashCode())).removeValue();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            dialogWaitDeleting.dismiss();
            new LovelyInfoDialog(context)
                    .setTopColorRes(R.color.colorPrimary)
                    .setTitle("Error")
                    .setMessage("Error occurred during deleting friend")
                    .show();
        }
    }
}

class ItemFriendViewHolder extends RecyclerView.ViewHolder{
    public CircleImageView avata;
    public TextView txtName, txtTime, txtMessage;
    public ImageView imgOnline;

    private Context context;

    ItemFriendViewHolder(Context context, View itemView) {
        super(itemView);
        avata = (CircleImageView) itemView.findViewById(R.id.icon_avata);
        txtName = (TextView) itemView.findViewById(R.id.txtName);
        txtTime = (TextView) itemView.findViewById(R.id.txtTime);
        txtMessage = (TextView) itemView.findViewById(R.id.txtMessage);
        imgOnline = (ImageView) itemView.findViewById(R.id.user_single_online_icon);
        this.context = context;
    }

}
