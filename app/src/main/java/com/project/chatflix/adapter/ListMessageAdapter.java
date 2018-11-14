package com.project.chatflix.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.activity.ChatActivity;
import com.project.chatflix.object.Conversation;
import com.project.chatflix.object.Message;
import com.project.chatflix.utils.StaticConfig;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_friend, parent, false);
            return new ItemMessageFriendHolder(view);
        } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_user, parent, false);
            return new ItemMessageUserHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemMessageFriendHolder) {
            Message mess = conversation.getListMessageData().get(position);

            if (mess != null) {
                String mess_type = mess.type;
                if (mess_type.equals(context.getString(R.string.image_field))) {
                    ((ItemMessageFriendHolder) holder).viewCallFriend.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.INVISIBLE);
                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.VISIBLE);
//                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.VISIBLE);
                    Picasso.with(((ItemMessageFriendHolder) holder).avata.getContext())
                            .load(mess.text)
                            .placeholder(R.drawable.loading).resize(356, 356)
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

                } else if (mess_type.equals(context.getString(R.string.call_field))) {
                    ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).viewCallFriend.setVisibility(View.VISIBLE);
                    ((ItemMessageFriendHolder) holder).txtDurationFriend
                            .setText(conversation.getListMessageData().get(position).durationCall);
                } else {
//                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.INVISIBLE);
                    ((ItemMessageFriendHolder) holder).viewCallFriend.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).imgFriend.setVisibility(View.GONE);
                    ((ItemMessageFriendHolder) holder).txtContent.setVisibility(View.VISIBLE);
                    ((ItemMessageFriendHolder) holder).txtContent
                            .setText(conversation.getListMessageData().get(position).text);
                }
            }

            Bitmap currentAvata = bitmapAvata.
                    get(conversation.getListMessageData().get(position).idSender);
            if (currentAvata != null) {
                ((ItemMessageFriendHolder) holder).avata.setImageBitmap(currentAvata);
            } else {
                final String id = conversation.getListMessageData().get(position).idSender;
                if (bitmapAvataDB.get(id) == null) {
                    bitmapAvataDB.put(id, FirebaseDatabase.getInstance().getReference()
                            .child(context.getString(R.string.users) + "/" + id + "/" + context.getString(R.string.avatar_field)));
                    bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                String avataStr = (String) dataSnapshot.getValue();
                                if (!avataStr.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                                    byte[] decodedString = Base64.decode(avataStr, Base64.DEFAULT);
                                    ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                                } else {
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
            if (mess != null) {
                String mess_type = mess.type;
                if (mess_type.equals(context.getString(R.string.image_field))) {
                    ((ItemMessageUserHolder) holder).viewCallUser.setVisibility(View.GONE);
                    ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.INVISIBLE);
                    ((ItemMessageUserHolder) holder).imgUser.setVisibility(View.VISIBLE);
                    Picasso.with(((ItemMessageUserHolder) holder).avata.getContext())
                            .load(mess.text)
                            .placeholder(R.drawable.loading).resize(356, 356)
                            .centerInside()
                            .into(((ItemMessageUserHolder) holder).imgUser);

                } else if (mess_type.equals(context.getString(R.string.call_field))) {
                    ((ItemMessageUserHolder) holder).txtContent.setVisibility(View.GONE);
                    ((ItemMessageUserHolder) holder).imgUser.setVisibility(View.GONE);
                    ((ItemMessageUserHolder) holder).viewCallUser.setVisibility(View.VISIBLE);
                    ((ItemMessageUserHolder) holder).txtDurationUser
                            .setText(conversation.getListMessageData().get(position).durationCall);
                } else {
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
                .idSender.equals(StaticConfig.UID) ?
                ChatActivity.VIEW_TYPE_USER_MESSAGE : ChatActivity.VIEW_TYPE_FRIEND_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return conversation.getListMessageData().size();
    }

    class ItemMessageUserHolder extends RecyclerView.ViewHolder {
        public TextView txtContent, txtDurationUser;
        public CircleImageView avata;
        public ImageView imgUser;
        public View viewCallUser;

        public ItemMessageUserHolder(View itemView) {
            super(itemView);
            txtContent = itemView.findViewById(R.id.textContentUser);
            avata = itemView.findViewById(R.id.imageView2);
            imgUser = itemView.findViewById(R.id.message_image_layout_user);
            txtDurationUser = itemView.findViewById(R.id.txtDurationUser);
            viewCallUser = itemView.findViewById(R.id.viewCallUser);
        }
    }

    class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
        public TextView txtContent, txtDurationFriend;
        public CircleImageView avata;
        public ImageView imgFriend;
        public View viewCallFriend;

        public ItemMessageFriendHolder(View itemView) {
            super(itemView);
            txtContent = itemView.findViewById(R.id.textContentFriend);
            avata = itemView.findViewById(R.id.imageView3);
            imgFriend = itemView.findViewById(R.id.message_image_layout_friend);
            txtDurationFriend = itemView.findViewById(R.id.txtDurationFriend);
            viewCallFriend = itemView.findViewById(R.id.viewCallFriend);
        }
    }
}

