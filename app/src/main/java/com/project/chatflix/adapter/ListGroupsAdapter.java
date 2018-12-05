package com.project.chatflix.adapter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
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
import com.project.chatflix.fragment.GroupFragment;
import com.project.chatflix.object.Friend;
import com.project.chatflix.object.Group;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.object.Message;
import com.project.chatflix.utils.StaticConfig;

import java.util.ArrayList;
import java.util.HashMap;

public class ListGroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Group> listGroup;
    private ListFriend listFriend;
    private Context context;
    private DatabaseReference mDatabaseRef;

    public ListGroupsAdapter(Context context, ArrayList<Group> listGroup) {
        this.context = context;
        this.listGroup = listGroup;
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        listFriend = ChatFragment.dataListFriend;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new ItemGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder1, final int position) {

        try {
            ItemGroupViewHolder holder = (ItemGroupViewHolder) holder1;

            final String groupName = listGroup.get(position).groupInfo.get(context.getString(R.string.name_field));
            if (groupName != null && groupName.length() > 0) {
                holder.txtGroupName.setText(groupName);
                holder.iconGroup.setText((groupName.charAt(0) + "").toUpperCase());
            }

            holder.btnMore.setOnClickListener(view -> {
                view.setTag(new Object[]{groupName, position});
                view.getParent().showContextMenuForChild(view);
            });
            ((RelativeLayout) holder.txtGroupName.getParent())
                    .setOnClickListener(view -> {
                        listFriend = ChatFragment.dataListFriend;

                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, groupName);
                        ArrayList<CharSequence> idFriend = new ArrayList<>();
                        ChatActivity.bitmapAvataFriend = new HashMap<>();

                        for (String id : listGroup.get(position).member) {
                            idFriend.add(id);
                            String avatar = listFriend.getAvataById(id);

                            if (!avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                                ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                            } else if (avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avatar));
                            } else {
                                ChatActivity.bitmapAvataFriend.put(id, null);
                            }
                        }
                        String idRoom = listGroup.get(position).id;
                        intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, idRoom);
                        intent.putExtra(context.getString(R.string.kind_of_chat), context.getString(R.string.group_chat));
                        context.startActivity(intent);

                        mDatabaseRef.child(context.getString(R.string.online_chat_table)).child(StaticConfig.UID).setValue(idRoom);
                        mDatabaseRef.child(context.getString(R.string.online_chat_table)).keepSynced(false);
                    });

            getLastMessage(holder, groupName);
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    private void getLastMessage(ItemGroupViewHolder holder, String groupName) {
        try {
            int position = holder.getAdapterPosition();
            String idRoom = listGroup.get(position).id;

            mDatabaseRef.child(context.getString(R.string.message_table))
                    .child(idRoom).limitToLast(1)
                    .addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            try {
                                if (dataSnapshot.getChildrenCount() != 0) {
                                    Message message = dataSnapshot.getValue(Message.class);
                                    if (!message.idSender.startsWith(StaticConfig.UID)) {
                                        if (!GroupFragment.firstLoad) {
                                            mDatabaseRef.child(context.getString(R.string.online_chat_table)).child(StaticConfig.UID)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {

                                                            try {
                                                                Friend friendChoose = null;

                                                                for (Friend friend : listFriend.getListFriend()) {
                                                                    if (friend.id.equalsIgnoreCase(message.idSender)) {
                                                                        friendChoose = friend;
                                                                    }
                                                                }

                                                                if (dataSnapshot.getValue() == null) {
                                                                    putNotiMessageGroup(friendChoose, message, listGroup.get(position));
                                                                } else {
                                                                    if (!dataSnapshot.getValue().equals(idRoom + "")) {
                                                                        putNotiMessageGroup(friendChoose, message, listGroup.get(position));
                                                                    }
                                                                }
                                                            } catch (Exception e) {
                                                                Log.e(getClass().getName(), e.toString()); Crashlytics.logException(e);
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {

                                                        }
                                                    });
                                        }
                                    }
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
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }

    }

    private void putNotiMessageGroup(Friend friend, Message message, Group group) {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context);

            //Create the intent that’ll fire when the user taps the notification/
            Intent intent = new Intent(context, ChatActivity.class);

            intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, group.groupInfo.get(context.getString(R.string.name_field)));
            intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, group.id);
            intent.putExtra(context.getString(R.string.kind_of_chat), context.getString(R.string.group_chat));

            intent.putExtra(StaticConfig.CLICK_INTENT_FROM_NOTI, true);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(pendingIntent);

            mBuilder.setSmallIcon(R.drawable.logo_1);
            mBuilder.setContentTitle(friend.name + " " +
                    context.getString(R.string.sent_to) + " " +
                    group.groupInfo.get(context.getString(R.string.name_field)));
            mBuilder.setContentText(message.text);

            NotificationManager mNotificationManager =

                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(context.getString(R.string.app_name), 001, mBuilder.build());
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    @Override
    public int getItemCount() {
        return listGroup.size();
    }

    class ItemGroupViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        public TextView iconGroup, txtGroupName;
        public ImageButton btnMore;

        public ItemGroupViewHolder(View itemView) {
            super(itemView);
            itemView.setOnCreateContextMenuListener(this);
            iconGroup = itemView.findViewById(R.id.icon_group);
            txtGroupName = itemView.findViewById(R.id.txtName);
            btnMore = itemView.findViewById(R.id.btnMoreAction);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            menu.setHeaderTitle((String) ((Object[]) btnMore.getTag())[0]);
            Intent data = new Intent();
            data.putExtra(GroupFragment.CONTEXT_MENU_KEY_INTENT_DATA_POS, (Integer) ((Object[]) btnMore.getTag())[1]);
            menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_EDIT, Menu.NONE, context.getString(R.string.edit_group)).setIntent(data);
            menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_DELETE, Menu.NONE, context.getString(R.string.delete_group)).setIntent(data);
            menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_LEAVE, Menu.NONE, context.getString(R.string.leave_group)).setIntent(data);
        }
    }
}
