package com.project.chatflix.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.adapter.UserInfoAdapter;
import com.project.chatflix.database.SharedPreferenceHelper;
import com.project.chatflix.object.Configuration;
import com.project.chatflix.object.User;
import com.project.chatflix.utils.ImageUtils;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class InfoFragment extends Fragment {

    TextView tvUserName;
    ImageView circleAvatar;
    String name, email;

    private DatabaseReference userDB;
    private FirebaseUser currenUser;
    private FirebaseAuth mAuth;

    private List<Configuration> listConfig = new ArrayList<>();
    private RecyclerView recyclerView;
    private UserInfoAdapter infoAdapter;
    private static final int PICK_IMAGE = 1994;

    private LovelyProgressDialog waitingDialog;

    private Context context;
    private User myAccount;

    public InfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        currenUser = FirebaseAuth.getInstance().getCurrentUser();

        String current_uid = currenUser.getUid();

        userDB = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users)).child(current_uid);
//        userDB.addListenerForSingleValueEvent(userListener);
        mAuth = FirebaseAuth.getInstance();

        userDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

//                name = dataSnapshot.child("name").getValue().toString();
//                email = dataSnapshot.child("email").getValue().toString();
//                final String image = dataSnapshot.child("image").getValue().toString();

                //Lấy thông tin của user về và cập nhật lên giao diện
                listConfig.clear();
                myAccount = dataSnapshot.getValue(User.class);
                setupArrayListInfo(myAccount);
                if (infoAdapter != null) {
                    infoAdapter.notifyDataSetChanged();
                }

                if (tvUserName != null) {
                    tvUserName.setText(myAccount.name);
                }

//                setImageAvatar(context, image);
                setImageAvatar(context, myAccount.avatar);
                SharedPreferenceHelper preferenceHelper = SharedPreferenceHelper.getInstance(context);
                preferenceHelper.saveUserInfo(myAccount);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        /** */

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        context = view.getContext();
        circleAvatar = (ImageView) view.findViewById(R.id.circleAvatar);
        circleAvatar.setOnClickListener(onAvatarClick);
        tvUserName = (TextView) view.findViewById(R.id.tv_username);

        SharedPreferenceHelper prefHelper = SharedPreferenceHelper.getInstance(context);
        myAccount = prefHelper.getUserInfo();
        setupArrayListInfo(myAccount);
        setImageAvatar(context, myAccount.avatar);
        tvUserName.setText(myAccount.name);

        recyclerView = (RecyclerView) view.findViewById(R.id.info_recycler_view);
        infoAdapter = new UserInfoAdapter(getActivity(), listConfig, myAccount, (newName -> {
            userDB.child(getString(R.string.name_field)).setValue(newName);
            myAccount.name = newName;
            prefHelper.saveUserInfo(myAccount);

            tvUserName.setText(newName);
            setupArrayListInfo(myAccount);
        }));
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(infoAdapter);

        waitingDialog = new LovelyProgressDialog(context);

        return view;
    }

    /**
     * Khi click vào avatar thì bắn intent mở trình xem ảnh mặc định để chọn ảnh
     */
    private View.OnClickListener onAvatarClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            new AlertDialog.Builder(context)
                    .setTitle(getString(R.string.avatar))
                    .setMessage(getString(R.string.sure_want_to_change_avatar))
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_PICK);
                        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE);
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    }).show();
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(context, getString(R.string.error_occured_please_try_again), Toast.LENGTH_LONG).show();
                return;
            }
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(data.getData());

                Bitmap imgBitmap = BitmapFactory.decodeStream(inputStream);
                imgBitmap = ImageUtils.cropToSquare(imgBitmap);
                InputStream is = ImageUtils.convertBitmapToInputStream(imgBitmap);
                final Bitmap liteImage = ImageUtils.makeImageLite(is,
                        imgBitmap.getWidth(), imgBitmap.getHeight(),
                        ImageUtils.AVATAR_WIDTH, ImageUtils.AVATAR_HEIGHT);

                String imageBase64 = ImageUtils.encodeBase64(liteImage);
                myAccount.avatar = imageBase64;

                waitingDialog.setCancelable(false)
                        .setTitle(getString(R.string.avatar_updating))
                        .setTopColorRes(R.color.colorPrimary)
                        .show();

                userDB.child(getString(R.string.avatar_field)).setValue(imageBase64)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                waitingDialog.dismiss();
                                SharedPreferenceHelper preferenceHelper = SharedPreferenceHelper
                                        .getInstance(context);
                                preferenceHelper.saveUserInfo(myAccount);
                                circleAvatar.setImageDrawable(ImageUtils.roundedImage(context,
                                        liteImage));

                                new LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorPrimary)
                                        .setTitle(getString(R.string.success))
                                        .setMessage(getString(R.string.update_avatar_success))
                                        .show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            waitingDialog.dismiss();
                            new LovelyInfoDialog(context)
                                    .setTopColorRes(R.color.colorAccent)
                                    .setTitle(getString(R.string.failed))
                                    .setMessage(getString(R.string.error_upload_avatar))
                                    .show();
                        });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void setupArrayListInfo(User myAccount) {
        listConfig.clear();
        Configuration userNameConfig = new Configuration(StaticConfig.USERNAME_LABEL, myAccount.name, R.mipmap.ic_account_box);
        listConfig.add(userNameConfig);

        Configuration emailConfig = new Configuration(StaticConfig.EMAIL_LABEL, myAccount.email, R.mipmap.ic_email);
        listConfig.add(emailConfig);

        Configuration resetPass = new Configuration(StaticConfig.RESETPASS_LABEL, "", R.mipmap.ic_restore);
        listConfig.add(resetPass);

        Configuration signout = new Configuration(StaticConfig.SIGNOUT_LABEL, "", R.mipmap.ic_power_settings);
        listConfig.add(signout);
    }

    private void setImageAvatar(Context context, String imgBase64) {
        try {
            Resources res = getResources();
            //Nếu chưa có avatar thì để hình mặc định
            Bitmap src;
            if (imgBase64.equals("default")) {
                src = BitmapFactory.decodeResource(res, R.drawable.default_avatar);
            } else {
                byte[] decodedString = Base64.decode(imgBase64, Base64.DEFAULT);
                src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            }

            circleAvatar.setImageDrawable(ImageUtils.roundedImage(context, src));
        } catch (Exception e) {
        }
    }

}
