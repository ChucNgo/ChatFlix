package com.project.chatflix.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.project.chatflix.MainActivity;
import com.project.chatflix.R;
import com.project.chatflix.activity.ChatActivity;
import com.project.chatflix.database.FriendDB;
import com.project.chatflix.database.GroupDB;
import com.project.chatflix.object.Friend;
import com.project.chatflix.object.Group;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.utils.StaticConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FriendChatService extends Service {
    private static String TAG = "FriendChatService";
    // Binder given to clients
    public final IBinder mBinder = new LocalBinder();
    public Map<String, Boolean> mapMark;
    public Map<String, Query> mapQuery;
    public Map<String, ChildEventListener> mapChildEventListenerMap;
    public Map<String, Bitmap> mapBitmap;
    public ArrayList<String> listKey;
    public ListFriend listFriend;
    public ArrayList<Group> listGroup;

    public FriendChatService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mapMark = new HashMap<>();
        mapQuery = new HashMap<>();
        mapChildEventListenerMap = new HashMap<>();
        listFriend = FriendDB.getInstance(this).getListFriend();
        listGroup = GroupDB.getInstance(this).getListGroups();
        listKey = new ArrayList<>();
        mapBitmap = new HashMap<>();

        if (listFriend.getListFriend().size() > 0 || listGroup.size() > 0) {

            // Nhận thông báo Friend
            for (final Friend friend : listFriend.getListFriend()) {
                if (!listKey.contains(friend.idRoom)) {
                    mapQuery.put(friend.idRoom, FirebaseDatabase.getInstance().getReference()
                            .child("Message/" + friend.idRoom)
                            .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode())).limitToLast(1));

                    mapChildEventListenerMap.put(friend.idRoom, new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            if (mapMark.get(friend.idRoom) != null && mapMark.get(friend.idRoom)) {

                                if (mapBitmap.get(friend.idRoom) == null) {
                                    if (!friend.avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                        byte[] decodedString = Base64.decode(friend.avatar, Base64.DEFAULT);
                                        mapBitmap.put(friend.idRoom, BitmapFactory
                                                .decodeByteArray(decodedString,
                                                0, decodedString.length));
                                    } else {
                                        mapBitmap.put(friend.idRoom, BitmapFactory.decodeResource
                                                (getResources(), R.drawable.default_avatar));
                                    }
                                }

                                if (ChatActivity.isActive == false) {
                                    createNotify(friend.name, (String) ((HashMap) dataSnapshot.getValue())
                                                    .get(getString(R.string.text)),
                                            friend.idRoom.hashCode(), mapBitmap.get(friend.idRoom),
                                            false);
                                }
//                                else if (CallScreenActivity.isCalling){
//
//                                    stopNotify(friend.idRoom);
//
//                                }


                            } else {
                                mapMark.put(friend.idRoom, true);
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
                    listKey.add(friend.idRoom);
                }
                mapQuery.get(friend.idRoom).addChildEventListener(
                        mapChildEventListenerMap.get(friend.idRoom));
            }

            // Nhận thông báo nhóm
            for (final Group group : listGroup) {
                if (!listKey.contains(group.id)) {
                    mapQuery.put(group.id, FirebaseDatabase.getInstance().getReference()
                            .child("Message/" + group.id).limitToLast(1));

                    mapChildEventListenerMap.put(group.id, new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                            if (mapMark.get(group.id) != null && mapMark.get(group.id)) {

                                if (mapBitmap.get(group.id) == null) {
                                    mapBitmap.put(group.id, BitmapFactory.decodeResource(getResources(),
                                            R.drawable.ic_notify_group));
                                }
                                if (ChatActivity.isActive == false){
                                    createNotify(group.groupInfo.get(getString(R.string.name_field)),
                                            (String) ((HashMap) dataSnapshot.getValue())
                                                    .get(getString(R.string.text)), group.id.hashCode(), mapBitmap.get(group.id) ,
                                            true);
                                }

                            } else {
                                mapMark.put(group.id, true);
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
                    listKey.add(group.id);
                }
                mapQuery.get(group.id).addChildEventListener(mapChildEventListenerMap.get(group.id));
            }

        } else {
            stopSelf();
        }
    }

    public void stopNotify(String id) {
        mapMark.put(id, false);
    }

    public void createNotify(String name, String content, int id, Bitmap icon, boolean isGroup) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent,
                PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder notificationBuilder = new
                NotificationCompat.Builder(this)
                .setLargeIcon(icon)
                .setContentTitle(name)
                .setContentText(content)
                .setContentIntent(pendingIntent)
//                .setVibrate(new long[] { 1000, 1000})
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setAutoCancel(true);
        if (isGroup) {
            notificationBuilder.setSmallIcon(R.drawable.ic_tab_group);
        } else {
            notificationBuilder.setSmallIcon(R.drawable.ic_tab_person);
        }
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
        notificationManager.notify(id,
                notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OnStartService");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "OnBindService");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (String id : listKey) {
            mapQuery.get(id).removeEventListener(mapChildEventListenerMap.get(id));
        }
        mapQuery.clear();
        mapChildEventListenerMap.clear();
        mapBitmap.clear();

        Log.d(TAG, "OnDestroyService");
    }

    public class LocalBinder extends Binder {
        public FriendChatService getService() {
            // Return this instance of LocalService so clients can call public methods
            return FriendChatService.this;
        }
    }
}
