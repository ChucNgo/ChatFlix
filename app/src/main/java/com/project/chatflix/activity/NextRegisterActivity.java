package com.project.chatflix.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.chatflix.R;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NextRegisterActivity extends AppCompatActivity {

    private static final String TAG = "hello";
    public static String STR_EXTRA_ACTION_REGISTER = "register";
    private final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    TextInputLayout txtName,txtEmail,txtPassword;
    Button btnCreateAcc;

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference database;

    // Progress Dialog
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_register);

        auth = FirebaseAuth.getInstance();

        txtName = (TextInputLayout) findViewById(R.id.txtName);
        txtEmail = (TextInputLayout) findViewById(R.id.txtEmail);
        txtPassword = (TextInputLayout) findViewById(R.id.txtPassword);

        btnCreateAcc = (Button) findViewById(R.id.btnCreateAcc);

        progressDialog = new ProgressDialog(this);

        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnCreateAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = txtName.getEditText().getText().toString();
                String email = txtEmail.getEditText().getText().toString();
                String password = txtPassword.getEditText().getText().toString();

                if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)){
                    if (validate(email,password,name)){
                        progressDialog.setTitle("Register User");
                        progressDialog.setMessage("Creating user...");
                        progressDialog.show();
                        register_user(name,email,password);
                    }else {
                        Toast.makeText(NextRegisterActivity.this, "Invalid email!", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(NextRegisterActivity.this, "Must fill all requirements!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private boolean validate(String emailStr, String password, String name) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return password.length() > 0 && name.length() > 0 && matcher.find();
    }


    private void register_user(final String name, final String email, final String password) {

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){

                    FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                    String uid = current_user.getUid();

                    // Lấy từ User, lấy uid
                    database = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

                    // Lưu vào database name,status với id mặc định
                    HashMap<String,String> userMap = new HashMap<>();
                    userMap.put("name", name);
                    userMap.put("avatar","default");
                    userMap.put("email", email);
                    userMap.put("user_id", uid);
//                    userMap.put("friend","");

                    database.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                progressDialog.dismiss();

                                Intent intent = new Intent(NextRegisterActivity.this,LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent.putExtra("email",email);
                                intent.putExtra("password",password);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });

                }else {
                    progressDialog.hide();
                    FirebaseAuthException e = (FirebaseAuthException)task.getException();
                    Toast.makeText(NextRegisterActivity.this, "Failed Registration: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("LoginActivity", "Failed Registration", e);
                    return;
//                    Toast.makeText(NextRegisterActivity.this,"Failed",Toast.LENGTH_SHORT).show();
                }
            }
        });





    }
}
