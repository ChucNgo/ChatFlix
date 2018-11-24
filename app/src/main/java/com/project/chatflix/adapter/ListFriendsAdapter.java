package com.project.chatflix.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.project.chatflix.fragment.ChatFragment;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListFriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ListFriend listFriend;
    private Context context;
    public static Map<String, Boolean> mapMark;
    private ChatFragment fragment;
    LovelyProgressDialog dialogWaitDeleting;
    private DatabaseReference mDatabaseRef;

    public ListFriendsAdapter(Context context, ListFriend listFriend, ChatFragment fragment) {
        this.listFriend = listFriend;
        this.context = context;
        mapMark = new HashMap<>();
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
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final String name = listFriend.getListFriend().get(position).name;
        final String id = listFriend.getListFriend().get(position).id;
        final String idRoom = listFriend.getListFriend().get(position).idRoom;
        ((ItemFriendViewHolder) holder).txtName.setText(name);

        addEvents(holder, position);

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
            mapMark.put(id, true);
        }

        if (listFriend.getListFriend().get(position).avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            ((ItemFriendViewHolder) holder).avata.setImageResource(R.drawable.default_avatar);
        } else {
            byte[] decodedString = Base64.decode(listFriend.getListFriend().get(position).avatar, Base64.DEFAULT);
            Bitmap src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ((ItemFriendViewHolder) holder).avata.setImageBitmap(src);
        }

        if (listFriend.getListFriend().get(position).status.isOnline) {
            ((ItemFriendViewHolder) holder).avata.setBorderWidth(6);
        } else {
            ((ItemFriendViewHolder) holder).avata.setBorderWidth(0);
        }

        mDatabaseRef.child(context.getString(R.string.users)).child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(context.getString(R.string.online))) {
                            String userOnline = dataSnapshot.child(context.getString(R.string.online)).getValue().toString();
                            if (userOnline.equals(context.getString(R.string.true_field))) {
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

    private void addEvents(RecyclerView.ViewHolder holder, int position) {
        final String name = listFriend.getListFriend().get(position).name;
        final String id = listFriend.getListFriend().get(position).id;
        final String idRoom = listFriend.getListFriend().get(position).idRoom;
        final String avatar = listFriend.getListFriend().get(position).avatar;

        ((View) ((ItemFriendViewHolder) holder).txtName.getParent().getParent().getParent())
                .setOnClickListener(v -> {
                    ((ItemFriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT);
                    ((ItemFriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT);
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, name);
                    intent.putExtra(context.getString(R.string.kind_of_chat), context.getString(R.string.friend_chat));
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
                });

        //nhấn giữ để xóa bạn
        ((View) ((ItemFriendViewHolder) holder).txtName.getParent().getParent().getParent())
                .setOnLongClickListener(v -> {
                    String friendName = (String) ((ItemFriendViewHolder) holder).txtName.getText();

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
                                deleteFriend(idFriendRemoval);
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

    /**
     * Delete friend
     *
     * @param idFriend
     */
    private void deleteFriend(final String idFriend) {
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
                                mDatabaseRef.child(context.getString(R.string.users))
                                        .child(StaticConfig.UID).child(context.getString(R.string.friend_field))
                                        .child(idRemoval).removeValue()
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
                                        .child(idFriend).child(context.getString(R.string.friend_field)).orderByValue().equalTo(StaticConfig.UID)
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
                                        .child(String.valueOf(idFriend.hashCode()))
                                        .child(String.valueOf(StaticConfig.UID.hashCode()))
                                        .removeValue();

                                mDatabaseRef.child(context.getString(R.string.message_table))
                                        .child(String.valueOf(StaticConfig.UID.hashCode()))
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
                    .setTitle(context.getString(R.string.error))
                    .setMessage(context.getString(R.string.error_delete_friend))
                    .show();
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


