package com.project.chatflix.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.chatflix.MainActivity;
import com.project.chatflix.R;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends Activity
//        implements SinchService.StartFailedListener
{

    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";

    public static final String MY_PREFERENCES = "MyPrefs";

    EditText txtEmailLogin, txtPasswordLogin;
    Button btnSignup, btnReg;
    String email, password;

    private final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static String TAG = "LoginActivity";
    // progress dialog
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private boolean firstTimeAccess;
    private LinearLayout layoutRememberLogin;
    SharedPreferences sharedpreferences;

    private LovelyProgressDialog waitingDialog;
    private ImageView imgRemember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
        addEvent();
        loadAccount();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            email = (String) extras.get("email");
            password = (String) extras.get("password");
            txtEmailLogin.setText(email);
            txtPasswordLogin.setText(password);
        }


        firstTimeAccess = true;
        initFirebase();

    }

    private void initView() {
        getWindow().setBackgroundDrawableResource(R.drawable.background);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        waitingDialog = new LovelyProgressDialog(this);
        txtEmailLogin = (EditText) findViewById(R.id.txtEmailLogin);
        txtPasswordLogin = (EditText) findViewById(R.id.txtPasswordLogin);
        btnSignup = (Button) findViewById(R.id.txtSignup);
        btnReg = (Button) findViewById(R.id.btnReg);
        layoutRememberLogin = (LinearLayout) findViewById(R.id.layout_remember_login);
        imgRemember = (ImageView) findViewById(R.id.radioButton);
    }

    private void addEvent() {
        btnReg.setOnClickListener(v -> {
            Intent reg_intent = new Intent(LoginActivity.this, NextRegisterActivity.class);
            startActivity(reg_intent);
        });

        btnSignup.setOnClickListener(v -> {

            String email = txtEmailLogin.getText().toString();
            String password = txtPasswordLogin.getText().toString();

            if (!TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)) {

                if (validate(email, password)) {
                    progressDialog.setTitle("Sign in");
                    progressDialog.setMessage("Please wait....!");
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();

                    loginUser(email, password);
                    if (imgRemember.getTag().toString().equalsIgnoreCase("1")) {
                        SharedPreferences.Editor editor = sharedpreferences.edit();

                        editor.putString(EMAIL, email);
                        editor.putString(PASSWORD, password);
                        editor.apply();
                    }
//                        if (!getSinchServiceInterface().isStarted()) {
//                            getSinchServiceInterface().startClient(email);
////                            Toast.makeText(LoginActivity.this,"Start Service",Toast.LENGTH_SHORT).show();
//                        }

                } else {
                    Toast.makeText(LoginActivity.this, "Invalid email!", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(LoginActivity.this, "Must fill all requirements!", Toast.LENGTH_SHORT).show();
            }
        });

        layoutRememberLogin.setOnClickListener(v -> {
            int tag = Integer.parseInt(imgRemember.getTag().toString());

            if (tag == 0) {
                imgRemember.setTag(1);
                imgRemember.setBackgroundResource(R.drawable.rb_checked);
            } else {
                imgRemember.setTag(0);
                imgRemember.setBackgroundResource(R.drawable.rb_unchecked);
            }
        });
    }

//    @Override
//    protected void onServiceConnected() {
//        getSinchServiceInterface().setStartListener(this);
//    }
//
//    @Override
//    public void onStartFailed(SinchError error) {
//        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
//    }

//    @Override
//    public void onStarted() {
//
//    }

    private void loadAccount() {
        sharedpreferences = getSharedPreferences(MY_PREFERENCES,
                Context.MODE_PRIVATE);
        if (sharedpreferences.contains(EMAIL)) {
            txtEmailLogin.setText(sharedpreferences.getString(EMAIL, ""));
        }
        if (sharedpreferences.contains(PASSWORD)) {
            txtPasswordLogin.setText(sharedpreferences.getString(PASSWORD, ""));
        }
    }

    private boolean validate(String emailStr, String password) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return (password.length() > 0 || password.equals(";")) && matcher.find();
    }


    /**
     * Khởi tạo các thành phần cần thiết cho việc quản lý đăng nhập
     */
    private void initFirebase() {
        //Khoi tao thanh phan de dang nhap, dang ky
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    StaticConfig.UID = user.getUid();
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    if (firstTimeAccess) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        LoginActivity.this.finish();
                    }
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                firstTimeAccess = false;
            }
        };

        //Khoi tao dialog waiting khi dang nhap
        waitingDialog = new LovelyProgressDialog(this).setCancelable(false);
    }

    private void loginUser(final String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressDialog.dismiss();
                // Khởi động service Sinch
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                progressDialog.hide();
                Toast.makeText(LoginActivity.this, "Email or Password is invalid! Please try again!", Toast.LENGTH_LONG).show();
            }
        });
    }
}
