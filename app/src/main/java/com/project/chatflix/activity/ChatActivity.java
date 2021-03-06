package com.project.chatflix.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aditya.filebrowser.Constants;
import com.aditya.filebrowser.FileChooser;
import com.crashlytics.android.Crashlytics;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.project.chatflix.R;
import com.project.chatflix.activity.Sinch_Calling.BaseActivity;
import com.project.chatflix.activity.Sinch_Calling.CallScreenActivity;
import com.project.chatflix.adapter.ListMessageAdapter;
import com.project.chatflix.database.SharedPreferenceHelper;
import com.project.chatflix.object.Conversation;
import com.project.chatflix.object.GetTimeAgo;
import com.project.chatflix.object.Message;
import com.project.chatflix.service.SinchService;
import com.project.chatflix.utils.StaticConfig;
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.calling.Call;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends BaseActivity {
    public static boolean isActive = false;
    Toolbar tbChat;

    private RecyclerView recyclerChat;
    private static final int PICK_PDF = 444;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private String roomId, kindOfChat, current_user_ref, email_friend, online;
    private String idFriend;
    private Conversation conversation;
    private ImageButton btnSend;
    private Button btnAddImage, btnCall, btnFile;
    private EditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    public static HashMap<String, Bitmap> bitmapAvataFriend;
    public static Bitmap bitmapAvataUser;

    private TextView mTitleView;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;

    private StorageReference mImageStorage;
    private UploadTask uploadTask;
    private DatabaseReference mDatabaseRef;
    private ProgressBar progressBar;
    private RelativeLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initView();
        addEvents();
    }

    private void initView() {
        try {
            layout = findViewById(R.id.wrapper_layout);
            progressBar = findViewById(R.id.progress_bar);
            mDatabaseRef = FirebaseDatabase.getInstance().getReference();
            tbChat = findViewById(R.id.toolbarChat);
            setSupportActionBar(tbChat);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow_left);
            mImageStorage = FirebaseStorage.getInstance().getReference();
            Bundle intentData = getIntent().getExtras();
            idFriend = intentData.getString(StaticConfig.INTENT_KEY_CHAT_ID);
            kindOfChat = intentData.getString(getString(R.string.kind_of_chat));
            roomId = intentData.getString(StaticConfig.INTENT_KEY_CHAT_ROOM_ID);

            String nameFriend = intentData.getString(StaticConfig.INTENT_KEY_CHAT_FRIEND);

            conversation = new Conversation();
            btnSend = findViewById(R.id.btnSend);
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

            getSupportActionBar().setCustomView(action_bar_view);

            mTitleView = findViewById(R.id.custom_bar_title);
            mTitleView.setText(nameFriend);
            mLastSeenView = findViewById(R.id.custom_bar_seen);
            mProfileImage = findViewById(R.id.custom_bar_image);
            btnCall = findViewById(R.id.btnCall);
            btnFile = findViewById(R.id.btnAddFile);
            btnAddImage = findViewById(R.id.btnAddImage);

            editWriteMessage = findViewById(R.id.editWriteMessage);
            linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerChat = findViewById(R.id.recyclerChat);
            recyclerChat.setLayoutManager(linearLayoutManager);
            adapter = new
                    ListMessageAdapter(this, conversation, bitmapAvataFriend);
            recyclerChat.setAdapter(adapter);

            String base64AvataUser = SharedPreferenceHelper.getInstance(this).getUserInfo().avatar;
            if (!base64AvataUser.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                byte[] decodedString = Base64.decode(base64AvataUser, Base64.DEFAULT);
                bitmapAvataUser = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            } else {
                bitmapAvataUser = null;
            }

            getListMessage();

            if (idFriend != null) {
                showLastSeenTime(idFriend);
            }

            mDatabaseRef.child(getString(R.string.online_chat_table)).child(StaticConfig.UID).setValue(roomId);
            mDatabaseRef.child(getString(R.string.online_chat_table)).keepSynced(false);

            NotificationManagerCompat.from(this).cancel(getString(R.string.app_name), 001);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    private void showLastSeenTime(String idFriend) {
        mDatabaseRef.child(getString(R.string.users))
                .child(String.valueOf(idFriend))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
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
                        } catch (Exception e) {
                            Log.e(getClass().getName(), e.toString());
                            Crashlytics.logException(e);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void getListMessage() {

        if (kindOfChat.equalsIgnoreCase(getString(R.string.group_chat))) {
            btnCall.setVisibility(View.GONE);
        }

        mDatabaseRef.child(getString(R.string.message_table)).child(roomId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        try {
                            if (dataSnapshot.getValue() != null) {
                                HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                                Message newMessage = new Message();
                                newMessage.idSender = (String) mapMessage.get(getString(R.string.id_sender));
                                newMessage.idReceiver = (String) mapMessage.get(getString(R.string.id_receiver));
                                newMessage.text = (String) mapMessage.get(getString(R.string.text));
                                newMessage.timestamp = (Long) mapMessage.get(getString(R.string.timestamp));
                                newMessage.durationCall = (String) mapMessage.get(getString(R.string.duration));
                                newMessage.type = (String) mapMessage.get(getString(R.string.type));

                                newMessage.link = mapMessage.get(getString(R.string.link)) != null
                                        ? (String) mapMessage.get(getString(R.string.link))
                                        : null;

                                conversation.getListMessageData().add(newMessage);
                                adapter.notifyDataSetChanged();
                                linearLayoutManager.scrollToPosition(conversation.getListMessageData().size() - 1);
                            }
                        } catch (Exception e) {
                            Log.e(getClass().getName(), e.toString());
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

    private void addEvents() {
        btnSend.setOnClickListener(v -> {
            try {
                String content = editWriteMessage.getText().toString().trim();

                if (content.length() > 0) {
                    editWriteMessage.setText("");
                    Message newMessage = new Message();
                    newMessage.text = content;
                    newMessage.idSender = StaticConfig.UID;
                    newMessage.idReceiver = roomId;
                    newMessage.type = getString(R.string.text);
                    newMessage.timestamp = System.currentTimeMillis();

                    if (kindOfChat.equalsIgnoreCase(getString(R.string.friend_chat))) {
                        mDatabaseRef.child(getString(R.string.message_table))
                                .child(roomId)
                                .push().setValue(newMessage);
                    } else {
                        mDatabaseRef.child(getString(R.string.message_table) + "/" + roomId)
                                .push().setValue(newMessage);
                    }
                }
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
                Crashlytics.logException(e);
            }
        });

        btnAddImage.setOnClickListener(v -> {
            requestPermission();
        });

        //Make a phone call
        btnCall.setOnClickListener(v -> {
            try {
                if (online.equals(getString(R.string.true_field))) {
                    try {
                        Call call = getSinchServiceInterface().callUser(email_friend);
                        String callId = call.getCallId();
                        Intent callScreen = new Intent(ChatActivity.this, CallScreenActivity.class);
                        callScreen.putExtra(SinchService.CALL_ID, callId);
                        callScreen.putExtra(getString(R.string.room), roomId);
                        startActivity(callScreen);

                    } catch (MissingPermissionException e) {
                        ActivityCompat.requestPermissions(ChatActivity.this,
                                new String[]{e.getRequiredPermission()}, 0);
                    }
                } else {

                    new LovelyInfoDialog(ChatActivity.this)
                            .setTopColorRes(R.color.colorPrimary)
                            .setIcon(R.drawable.no_call)
                            .setTitle(getString(R.string.message_table))
                            .setMessage(getString(R.string.cannot_make_call))
                            .show();

                }
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
                Crashlytics.logException(e);
            }
        });

        btnFile.setOnClickListener(v -> {
            Intent i = new Intent(this, FileChooser.class); //works for all 3 main classes (i.e FileBrowser, FileChooser, FileBrowserWithCustomHandler)
            i.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "pdf;txt;doc;docx;xls;xlsx;zip;rar");
            startActivityForResult(i, PICK_PDF);
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.permission_sinch), Toast
                    .LENGTH_LONG).show();
        }
    }

    public void requestPermission() {

        try {
            int checkCameraPer = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
            int checkWiteExPer = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int checkReadExPer = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            int permissionGranted = PackageManager.PERMISSION_GRANTED;

            String[] permissions = new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE};
            //check Android 6+
            if (Build.VERSION.SDK_INT >= 23) {
                //check permission granted
                //check permission granted
                if (checkCameraPer != permissionGranted
                        || checkWiteExPer != permissionGranted
                        || checkReadExPer != permissionGranted
                        ) {
                    //request Permissions
                    ActivityCompat.requestPermissions(this, permissions, StaticConfig.PERMISSION_CONSTANT);

                } else {
                    sendToGallery();
                }
            } else {
                sendToGallery();
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    private void sendToGallery() {
        ImagePicker.create(this)
                .folderMode(true) // folder mode (false by default)
                .toolbarFolderTitle(getString(R.string.folder)) // folder selection title
                .toolbarImageTitle(getString(R.string.tap_to_select)) // image selection title
                .toolbarArrowColor(Color.WHITE) // Toolbar 'up' arrow color
                .limit(1) // max images can be selected (99 by default)
                .showCamera(true) // show camera or not (true by default)
                .imageDirectory(getString(R.string.chat_flix)) // directory name for captured image  ("Camera" folder by default)
                .theme(R.style.ImagePickerTheme) // must inherit ef_BaseTheme. please refer to sample
                .enableLog(false) // disabling log
                .start(); // start image picker activity with request code
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
        mDatabaseRef.child(getString(R.string.online_chat_table)).child(StaticConfig.UID).removeValue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
                showProgress(true);
                List<Image> images = ImagePicker.getImages(data);
                Uri imageUri = Uri.fromFile(new File(images.get(0).getPath()));

                current_user_ref = getString(R.string.message_table) + "/" + roomId;

                DatabaseReference user_message_push = mDatabaseRef
                        .child(getString(R.string.message_table))
                        .child(roomId)
                        .push();

                final String push_id = user_message_push.getKey();
                StorageReference filepath = mImageStorage.child(getString(R.string.message_images)).child(push_id + ".jpg");

                uploadTask = filepath.putFile(imageUri);

                uploadTask.addOnCompleteListener(task -> {

                    try {
                        if (task.isSuccessful()) {
                            String download_url = String.valueOf(task.getResult().getDownloadUrl());

                            Map messageMap = new HashMap();
                            messageMap.put(getString(R.string.text), download_url);
                            messageMap.put(getString(R.string.id_sender), StaticConfig.UID);
                            messageMap.put(getString(R.string.id_receiver), roomId);
                            messageMap.put(getString(R.string.type), getString(R.string.image_field));
                            messageMap.put(getString(R.string.timestamp), ServerValue.TIMESTAMP);

                            Map messageUserMap = new HashMap();
                            messageUserMap.put(current_user_ref + "/" + push_id, messageMap);

                            editWriteMessage.setText("");

                            mDatabaseRef.updateChildren(messageUserMap, (databaseError, databaseReference) -> {
                                if (databaseError != null) {
                                    Log.e(getClass().getSimpleName(), databaseError.getMessage());
                                }
                            });
                            showProgress(false);
                        }
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), e.toString());
                        Crashlytics.logException(e);
                        showProgress(false);
                    }
                })
                        .addOnFailureListener(e -> {
                            String s = "";
                            showProgress(false);
                        });
            } else {
                if (requestCode == PICK_PDF && resultCode == Activity.RESULT_OK) {
                    showProgress(true);
                    handleFileAndUpload(data);
                } else {
                    Toast.makeText(this, "Cannot upload file to server", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    private void handleFileAndUpload(Intent data) {
        // Get the Uri of the selected file
        Uri uri = data.getData();
        String uriString = uri.toString();
        File myFile = new File(uriString);
        String path = myFile.getAbsolutePath();

        StorageReference filepath = mImageStorage.child(getString(R.string.message_files)).child(StaticConfig.UID);

        uploadTask = filepath.putFile(uri);

        uploadTask.addOnCompleteListener(task -> {

            try {
                if (task.isSuccessful()) {
                    String download_url = String.valueOf(task.getResult().getDownloadUrl());
                    String displayName = null;

                    if (uriString.startsWith("content://")) {
                        Cursor cursor = null;
                        try {
                            cursor = this.getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            }
                        } finally {
                            cursor.close();
                        }
                    } else if (uriString.startsWith("file://")) {
                        displayName = myFile.getName();
                    }

                    current_user_ref = getString(R.string.message_table) + "/" + roomId;

                    DatabaseReference user_message_push = mDatabaseRef
                            .child(getString(R.string.message_table))
                            .child(StaticConfig.UID)
                            .child(roomId)
                            .push();

                    final String push_id = user_message_push.getKey();

                    Map messageMap = new HashMap();
                    messageMap.put(getString(R.string.text), displayName);
                    messageMap.put(getString(R.string.link), download_url);
                    messageMap.put(getString(R.string.id_sender), StaticConfig.UID);
                    messageMap.put(getString(R.string.id_receiver), roomId);
                    messageMap.put(getString(R.string.type), getString(R.string.file_field));
                    messageMap.put(getString(R.string.timestamp), ServerValue.TIMESTAMP);

                    Map messageUserMap = new HashMap();
                    messageUserMap.put(current_user_ref + "/" + push_id, messageMap);

                    mDatabaseRef.updateChildren(messageUserMap, (databaseError, databaseReference) -> {
                        if (databaseError != null) {
                            Log.e(getClass().getSimpleName(), databaseError.getMessage());
                        }
                    });
                    showProgress(false);
                }
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), e.toString());
                Crashlytics.logException(e);
                showProgress(false);
            }
        })
                .addOnFailureListener(e -> {
                    String s = "";
                    showProgress(false);
                });
    }

    private void showProgress(boolean kq) {
        if (kq == true) {
            progressBar.setVisibility(View.VISIBLE);
            layout.setAlpha((float) 0.5);
        } else {
            progressBar.setVisibility(View.GONE);
            layout.setAlpha(1);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
