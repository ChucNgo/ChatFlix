package com.project.chatflix.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
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

public class InfoFragment extends Fragment {

    TextView tvUserName;
    ImageView circleAvatar;

    private DatabaseReference userDB;

    private List<Configuration> listConfig = new ArrayList<>();
    private RecyclerView recyclerView;
    private UserInfoAdapter infoAdapter;
    private static final int PICK_IMAGE = 1994;

    private LovelyProgressDialog waitingDialog;

    private Context context;
    private User myAccount;
    SharedPreferenceHelper prefHelper;

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

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        initView(view);
        addEvent();

        return view;
    }

    private void initView(View view) {
        context = view.getContext();
        circleAvatar = view.findViewById(R.id.circleAvatar);
        circleAvatar.setOnClickListener(onAvatarClick);
        tvUserName = view.findViewById(R.id.tv_username);

        prefHelper = SharedPreferenceHelper.getInstance(context);
        myAccount = prefHelper.getUserInfo();
        setupArrayListInfo(myAccount);
        setImageAvatar(context, myAccount.avatar);
        tvUserName.setText(myAccount.name);

        recyclerView = view.findViewById(R.id.info_recycler_view);
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

        userDB = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users)).child(StaticConfig.UID);
    }

    private void addEvent() {
        userDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                listConfig.clear();
                myAccount = dataSnapshot.getValue(User.class);
                setupArrayListInfo(myAccount);
                if (infoAdapter != null) {
                    infoAdapter.notifyDataSetChanged();
                }

                if (tvUserName != null) {
                    tvUserName.setText(myAccount.name);
                }

                setImageAvatar(context, myAccount.avatar);
                SharedPreferenceHelper preferenceHelper = SharedPreferenceHelper.getInstance(context);
                preferenceHelper.saveUserInfo(myAccount);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
            Bitmap src;
            if (imgBase64.equals(context.getString(R.string.default_field))) {
                src = BitmapFactory.decodeResource(res, R.drawable.default_avatar);
            } else {
                byte[] decodedString = Base64.decode(imgBase64, Base64.DEFAULT);
                src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            }

            circleAvatar.setImageDrawable(ImageUtils.roundedImage(context, src));
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.toString());
        }
    }

    private View.OnClickListener onAvatarClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                    .setTitle(getString(R.string.avatar))
                    .setMessage(getString(R.string.sure_want_to_change_avatar))
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        requestPermission();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    }).show();
        }
    };

    public void handleImageUpload(Intent data) {
        try {
            List<Image> images = ImagePicker.getImages(data);

            Bitmap imgBitmap = BitmapFactory.decodeFile(images.get(0).getPath());
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
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.toString());
        }
    }

    public void requestPermission() {

        int checkCameraPer = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA);
        int checkWiteExPer = ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int checkReadExPer = ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE);
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
                ActivityCompat.requestPermissions(getActivity(), permissions, StaticConfig.PERMISSION_CONSTANT);

            } else {
                sendToGallery();
            }
        } else {
            sendToGallery();
        }
    }

    private void sendToGallery() {
        ImagePicker.create(getActivity())
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
}
