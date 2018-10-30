package com.project.chatflix.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.storage.UploadTask;
import com.project.chatflix.R;
import com.project.chatflix.database.SharedPreferenceHelper;
import com.project.chatflix.object.Conversation;
import com.project.chatflix.object.GetTimeAgo;
import com.project.chatflix.object.Message;
import com.project.chatflix.utils.StaticConfig;
import com.squareup.picasso.Picasso;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

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
    private String roomId,kindOfChat,current_user_ref,chat_user_ref,email_friend,online;
    private ArrayList<CharSequence> idFriend;
    private Conversation conversation;
    private ImageButton btnSend;
    private Button btnAddImage,btnCall,btnVideo;
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
        kindOfChat = intentData.getStringExtra("Kind Of Chat");
        roomId = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID);
        Log.e("message","Group roomId = " + roomId);

        final String nameFriend = intentData.getStringExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND);

        conversation = new Conversation();

        btnSend = (ImageButton) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String content = editWriteMessage.getText().toString().trim();

                if (content.length() > 0) {
                    editWriteMessage.setText("");
                    Message newMessage = new Message();
                    newMessage.text = content;
                    newMessage.idSender = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    newMessage.idReceiver = roomId;
                    newMessage.type = "text";
                    newMessage.timestamp = System.currentTimeMillis();

                    if (kindOfChat.equalsIgnoreCase("FriendChat")){
                        FirebaseDatabase.getInstance().getReference().child("message/" + roomId)
                                .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode()))
                                .push().setValue(newMessage);
                        FirebaseDatabase.getInstance().getReference().child("message")
                                .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode())).child(roomId)
                                .push().setValue(newMessage);
                    }else {
                        FirebaseDatabase.getInstance().getReference().child("message/" + roomId)
                                .push().setValue(newMessage);
                    }

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

            if (kindOfChat.equalsIgnoreCase("FriendChat")){
                FirebaseDatabase.getInstance().getReference().child("message/" + roomId)
                        .child(String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode()))
                        .addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                if (dataSnapshot.getValue() != null) {
                                    HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                                    Message newMessage = new Message();
                                    newMessage.idSender = (String) mapMessage.get("idSender");
                                    newMessage.idReceiver = (String) mapMessage.get("idReceiver");
                                    newMessage.text = (String) mapMessage.get("text");
                                    newMessage.timestamp = (long) mapMessage.get("timestamp");
                                    newMessage.durationCall = (String) mapMessage.get("duration");
                                    // Lấy ảnh
                                    newMessage.type = (String) mapMessage.get("type");
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
            }else{
                FirebaseDatabase.getInstance().getReference().child("message/" + roomId)
                        .addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                if (dataSnapshot.getValue() != null) {
                                    HashMap mapMessage = (HashMap) dataSnapshot.getValue();
                                    Message newMessage = new Message();
                                    newMessage.idSender = (String) mapMessage.get("idSender");
                                    newMessage.idReceiver = (String) mapMessage.get("idReceiver");
                                    newMessage.text = (String) mapMessage.get("text");
                                    newMessage.timestamp = (long) mapMessage.get("timestamp");
                                    newMessage.durationCall = (String) mapMessage.get("duration");
                                    // Lấy ảnh
                                    newMessage.type = (String) mapMessage.get("type");

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

        FirebaseDatabase.getInstance().getReference().child("Users")
                .child(String.valueOf(idFriend.get(0)))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        email_friend = dataSnapshot.child("email").getValue().toString();
//                        Toast.makeText(ChatActivity.this,email_friend,Toast.LENGTH_SHORT).show();
                        online = dataSnapshot.child("online").getValue().toString();
                        if(online.equals("true")){

                            mLastSeenView.setText("Online");

                        }else {
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
        btnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent,"SELECT IMAGE"
                ), GALLERY_PICK);

            }
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
            Toast.makeText(this, "You may now place a call", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "This application needs permission to use your microphone to function properly.", Toast
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

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK){

            Uri imageUri = data.getData();
            if (kindOfChat.equalsIgnoreCase("FriendChat")) {

                current_user_ref = "message/" + roomId + "/" +
                        String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode());

                chat_user_ref = "message/" +
                        String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode())
                        + "/" + roomId;
            }else {
                current_user_ref = "message/" + roomId;
                chat_user_ref = "message/" + roomId;
            }


            DatabaseReference user_message_push = FirebaseDatabase.getInstance().getReference()
                    .child("message")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child(roomId)
                    .push();

            final String push_id = user_message_push.getKey();


            StorageReference filepath = mImageStorage.child("message_images").child( push_id + ".jpg");

            filepath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if(task.isSuccessful()){

                        String download_url = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("text", download_url);
                        messageMap.put("idSender", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        messageMap.put("idReceiver", roomId);
                        messageMap.put("type", "image");
                        messageMap.put("timestamp", ServerValue.TIMESTAMP);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                        messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

                        editWriteMessage.setText("");

                        FirebaseDatabase.getInstance().getReference()
                                .updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                if(databaseError != null) {

                                    Log.d("CHAT_LOG", databaseError.getMessage().toString());

                                }
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            Intent result = new Intent();
            result.putExtra("idFriend", idFriend.get(0));
            setResult(RESULT_OK, result);
            this.finish();
        }
        return true;

//        switch (item.getItemId()) {
//            case R.id.item1:
//                getWindow().setBackgroundDrawableResource(R.drawable.bg_moutain);
//                return true;
//            case R.id.item2:
//                getWindow().setBackgroundDrawableResource(R.drawable.rain);
//                return true;
//            case R.id.item3:
//                getWindow().setBackgroundDrawableResource(R.drawable.rail);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }

    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("idFriend", idFriend.get(0));
        setResult(RESULT_OK, result);
        this.finish();
    }

}

class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private Conversation conversation;
    private HashMap<String, Bitmap> bitmapAvata;
    private HashMap<String, DatabaseReference> bitmapAvataDB;
    private Bitmap bitmapAvataUser;

    public ListMessageAdapter(Context context, Conversation conversation, HashMap<String, Bitmap> bitmapAvata, Bitmap bitmapAvataUser) {
        this.context = context;
        this.conversation = conversation;
        this.bitmapAvata = bitmapAvata;
        this.bitmapAvataUser = bitmapAvataUser;
        bitmapAvataDB = new HashMap<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ChatActivity.VIEW_TYPE_FRIEND_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend, parent, false);
            return new ItemMessageFriendHolder(view);
        } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false);
            return new ItemMessageUserHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemMessageFriendHolder) {
            // Hiển thị ảnh người khác gửi
            Message mess = conversation.getListMessageData().get(position);

            String mess_type = mess.type;

            // Friend gửi ảnh
            if (mess != null){
                if(mess_type.equals("image")) {
                    ((ItemMessageFriendHolder) holder).viewCallFriend.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.INVISIBLE);
                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.VISIBLE);
//                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.VISIBLE);
                    Picasso.with(((ItemMessageFriendHolder) holder).avata.getContext())
                            .load(mess.text)
                            .placeholder(R.drawable.loading).resize(356,356)
                            .centerInside()
                            .into(((ItemMessageFriendHolder) holder).imgFriend);

//                    InputStream inputStream = context.getContentResolver().openInputStream(data.getData());
//
//                    Bitmap imgBitmap = BitmapFactory.decodeStream(inputStream);
//                    imgBitmap = ImageUtils.cropToSquare(imgBitmap);
//                    InputStream is = ImageUtils.convertBitmapToInputStream(imgBitmap);
//                    final Bitmap liteImage = ImageUtils.makeImageLite(is,
//                            imgBitmap.getWidth(), imgBitmap.getHeight(),
//                            ImageUtils.AVATAR_WIDTH, ImageUtils.AVATAR_HEIGHT);
//
//                    String imageBase64 = ImageUtils.encodeBase64(liteImage);

                }else if (mess_type.equals("call")){

                    ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).viewCallFriend.setVisibility(View.VISIBLE);
                    ((ItemMessageFriendHolder) holder).txtDurationFriend
                            .setText(conversation.getListMessageData().get(position).durationCall);


                }
                else {
//                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.INVISIBLE);
                    ((ItemMessageFriendHolder) holder).viewCallFriend.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.VISIBLE);
                    // Hiển thị message lên TextView
                    ((ItemMessageFriendHolder) holder).txtContent
                            .setText(conversation.getListMessageData().get(position).text);
                }
            }

            // Hiển thị avatar cạnh TextView
            Bitmap currentAvata = bitmapAvata.
                    get(conversation.getListMessageData().get(position).idSender);
            if (currentAvata != null) {
                ((ItemMessageFriendHolder) holder).avata.setImageBitmap(currentAvata);
            } else {
                final String id = conversation.getListMessageData().get(position).idSender;
                if(bitmapAvataDB.get(id) == null){
                    bitmapAvataDB.put(id, FirebaseDatabase.getInstance().getReference()
                            .child("Users/" + id + "/avatar"));
                    bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                String avataStr = (String) dataSnapshot.getValue();
                                // Check String avatar bằng "" hay value
                                if(!avataStr.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                    byte[] decodedString = Base64.decode(avataStr, Base64.DEFAULT);
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                                }else{
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avatar));
                                }
                                notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
        } else if (holder instanceof ItemMessageUserHolder) {


            Message mess = conversation.getListMessageData().get(position);

            String mess_type = mess.type;


            // User gửi ảnh
            if (mess != null){
                if(mess_type.equals("image")) {
                    ((ItemMessageUserHolder) holder).viewCallUser.setVisibility(View.GONE);
                    ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.INVISIBLE);
                    ((ItemMessageUserHolder) holder).imgUser.setVisibility(View.VISIBLE);
                    Picasso.with(((ItemMessageUserHolder) holder).avata.getContext())
                            .load(mess.text)
                            .placeholder(R.drawable.loading).resize(356,356)
                            .centerInside()
                            .into(((ItemMessageUserHolder) holder).imgUser);


                }else if (mess_type.equals("call")){

                    ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.GONE);
                    ((ItemMessageUserHolder) holder).imgUser.setVisibility(View.GONE);
                    ((ItemMessageUserHolder) holder).viewCallUser.setVisibility(View.VISIBLE);
                    ((ItemMessageUserHolder) holder).txtDurationUser
                            .setText(conversation.getListMessageData().get(position).durationCall);


                }
                else {
                    ((ItemMessageUserHolder) holder).viewCallUser.setVisibility(View.GONE);
                    ((ItemMessageUserHolder) holder).imgUser.setVisibility(View.GONE);
                    ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.VISIBLE);
                    ((ItemMessageUserHolder) holder).txtContent
                            .setText(mess.text);
//                    Picasso.with(((ItemMessageUserHolder) holder).avata.getContext())
//                            .load(mess.text)
//                            .placeholder(R.drawable.loading).resize(1,1)
//                            .centerInside()
//                            .into(((ItemMessageUserHolder) holder).imgUser);

                }
            }


            if (bitmapAvataUser != null) {
                ((ItemMessageUserHolder) holder).avata.setImageBitmap(bitmapAvataUser);
            }
        }

    }

    @Override
    public int getItemViewType(int position) {
        return conversation.getListMessageData().get(position)
                .idSender.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) ?
                ChatActivity.VIEW_TYPE_USER_MESSAGE : ChatActivity.VIEW_TYPE_FRIEND_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return conversation.getListMessageData().size();
    }
}

class ItemMessageUserHolder extends RecyclerView.ViewHolder {
    public TextView txtContent,txtDurationUser;
    public CircleImageView avata;
    public ImageView imgUser;
    public View viewCallUser;

    public ItemMessageUserHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentUser);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView2);
        imgUser = (ImageView) itemView.findViewById(R.id.message_image_layout_user);
        txtDurationUser = (TextView) itemView.findViewById(R.id.txtDurationUser);
        viewCallUser = itemView.findViewById(R.id.viewCallUser);
    }
}

class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
    public TextView txtContent,txtDurationFriend;
    public CircleImageView avata;
    public ImageView imgFriend;
    public View viewCallFriend;

    public ItemMessageFriendHolder(View itemView) {
        super(itemView);
        txtContent = (TextView) itemView.findViewById(R.id.textContentFriend);
        avata = (CircleImageView) itemView.findViewById(R.id.imageView3);
        imgFriend = (ImageView) itemView.findViewById(R.id.message_image_layout_friend);
        txtDurationFriend = (TextView) itemView.findViewById(R.id.txtDurationFriend);
        viewCallFriend = itemView.findViewById(R.id.viewCallFriend);
    }
}
