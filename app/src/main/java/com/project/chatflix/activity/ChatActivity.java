package com.project.chatflix.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.chatflix.R;
import com.project.chatflix.adapter.ListMessageAdapter;
import com.project.chatflix.database.SharedPreferenceHelper;
import com.project.chatflix.object.Conversation;
import com.project.chatflix.object.GetTimeAgo;
import com.project.chatflix.object.Message;
import com.project.chatflix.utils.StaticConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    public static boolean isActive = false;
    private static final int GALLERY_PICK = 1;
    Toolbar tbChat;

    public static boolean isVideo = false;

    private RecyclerView recyclerChat;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private String roomId, kindOfChat, current_user_ref, chat_user_ref, email_friend, online;
    private ArrayList<CharSequence> idFriend;
    private Conversation conversation;
    private ImageButton btnSend;
    private Button btnAddImage, btnCall, btnVideo;
    private EditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    public static HashMap<String, Bitmap> bitmapAvataFriend;
    public Bitmap bitmapAvataUser;

    private TextView mTitleView;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;

    // Storage Firebase
    private StorageReference mImageStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tbChat = (Toolbar) findViewById(R.id.toolbarChat);
        setSupportActionBar(tbChat);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow_left);

//        getWindow().setBackgroundDrawableResource(R.drawable.bg_moutain_1);

        mImageStorage = FirebaseStorage.getInstance().getReference();

        Intent intentData = getIntent();
        idFriend = intentData.getCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID);
        kindOfChat = intentData.getStringExtra(getString(R.string.kind_of_chat));
        roomId = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID);
        Log.e("Message", "Group roomId = " + roomId);

        final String nameFriend = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND);

        conversation = new Conversation();

        btnSend = (ImageButton) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> {

            String content = editWriteMessage.getText().toString().trim();

            if (content.length() > 0) {
                editWriteMessage.setText("");
                Message newMessage = new Message();
                newMessage.text = content;
                newMessage.idSender = FirebaseAuth.getInstance().getCurrentUser().getUid();
                newMessage.idReceiver = roomId;
                newMessage.type = getString(R.string.text);
                newMessage.timestamp = System.currentTimeMillis();

                if (kindOfChat.equalsIgnoreCase(getString(R.string.friend_chat))) {
                    FirebaseDatabase.getInstance().getReference().child("Message/" + roomId)
                            .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode()))
                            .push().setValue(newMessage);
                    FirebaseDatabase.getInstance().getReference().child("Message")
                            .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode())).child(roomId)
                            .push().setValue(newMessage);
                } else {
                    FirebaseDatabase.getInstance().getReference().child("Message/" + roomId)
                            .push().setValue(newMessage);
                }

            }
        });

        String base64AvataUser = SharedPreferenceHelper.getInstance(this).getUserInfo().avatar;
        if (!base64AvataUser.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            byte[] decodedString = Base64.decode(base64AvataUser, Base64.DEFAULT);
            bitmapAvataUser = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } else {
            bitmapAvataUser = null;
        }

        editWriteMessage = (EditText) findViewById(R.id.editWriteMessage);
        if (idFriend != null && nameFriend != null) {
            linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerChat = (RecyclerView) findViewById(R.id.recyclerChat);
            recyclerChat.setLayoutManager(linearLayoutManager);
            adapter = new
                    ListMessageAdapter(this, conversation, bitmapAvataFriend, bitmapAvataUser);

            if (kindOfChat.equalsIgnoreCase(getString(R.string.friend_chat))) {
                FirebaseDatabase.getInstance().getReference().child("Message/" + roomId)
                        .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode()))
                        .addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                if (dataSnapshot.getValue() != null) {
                                    HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                                    Message newMessage = new Message();
                                    newMessage.idSender = (String) mapMessage.get(getString(R.string.id_sender));
                                    newMessage.idReceiver = (String) mapMessage.get(getString(R.string.id_receiver));
                                    newMessage.text = (String) mapMessage.get(getString(R.string.text));
                                    newMessage.timestamp = (long) mapMessage.get(getString(R.string.timestamp));
                                    newMessage.durationCall = (String) mapMessage.get(getString(R.string.duration));
                                    // Lấy ảnh
                                    newMessage.type = (String) mapMessage.get(getString(R.string.type));
                                    conversation.getListMessageData().add(newMessage);

                                    adapter.notifyDataSetChanged();
                                    linearLayoutManager.scrollToPosition(conversation.getListMessageData().size() - 1);
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
                recyclerChat.setAdapter(adapter);
            } else {
                FirebaseDatabase.getInstance().getReference().child("Message/" + roomId)
                        .addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                if (dataSnapshot.getValue() != null) {
                                    HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                                    Message newMessage = new Message();
                                    newMessage.idSender = (String) mapMessage.get(getString(R.string.id_sender));
                                    newMessage.idReceiver = (String) mapMessage.get(getString(R.string.id_receiver));
                                    newMessage.text = (String) mapMessage.get(getString(R.string.text));
                                    newMessage.timestamp = (long) mapMessage.get(getString(R.string.timestamp));
                                    newMessage.durationCall = (String) mapMessage.get(getString(R.string.duration));
                                    // Lấy ảnh
                                    newMessage.type = (String) mapMessage.get(getString(R.string.type));

                                    conversation.getListMessageData().add(newMessage);
                                    adapter.notifyDataSetChanged();
                                    linearLayoutManager.scrollToPosition(conversation.getListMessageData().size() - 1);
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
                recyclerChat.setAdapter(adapter);

            }
        }

        //setActionBar bằng một layout
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

        getSupportActionBar().setCustomView(action_bar_view);

        // ---- Custom Action bar Items ----
        mTitleView = (TextView) findViewById(R.id.custom_bar_title);
        mLastSeenView = (TextView) findViewById(R.id.custom_bar_seen);
        mProfileImage = (CircleImageView) findViewById(R.id.custom_bar_image);
        btnCall = (Button) findViewById(R.id.btnCall);
        btnVideo = (Button) findViewById(R.id.btnVideo);

        FirebaseDatabase.getInstance().getReference().child(getString(R.string.users))
                .child(String.valueOf(idFriend.get(0)))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        email_friend = dataSnapshot.child(getString(R.string.email)).getValue().toString();
                        online = dataSnapshot.child(getString(R.string.online)).getValue().toString();
                        if (online.equals(getString(R.string.true_field))) {

                            mLastSeenView.setText(getString(R.string.online_status));

                        } else {
                            GetTimeAgo getTimeAgo = new GetTimeAgo();
                            long lastTime = Long.parseLong(online);
                            String lastSeenTime = getTimeAgo.getTimeAgo(lastTime, getApplicationContext());

                            mLastSeenView.setText(lastSeenTime);

                        }

                        mTitleView.setText(nameFriend);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        // Send Image
        btnAddImage = (Button) findViewById(R.id.btnAddImage);
        btnAddImage.setOnClickListener(v -> {
            Intent galleryIntent = new Intent();
            galleryIntent.setType("image/*");
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"
            ), GALLERY_PICK);
        });

        //Make a phone call
//        btnCall.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (online.equals("true")){
//                    try {
//                        Call call = getSinchServiceInterface().callUser(email_friend);
//                        String callId = call.getCallId();
//                        Intent callScreen = new Intent(ChatActivity.this, CallScreenActivity.class);
//                        callScreen.putExtra(SinchService.CALL_ID, callId);
//                        callScreen.putExtra("Room",roomId);
//                        startActivity(callScreen);
//
//                    } catch (MissingPermissionException e) {
//
//                        ActivityCompat.requestPermissions(ChatActivity.this,
//                                new String[]{e.getRequiredPermission()}, 0);
//
//                    }
//                }else {
//
//                    new LovelyInfoDialog(ChatActivity.this)
//                            .setTopColorRes(R.color.colorPrimary)
//                            .setIcon(R.drawable.no_call)
//                            .setTitle("Message")
//                            .setMessage("Can't make a call! Your friend is not online")
//                            .show();
//
//                }
//
//            }
//        });

        //Make a video call
//        btnVideo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                isVideo = true;
//
//                if (online.equals("true")){
//                    try {
//                        Call call = getSinchServiceInterface().callUserVideo(email_friend);
//                        String callId = call.getCallId();
//
//                        Intent callScreen = new Intent(ChatActivity.this, VideoSreenActivity.class);
//                        callScreen.putExtra("Room",roomId);
//                        callScreen.putExtra(SinchService.CALL_ID, callId);
//                        startActivity(callScreen);
//
//                    } catch (MissingPermissionException e) {
//
//                        ActivityCompat.requestPermissions(ChatActivity.this,
//                                new String[]{e.getRequiredPermission()}, 0);
//
//                    }
//                }else {
//
//                    new LovelyInfoDialog(ChatActivity.this)
//                            .setTopColorRes(R.color.colorPrimary)
//                            .setIcon(R.drawable.no_call)
//                            .setTitle("Message")
//                            .setMessage("Can't make a call! Your friend is not online")
//                            .show();
//
//                }
//            }
//        });

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.notify_sinch), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.permission_sinch), Toast
                    .LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActive = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActive = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {

                Uri imageUri = data.getData();
                if (kindOfChat.equalsIgnoreCase(getString(R.string.friend_chat))) {

                    current_user_ref = "Message/" + roomId + "/" +
                            String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode());

                    chat_user_ref = "Message/" +
                            String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode())
                            + "/" + roomId;
                } else {
                    current_user_ref = "Message/" + roomId;
                    chat_user_ref = "Message/" + roomId;
                }


                DatabaseReference user_message_push = FirebaseDatabase.getInstance().getReference()
                        .child("Message")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(roomId)
                        .push();

                final String push_id = user_message_push.getKey();


                StorageReference filepath = mImageStorage.child(getString(R.string.message_images)).child(push_id + ".jpg");

                filepath.putFile(imageUri).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String download_url = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put(getString(R.string.text), download_url);
                        messageMap.put(getString(R.string.id_sender), FirebaseAuth.getInstance().getCurrentUser().getUid());
                        messageMap.put(getString(R.string.id_receiver), roomId);
                        messageMap.put(getString(R.string.type), getString(R.string.image_field));
                        messageMap.put(getString(R.string.timestamp), ServerValue.TIMESTAMP);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                        messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

                        editWriteMessage.setText("");

                        FirebaseDatabase.getInstance().getReference()
                                .updateChildren(messageUserMap, (databaseError, databaseReference) -> {
                                    if (databaseError != null) {
                                        Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                    }
                                });
                    }
                });
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.toString());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent result = new Intent();
            result.putExtra(getString(R.string.id_friend), idFriend.get(0));
            setResult(RESULT_OK, result);
            this.finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra(getString(R.string.id_friend), idFriend.get(0));
        setResult(RESULT_OK, result);
        this.finish();
    }

}
