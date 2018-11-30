package com.project.chatflix.adapter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.activity.ChatActivity;
import com.project.chatflix.fragment.ChatFragment;
import com.project.chatflix.object.Friend;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.object.Message;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListFriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ListFriend listFriend;
    private Context context;
    private ChatFragment fragment;
    LovelyProgressDialog dialogWaitDeleting;
    private DatabaseReference mDatabaseRef;

    public ListFriendsAdapter(Context context, ListFriend listFriend, ChatFragment fragment) {
        this.listFriend = listFriend;
        this.context = context;
        this.fragment = fragment;
        dialogWaitDeleting = new LovelyProgressDialog(context);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new ItemFriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder1, final int position) {
        final String name = listFriend.getListFriend().get(position).name;
        final String id = listFriend.getListFriend().get(position).id;
        ItemFriendViewHolder holder = (ItemFriendViewHolder) holder1;
        holder.txtName.setText(name);

        addEvents(holder, position);
        getLastMessage(holder);

        if (listFriend.getListFriend().get(position).avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            holder.avata.setImageResource(R.drawable.default_avatar);
        } else {
            byte[] decodedString = Base64.decode(listFriend.getListFriend().get(position).avatar, Base64.DEFAULT);
            Bitmap src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.avata.setImageBitmap(src);
        }

        if (listFriend.getListFriend().get(position).status.isOnline) {
            holder.avata.setBorderWidth(6);
        } else {
            holder.avata.setBorderWidth(0);
        }

        mDatabaseRef.child(context.getString(R.string.users)).child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(context.getString(R.string.online))) {
                            String userOnline = dataSnapshot.child(context.getString(R.string.online)).getValue().toString();
                            if (userOnline.equals(context.getString(R.string.true_field))) {
                                holder.imgOnline.setVisibility(View.VISIBLE);
                            } else {
                                holder.imgOnline.setVisibility(View.INVISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void getLastMessage(ItemFriendViewHolder holder) {
        int position = holder.getAdapterPosition();
        String id = listFriend.getListFriend().get(position).id;
        String idRoom = listFriend.getListFriend().get(position).idRoom;

        mDatabaseRef.child(context.getString(R.string.message_table))
                .child(idRoom).limitToLast(1)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        try {
                            if (dataSnapshot.getChildrenCount() != 0) {

                                holder.txtMessage.setVisibility(View.VISIBLE);
                                holder.txtTime.setVisibility(View.VISIBLE);

                                Message message = dataSnapshot.getValue(Message.class);

                                String time = new SimpleDateFormat("EEE, d MMM yyyy").format(new Date(message.timestamp));
                                String today = new SimpleDateFormat("EEE, d MMM yyyy").format(new Date(System.currentTimeMillis()));
                                if (today.equals(time)) {
                                    holder.txtTime.setText(new SimpleDateFormat("HH:mm")
                                            .format(new Date(message.timestamp)));
                                } else {
                                    holder.txtTime.setText(new SimpleDateFormat("MMM d")
                                            .format(new Date(message.timestamp)));
                                }

                                if (!message.idSender.startsWith(id)) {
                                    holder.txtMessage.setText("You: " + message.text);
                                    holder.txtMessage.setTypeface(Typeface.DEFAULT);
                                    holder.txtName.setTypeface(Typeface.DEFAULT);
                                } else {
                                    holder.txtMessage.setText(message.text);
                                    holder.txtMessage.setTypeface(Typeface.DEFAULT_BOLD);
                                    holder.txtName.setTypeface(Typeface.DEFAULT_BOLD);

                                    if (!ChatFragment.firstLoad) {
                                        mDatabaseRef.child(context.getString(R.string.online_chat_table)).child(StaticConfig.UID)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        if (dataSnapshot.getValue() == null) {
                                                            putNotiMessageFriend(listFriend.getListFriend().get(position), message, idRoom);
                                                        } else {
                                                            if (!dataSnapshot.getValue().equals(idRoom + "")) {
                                                                putNotiMessageFriend(listFriend.getListFriend().get(position), message, idRoom);
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {

                                                    }
                                                });
                                    }

                                }

                            } else {
                                holder.txtMessage.setVisibility(View.GONE);
                                holder.txtTime.setVisibility(View.GONE);
                            }
                        } catch (Exception e) {
                            Log.e(getClass().getSimpleName(), e.toString());
                            Crashlytics.logException(e);
                        }
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

    }

    private void putNotiMessageFriend(Friend friend, Message message, String idRoom) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context);

        //Create the intent that’ll fire when the user taps the notification//
        Intent intent = new Intent(context, ChatActivity.class);

        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, friend.name);
        intent.putExtra(context.getString(R.string.kind_of_chat), context.getString(R.string.friend_chat));
        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ID, friend.id);
        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, idRoom);

        intent.putExtra(StaticConfig.CLICK_INTENT_FROM_NOTI, true);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pendingIntent);

        mBuilder.setSmallIcon(R.drawable.logo_1);
        mBuilder.setContentTitle(friend.name);
        mBuilder.setContentText(message.text);

        NotificationManager mNotificationManager =

                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(context.getString(R.string.app_name), 001, mBuilder.build());
    }

    private void addEvents(RecyclerView.ViewHolder holder, int position) {
        final String name = listFriend.getListFriend().get(position).name;
        final String id = listFriend.getListFriend().get(position).id;
        final String idRoom = listFriend.getListFriend().get(position).idRoom;
        final String avatar = listFriend.getListFriend().get(position).avatar;
        String friendName = (String) ((ItemFriendViewHolder) holder).txtName.getText();

        ((View) ((ItemFriendViewHolder) holder).txtName.getParent().getParent().getParent())
                .setOnClickListener(v -> {
                    try {
                        ((ItemFriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT);
                        ((ItemFriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT);
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, name);
                        intent.putExtra(context.getString(R.string.kind_of_chat), context.getString(R.string.friend_chat));
                        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ID, id);
                        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, idRoom);
                        ChatActivity.bitmapAvataFriend = new HashMap<>();

                        if (!avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                            byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                            ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                        } else {
                            ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avatar));
                        }
                        fragment.startActivityForResult(intent, ChatFragment.ACTION_START_CHAT);
                        mDatabaseRef.child(context.getString(R.string.online_chat_table)).child(StaticConfig.UID).setValue(idRoom);
                        mDatabaseRef.child(context.getString(R.string.online_chat_table)).keepSynced(false);
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), e.toString()); Crashlytics.logException(e);
                    }
                });

        //nhấn giữ để xóa bạn
        ((View) ((ItemFriendViewHolder) holder).txtName.getParent().getParent().getParent())
                .setOnLongClickListener(v -> {

                    new AlertDialog.Builder(context)
                            .setTitle(context.getString(R.string.delete_friend))
                            .setMessage(context.getString(R.string.are_you_sure_to_delete) + friendName + "?")
                            .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                final String idFriendRemoval = listFriend.getListFriend()
                                        .get(position).id;
                                dialogWaitDeleting.setTitle(context.getString(R.string.deleting))
                                        .setCancelable(false)
                                        .setTopColorRes(R.color.colorAccent)
                                        .show();
                                deleteFriend(idFriendRemoval, position);
                            })
                            .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                            }).show();

                    return true;
                });
    }

    @Override
    public int getItemCount() {
        return listFriend.getListFriend() != null ? listFriend.getListFriend().size() : 0;
    }

    private void deleteFriend(final String idFriend, int position) {
        try {
            if (idFriend != null) {
                mDatabaseRef.child(context.getString(R.string.users)).child(StaticConfig.UID)
                        .child(context.getString(R.string.friend_field)).orderByValue().equalTo(idFriend)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getValue() == null) {
                                    dialogWaitDeleting.dismiss();
                                    new LovelyInfoDialog(context)
                                            .setTopColorRes(R.color.colorAccent)
                                            .setTitle(context.getString(R.string.error))
                                            .setMessage(context.getString(R.string.error_delete_friend))
                                            .show();
                                } else {
                                    String idRemoval = ((HashMap) dataSnapshot.getValue()).keySet().iterator()
                                            .next().toString();
                                    mDatabaseRef.child(context.getString(R.string.users)).child(StaticConfig.UID)
                                            .child(context.getString(R.string.friend_field)).child(idRemoval).removeValue()
                                            .addOnCompleteListener(task -> {
                                                dialogWaitDeleting.dismiss();

                                                new LovelyInfoDialog(context)
                                                        .setTopColorRes(R.color.colorAccent)
                                                        .setTitle(context.getString(R.string.success))
                                                        .setMessage(context.getString(R.string.delete_friend_successfully))
                                                        .show();

                                                Intent intentDeleted = new Intent(ChatFragment.ACTION_DELETE_FRIEND);
                                                intentDeleted.putExtra(context.getString(R.string.id_friend), idFriend);
                                                context.sendBroadcast(intentDeleted);
                                            })
                                            .addOnFailureListener(e -> {
                                                dialogWaitDeleting.dismiss();
                                                new LovelyInfoDialog(context)
                                                        .setTopColorRes(R.color.colorAccent)
                                                        .setTitle(context.getString(R.string.error))
                                                        .setMessage(context.getString(R.string.error_delete_friend))
                                                        .show();
                                            });
                                    mDatabaseRef.child(context.getString(R.string.users))
                                            .child(idFriend).child(context.getString(R.string.friend_field))
                                            .orderByValue()
                                            .equalTo(StaticConfig.UID)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    String idRemoval = ((HashMap) dataSnapshot.getValue()).keySet().iterator().next().toString();
                                                    mDatabaseRef.child(context.getString(R.string.users))
                                                            .child(idFriend).child(context.getString(R.string.friend_field))
                                                            .child(idRemoval).removeValue();
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });

                                    mDatabaseRef.child(context.getString(R.string.message_table))
                                            .child(listFriend.getListFriend().get(position).idRoom)
                                            .removeValue();

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
                        .setTitle(context.getString(R.string.error))
                        .setMessage(context.getString(R.string.error_delete_friend))
                        .show();
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.toString()); Crashlytics.logException(e);
        }
    }

    class ItemFriendViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView avata;
        public TextView txtName, txtTime, txtMessage;
        public ImageView imgOnline;

        ItemFriendViewHolder(View itemView) {
            super(itemView);
            avata = itemView.findViewById(R.id.icon_avata);
            txtName = itemView.findViewById(R.id.txtName);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            imgOnline = itemView.findViewById(R.id.user_single_online_icon);
        }

    }
}


