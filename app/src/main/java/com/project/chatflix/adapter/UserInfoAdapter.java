package com.project.chatflix.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.project.chatflix.interfaces.CallBack;
import com.project.chatflix.R;
import com.project.chatflix.activity.LoginActivity;
import com.project.chatflix.database.FriendDB;
import com.project.chatflix.database.GroupDB;
import com.project.chatflix.object.Configuration;
import com.project.chatflix.object.User;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import java.util.List;

public class UserInfoAdapter extends RecyclerView.Adapter<UserInfoAdapter.ViewHolder> {
    private List<Configuration> profileConfig;
    private Context context;
    private FirebaseAuth mAuth;
    private User myAccount;
    private CallBack listener;

    public UserInfoAdapter(Context ct, List<Configuration> profileConfig, User user, CallBack callBack) {
        this.context = ct;
        this.profileConfig = profileConfig;
        this.myAccount = user;
        this.listener = callBack;
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_info_layout, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Configuration config = profileConfig.get(position);
        holder.label.setText(config.getLabel());
        holder.value.setText(config.getValue());
        holder.icon.setImageResource(config.getIcon());
        ((RelativeLayout) holder.label.getParent()).setOnClickListener(view -> {
            if (config.getLabel().equals(StaticConfig.SIGNOUT_LABEL)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getResources().getString(R.string.sign_out_title));
                builder.setMessage(context.getResources().getString(R.string.sign_out_message));
                builder.setPositiveButton(context.getString(R.string.yes), (dialogInterface, i) -> {
                    FirebaseDatabase.getInstance().getReference().child(context.getString(R.string.users))
                            .child(StaticConfig.UID)
                            .child(context.getString(R.string.online)).setValue(ServerValue.TIMESTAMP);
                    FirebaseAuth.getInstance().signOut();
                    FriendDB.getInstance(context).dropDB();
                    GroupDB.getInstance(context).dropDB();

                    context.startActivity(new Intent(context, LoginActivity.class));
                });
                builder.setNegativeButton(context.getString(R.string.no), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
                builder.create().show();
            }

            if (config.getLabel().equals(StaticConfig.USERNAME_LABEL)) {
                View viewInflater = LayoutInflater.from(context)
                        .inflate(R.layout.dialog_edit_username, null, false);
                final EditText input = viewInflater.findViewById(R.id.edit_username);
                input.setText(myAccount.name);

                new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.edit_username))
                        .setView(viewInflater)
                        .setPositiveButton(context.getString(R.string.save), (dialogInterface, i) -> {
                            String newName = input.getText().toString();
                            if (!myAccount.name.equals(newName)) {
                                listener.changeUserName(newName);
                            }
                            dialogInterface.dismiss();
                        })
                        .setNegativeButton(context.getString(R.string.cancel), (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        }).show();
            }

            if (config.getLabel().equals(StaticConfig.RESETPASS_LABEL)) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.password))
                        .setMessage(context.getString(R.string.sure_want_to_reset_password))
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                            resetPassword(myAccount.email);
                            dialogInterface.dismiss();
                        })
                        .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        }).show();
            }
        });
    }

    private void resetPassword(final String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        new LovelyInfoDialog(context) {
                            @Override
                            public LovelyInfoDialog setConfirmButtonText(String text) {
                                findView(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm)
                                        .setOnClickListener(view -> {
                                            dismiss();
                                        });
                                return super.setConfirmButtonText(text);
                            }
                        }
                                .setTopColorRes(R.color.colorPrimary)
                                .setIcon(R.drawable.ic_pass_reset)
                                .setTitle(context.getString(R.string.password_recovery))
                                .setMessage(context.getString(R.string.email_sent_to) + email)
                                .setConfirmButtonText(context.getString(R.string.ok))
                                .show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        new LovelyInfoDialog(context) {
                            @Override
                            public LovelyInfoDialog setConfirmButtonText(String text) {
                                findView(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm)
                                        .setOnClickListener(view -> {
                                            dismiss();
                                        });
                                return super.setConfirmButtonText(text);
                            }
                        }
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_pass_reset)
                                .setTitle(context.getString(R.string.failed))
                                .setMessage(context.getString(R.string.cannot_send_email_to) + email)
                                .setConfirmButtonText(context.getString(R.string.ok))
                                .show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return profileConfig.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView label, value;
        public ImageView icon;

        public ViewHolder(View view) {
            super(view);
            label = view.findViewById(R.id.tv_title);
            value = view.findViewById(R.id.tv_detail);
            icon = view.findViewById(R.id.img_icon);
        }
    }

}
