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
    private final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    TextInputLayout txtName, txtEmail, txtPassword;
    Button btnCreateAcc;
    private FirebaseAuth auth;
    private DatabaseReference database;
    private ProgressDialog progressDialog;
    private Toolbar tb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_register);
        initView();
        addEvents();
    }

    private void initView() {
        auth = FirebaseAuth.getInstance();

        txtName = (TextInputLayout) findViewById(R.id.txtName);
        txtEmail = (TextInputLayout) findViewById(R.id.txtEmail);
        txtPassword = (TextInputLayout) findViewById(R.id.txtPassword);

        btnCreateAcc = (Button) findViewById(R.id.btnCreateAcc);

        progressDialog = new ProgressDialog(this);
        tb = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void addEvents() {
        btnCreateAcc.setOnClickListener(v -> {
            String name = txtName.getEditText().getText().toString();
            String email = txtEmail.getEditText().getText().toString();
            String password = txtPassword.getEditText().getText().toString();

            if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)) {
                if (validate(email, password, name)) {
                    progressDialog.setTitle(getString(R.string.register_user));
                    progressDialog.setMessage(getString(R.string.creating_user));
                    progressDialog.show();
                    register_user(name, email, password);
                } else {
                    Toast.makeText(NextRegisterActivity.this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(NextRegisterActivity.this, getString(R.string.fill_all_requirements), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validate(String emailStr, String password, String name) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return password.length() > 0 && name.length() > 0 && matcher.find();
    }


    private void register_user(final String name, final String email, final String password) {

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                String uid = current_user.getUid();

                // Lấy từ User, lấy uid
                database = FirebaseDatabase.getInstance().getReference().child(getString(R.string.users)).child(uid);

                // Lưu vào database name,status với id mặc định
                HashMap<String, String> userMap = new HashMap<>();
                userMap.put(getString(R.string.name_field), name);
                userMap.put(getString(R.string.avatar_field), getString(R.string.default_field));
                userMap.put(getString(R.string.email), email);
                userMap.put(getString(R.string.user_id), uid);

                database.setValue(userMap).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        progressDialog.dismiss();

                        Intent intent = new Intent(NextRegisterActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra(getString(R.string.email), email);
                        intent.putExtra(getString(R.string.password_field), password);
                        startActivity(intent);
                        finish();
                    }
                });

            } else {
                progressDialog.hide();
                FirebaseAuthException e = (FirebaseAuthException) task.getException();
                Toast.makeText(NextRegisterActivity.this, "Failed Registration: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }
}
