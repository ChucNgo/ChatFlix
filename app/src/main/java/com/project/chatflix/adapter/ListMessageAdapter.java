package com.project.chatflix.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
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
    private DatabaseReference mDatabaseRef;

    public ListMessageAdapter(Context context, Conversation conversation, HashMap<String, Bitmap> bitmapAvata) {
        this.context = context;
        this.conversation = conversation;
        this.bitmapAvata = bitmapAvata;
        bitmapAvataDB = new HashMap<>();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
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
    public void onBindViewHolder(RecyclerView.ViewHolder holder1, int position) {
        try {
            if (holder1 instanceof ItemMessageFriendHolder) {
                Message mess = conversation.getListMessageData().get(position);
                ItemMessageFriendHolder holder = (ItemMessageFriendHolder) holder1;

                if (mess != null) {
                    String mess_type = mess.type;
                    if (mess_type.equals(context.getString(R.string.image_field))) {
                        holder.viewCallFriend.setVisibility(View.GONE);
                        holder.txtContent.setVisibility(View.GONE);
                        holder.imgFriend.setVisibility(View.VISIBLE);
                        holder.viewFileFriend.setVisibility(View.GONE);
                        Picasso.with(context)
                                .load(mess.text)
                                .placeholder(R.drawable.loading).resize(356, 356)
                                .centerInside()
                                .into(holder.imgFriend);

                    } else if (mess_type.equals(context.getString(R.string.call_field))) {
                        holder.txtContent.setVisibility(View.GONE);
                        holder.imgFriend.setVisibility(View.GONE);
                        holder.viewCallFriend.setVisibility(View.VISIBLE);
                        holder.viewFileFriend.setVisibility(View.GONE);
                        holder.txtDurationFriend
                                .setText(conversation.getListMessageData().get(position).durationCall);
                    } else if (mess_type.equals(context.getString(R.string.file_field))) {
                        holder.txtContent.setVisibility(View.GONE);
                        holder.imgFriend.setVisibility(View.GONE);
                        holder.viewCallFriend.setVisibility(View.GONE);
                        holder.viewFileFriend.setVisibility(View.VISIBLE);

                        holder.txtFileName.setText(mess.text);

                        addEvents(holder);
                    } else {
                        holder.viewCallFriend.setVisibility(View.GONE);
                        holder.imgFriend.setVisibility(View.GONE);
                        holder.txtContent.setVisibility(View.VISIBLE);
                        holder.viewFileFriend.setVisibility(View.GONE);
                        holder.txtContent
                                .setText(conversation.getListMessageData().get(position).text);
                    }
                }

                setAvatarFriend(holder);

            } else if (holder1 instanceof ItemMessageUserHolder) {

                Message mess = conversation.getListMessageData().get(position);
                ItemMessageUserHolder holder = (ItemMessageUserHolder) holder1;

                if (mess != null) {
                    String mess_type = mess.type;
                    if (mess_type.equals(context.getString(R.string.image_field))) {
                        holder.viewCallUser.setVisibility(View.GONE);
                        holder.txtContent.setVisibility(View.GONE);
                        holder.imgUser.setVisibility(View.VISIBLE);
                        holder.viewFileUser.setVisibility(View.GONE);
                        Picasso.with(context)
                                .load(mess.text)
                                .placeholder(R.drawable.loading).resize(356, 356)
                                .centerInside()
                                .into(holder.imgUser);

                    } else if (mess_type.equals(context.getString(R.string.call_field))) {
                        holder.txtContent.setVisibility(View.GONE);
                        holder.imgUser.setVisibility(View.GONE);
                        holder.viewCallUser.setVisibility(View.VISIBLE);
                        holder.viewFileUser.setVisibility(View.GONE);
                        holder.txtDurationUser
                                .setText(conversation.getListMessageData().get(position).durationCall);
                    } else if (mess_type.equals(context.getString(R.string.file_field))) {
                        holder.txtContent.setVisibility(View.GONE);
                        holder.imgUser.setVisibility(View.GONE);
                        holder.viewCallUser.setVisibility(View.GONE);
                        holder.viewFileUser.setVisibility(View.VISIBLE);

                        holder.txtFileName.setText(mess.text);
                    } else {
                        holder.viewCallUser.setVisibility(View.GONE);
                        holder.imgUser.setVisibility(View.GONE);
                        holder.txtContent.setVisibility(View.VISIBLE);
                        holder.viewFileUser.setVisibility(View.GONE);
                        holder.txtContent
                                .setText(mess.text);
                    }
                }

                if (ChatActivity.bitmapAvataUser != null) {
                    holder.avata.setImageBitmap(ChatActivity.bitmapAvataUser);
                }
            }

            addEvents(holder1);
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    private void addEvents(RecyclerView.ViewHolder holder1) {
        try {
            int position = holder1.getAdapterPosition();
            Message message = conversation.getListMessageData().get(position);

            if (message != null) {
                if (message.type.equalsIgnoreCase(context.getString(R.string.file_field))) {
                    if (holder1 instanceof ItemMessageFriendHolder) {
                        ItemMessageFriendHolder holder = (ItemMessageFriendHolder) holder1;

                        holder.viewFileFriend.setOnClickListener(v -> {
                            Intent intentPdf = new Intent(Intent.ACTION_VIEW, Uri.parse(message.link));
                            context.startActivity(intentPdf);
                        });
                    } if (holder1 instanceof ItemMessageUserHolder) {
                        ItemMessageUserHolder holder = (ItemMessageUserHolder) holder1;

                        holder.viewFileUser.setOnClickListener(v -> {
                            Intent intentPdf = new Intent(Intent.ACTION_VIEW, Uri.parse(message.link));
                            context.startActivity(intentPdf);
                        });
                    }
                }
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
        }
    }

    private void setAvatarFriend(ItemMessageFriendHolder holder) {
        try {
            int position = holder.getAdapterPosition();

            Bitmap currentAvata = bitmapAvata.
                    get(conversation.getListMessageData().get(position).idSender);
            if (currentAvata != null) {
                holder.avata.setImageBitmap(currentAvata);
            } else {
                final String id = conversation.getListMessageData().get(position).idSender;
                if (bitmapAvataDB.get(id) == null) {
                    bitmapAvataDB.put(id, mDatabaseRef.child(context.getString(R.string.users))
                            .child(id).child(context.getString(R.string.avatar_field)));
                    bitmapAvataDB.get(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try {
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
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            Crashlytics.logException(e);
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
        public TextView txtContent, txtDurationUser, txtFileName;
        public CircleImageView avata;
        public ImageView imgUser;
        public View viewCallUser, viewFileUser;

        public ItemMessageUserHolder(View itemView) {
            super(itemView);
            txtContent = itemView.findViewById(R.id.textContentUser);
            avata = itemView.findViewById(R.id.imageView2);
            imgUser = itemView.findViewById(R.id.message_image_layout_user);
            txtDurationUser = itemView.findViewById(R.id.txtDurationUser);
            viewCallUser = itemView.findViewById(R.id.viewCallUser);
            txtFileName = itemView.findViewById(R.id.tvFileName);
            viewFileUser = itemView.findViewById(R.id.viewFileUser);
        }
    }

    class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
        public TextView txtContent, txtDurationFriend, txtFileName;
        public CircleImageView avata;
        public ImageView imgFriend;
        public View viewCallFriend, viewFileFriend;

        public ItemMessageFriendHolder(View itemView) {
            super(itemView);
            txtContent = itemView.findViewById(R.id.textContentFriend);
            avata = itemView.findViewById(R.id.imageView3);
            imgFriend = itemView.findViewById(R.id.message_image_layout_friend);
            txtDurationFriend = itemView.findViewById(R.id.txtDurationFriend);
            viewCallFriend = itemView.findViewById(R.id.viewCallFriend);
            txtFileName = itemView.findViewById(R.id.tvFileName);
            viewFileFriend = itemView.findViewById(R.id.viewFileFriend);
        }
    }
}

