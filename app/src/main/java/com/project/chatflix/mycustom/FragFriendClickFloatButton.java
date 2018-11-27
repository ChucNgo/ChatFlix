package com.project.chatflix.mycustom;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.fragment.ChatFragment;
import com.project.chatflix.object.Friend;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragFriendClickFloatButton implements View.OnClickListener {
    private Context context;
    private LovelyProgressDialog dialogWait;
    private DatabaseReference mDatabaseRef;
    private ChatFragment fragment;

    public FragFriendClickFloatButton(ChatFragment chatFragment) {
        this.fragment = chatFragment;
    }

    public FragFriendClickFloatButton getInstance(Context context) {
        this.context = context;
        dialogWait = new LovelyProgressDialog(context);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        return this;
    }

    @Override
    public void onClick(final View view) {
        try {
            new LovelyTextInputDialog(view.getContext(), R.style.EditTextTintTheme)
                    .setTopColorRes(R.color.colorPrimary)
                    .setTitle(context.getString(R.string.add_friend))
                    .setMessage(context.getString(R.string.enter_friend_email))
                    .setIcon(R.drawable.ic_add_friend)
                    .setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    .setInputFilter(context.getString(R.string.email_not_found), text -> {
                        Pattern VALID_EMAIL_ADDRESS_REGEX =
                                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
                        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(text);
                        return matcher.find();
                    })
                    .setConfirmButton(android.R.string.ok, email -> {
                        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                        if (myEmail.equals(email)) {
                            Toast.makeText(context, context.getString(R.string.cannot_add_your_self), Toast.LENGTH_LONG).show();
                        } else {
                            findIDEmail(email);
                        }
                    })
                    .show();
        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }

    private void findIDEmail(final String email) {
        dialogWait.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle(context.getString(R.string.finding_friend))
                .setTopColorRes(R.color.colorPrimary)
                .show();
        mDatabaseRef.child(context.getString(R.string.users))
                .orderByChild(context.getString(R.string.email))
                .equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            dialogWait.dismiss();
                            if (dataSnapshot.getValue() == null) {
                                new LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorAccent)
                                        .setIcon(R.drawable.ic_add_friend)
                                        .setTitle(context.getString(R.string.failed))
                                        .setMessage(context.getString(R.string.email_not_found))
                                        .show();
                            } else {

                                for (DataSnapshot emailSnapshot : dataSnapshot.getChildren()) {
                                    String id = String.valueOf(emailSnapshot.child(context.getString(R.string.user_id)).getValue());

                                    if (TextUtils.isEmpty(id)) {
                                        new LovelyInfoDialog(context)
                                                .setTopColorRes(R.color.colorAccent)
                                                .setIcon(R.drawable.ic_add_friend)
                                                .setTitle(context.getString(R.string.failed))
                                                .setMessage(context.getString(R.string.invalid_email))
                                                .show();
                                    } else {
                                        HashMap userMap = (HashMap) ((HashMap) dataSnapshot.getValue())
                                                .get(id);
                                        Friend user = new Friend();
                                        user.name = (String) userMap.get(context.getString(R.string.name_field));
                                        user.email = (String) userMap.get(context.getString(R.string.email));
                                        user.avatar = (String) userMap.get(context.getString(R.string.avatar_field));
                                        user.id = id;
                                        user.idRoom = id.compareTo(StaticConfig.UID) > 0 ?
                                                (StaticConfig.UID + id).hashCode() + "" : "" + (id + StaticConfig.UID)
                                                .hashCode();

                                        fragment.checkBeforAddFriend(id, user, dialogWait);
                                    }
                                }

                            }
                        } catch (Exception e) {
                            Crashlytics.logException(e);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }


}
