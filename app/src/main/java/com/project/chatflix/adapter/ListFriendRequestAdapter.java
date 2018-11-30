package com.project.chatflix.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.object.User;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

public class ListFriendRequestAdapter extends FirebaseRecyclerAdapter<User, ListFriendRequestAdapter.ItemViewHolder> {

    private Context context;
    private DatabaseReference mDatabaseRef;

    public ListFriendRequestAdapter(Context mContext, @NonNull FirebaseRecyclerOptions<User> options) {
        super(options);
        this.context = mContext;
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return getSnapshots().size();
    }

    @Override
    protected void onBindViewHolder(@NonNull ItemViewHolder holder, int position, @NonNull User model) {

        holder.tvDisplayName.setText(model.getName());

        if (!TextUtils.isEmpty(model.getAvatar())) {
            if (!model.getAvatar().equalsIgnoreCase(context.getString(R.string.default_field))) {
                byte[] decodedString = Base64.decode(model.getAvatar(), Base64.DEFAULT);
                Bitmap src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.imgAvatar.setImageBitmap(src);
            } else {
                holder.imgAvatar.setImageDrawable(context.getDrawable(R.drawable.ic_notify_group));
            }
        } else {
            holder.imgAvatar.setImageDrawable(context.getDrawable(R.drawable.ic_notify_group));
        }

        holder.btnRemove.setOnClickListener(v -> {

            String key = getRef(position).getKey();
            mDatabaseRef.child(context.getString(R.string.request_table)).child(StaticConfig.UID)
                    .child(key).removeValue()
                    .addOnCompleteListener(task -> {
                        Toast.makeText(context, context.getString(R.string.delete_request_successfully), Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(getClass().getSimpleName(), e.toString());
                        Crashlytics.logException(e);
                        Toast.makeText(context, context.getString(R.string.error_occured_please_try_again), Toast.LENGTH_LONG).show();
                    });

        });

        holder.btnAccept.setOnClickListener(v -> {
            addFriend(model.getUser_id(), true, holder.getAdapterPosition());
        });

    }

    private void addFriend(final String idFriend, boolean isIdFriend, int position) {
        if (!TextUtils.isEmpty(idFriend)) {
            if (isIdFriend) {
                mDatabaseRef.child(context.getString(R.string.users))
                        .child(StaticConfig.UID)
                        .child(context.getString(R.string.friend_field)).push().setValue(idFriend)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                addFriend(idFriend, false, position);
                            }
                        })
                        .addOnFailureListener(e -> {
                            new LovelyInfoDialog(context)
                                    .setTopColorRes(R.color.colorAccent)
                                    .setIcon(R.drawable.ic_add_friend)
                                    .setTitle(context.getString(R.string.failed))
                                    .setMessage(context.getString(R.string.add_friend_failed))
                                    .show();
                        });
            } else {
                mDatabaseRef.child(context.getString(R.string.users)).child(idFriend)
                        .child(context.getString(R.string.friend_field)).push().setValue(StaticConfig.UID)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                addFriend(null, false, position);
                            }
                        })
                        .addOnFailureListener(e -> {
                            new LovelyInfoDialog(context)
                                    .setTopColorRes(R.color.colorAccent)
                                    .setIcon(R.drawable.ic_add_friend)
                                    .setTitle(context.getString(R.string.failed))
                                    .setMessage(context.getString(R.string.add_friend_failed))
                                    .show();
                        });
            }
        } else {
            String key = getRef(position).getKey();
            mDatabaseRef.child(context.getString(R.string.request_table)).child(StaticConfig.UID)
                    .child(key).removeValue();
            new LovelyInfoDialog(context)
                    .setTopColorRes(R.color.colorPrimary)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle(context.getString(R.string.success))
                    .setMessage(context.getString(R.string.add_friend_success))
                    .show();
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        ImageView imgAvatar;
        TextView tvDisplayName;
        Button btnAccept, btnRemove;

        public ItemViewHolder(View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar_friend);
            tvDisplayName = itemView.findViewById(R.id.tv_friend_name);
            btnAccept = itemView.findViewById(R.id.btn_accept_friend);
            btnRemove = itemView.findViewById(R.id.btn_deny_friend);
        }
    }

}
