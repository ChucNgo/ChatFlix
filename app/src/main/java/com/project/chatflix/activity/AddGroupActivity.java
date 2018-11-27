package com.project.chatflix.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.chatflix.R;
import com.project.chatflix.adapter.ListChoosePeopleAdapter;
import com.project.chatflix.database.GroupDB;
import com.project.chatflix.fragment.ChatFragment;
import com.project.chatflix.object.Group;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.object.Room;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.util.HashSet;
import java.util.Set;

public class AddGroupActivity extends AppCompatActivity {

    private RecyclerView recyclerListFriend;
    private ListChoosePeopleAdapter adapter;
    private ListFriend listFriend;
    private LinearLayout btnAddGroup;
    private Set<String> listIDChoose;
    private Set<String> listIDRemove;
    private EditText editTextGroupName;
    private TextView txtGroupIcon, txtActionName;
    private LovelyProgressDialog dialogWait;
    private boolean isEditGroup;
    private Group groupEdit;
    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);
        initViews();
        addEvents();
    }

    private void initViews() {
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();

        Intent intentData = getIntent();
        txtActionName = findViewById(R.id.txtActionName);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        listFriend = ChatFragment.dataListFriend;
        listIDChoose = new HashSet<>();
        listIDRemove = new HashSet<>();
        listIDChoose.add(StaticConfig.UID);
        btnAddGroup = findViewById(R.id.btnAddGroup);
        editTextGroupName = findViewById(R.id.editGroupName);
        txtGroupIcon = findViewById(R.id.icon_group);
        dialogWait = new LovelyProgressDialog(this).setCancelable(false);

        if (intentData.getStringExtra(getString(R.string.group_id)) != null) {
            isEditGroup = true;
            String idGroup = intentData.getStringExtra(getString(R.string.group_id));
            txtActionName.setText(getString(R.string.save));
            btnAddGroup.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            groupEdit = GroupDB.getInstance(this).getGroup(idGroup);
            editTextGroupName.setText(groupEdit.groupInfo.get(getString(R.string.name_field)));
        } else {
            isEditGroup = false;
        }

        recyclerListFriend = findViewById(R.id.recycleListFriend);
        recyclerListFriend.setLayoutManager(linearLayoutManager);
        adapter = new ListChoosePeopleAdapter(this, listFriend, btnAddGroup, listIDChoose, listIDRemove, isEditGroup, groupEdit);
        recyclerListFriend.setAdapter(adapter);
    }

    private void addEvents() {
        editTextGroupName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() >= 1) {
                    txtGroupIcon.setText((charSequence.charAt(0) + "").toUpperCase());
                } else {
                    txtGroupIcon.setText("R");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btnAddGroup.setOnClickListener(v -> {
            if (listIDChoose.size() < 3) {
                Toast.makeText(AddGroupActivity.this, getString(R.string.add_at_least_two_people), Toast.LENGTH_SHORT).show();
            } else {
                if (editTextGroupName.getText().length() == 0) {
                    Toast.makeText(AddGroupActivity.this, getString(R.string.enter_group_name), Toast.LENGTH_SHORT).show();
                } else {
                    if (isEditGroup) {
                        editGroup();
                    } else {
                        createGroup();
                    }
                }
            }
        });
    }

    private void editGroup() {
        //Show dialog wait
        dialogWait.setIcon(R.drawable.ic_add_group_dialog)
                .setTitle(getString(R.string.editing))
                .setTopColorRes(R.color.colorPrimary)
                .show();
        //Delete group
        final String idGroup = groupEdit.id;
        Room room = new Room();
        for (String id : listIDChoose) {
            room.member.add(id);
        }
        room.groupInfo.put(getString(R.string.name_field), editTextGroupName.getText().toString());
        room.groupInfo.put(getString(R.string.admin), StaticConfig.UID);
        mDatabaseRef.child(getString(R.string.group_table) + "/" + idGroup).setValue(room)
                .addOnCompleteListener(task -> {
                    addRoomForUser(idGroup, 0);
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialogWait.dismiss();
                        new LovelyInfoDialog(AddGroupActivity.this) {
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
                                .setIcon(R.drawable.ic_add_group_dialog)
                                .setTitle(getString(R.string.failed))
                                .setMessage(getString(R.string.cannot_connect_database))
                                .setCancelable(false)
                                .setConfirmButtonText(getString(R.string.ok))
                                .show();
                    }
                });
    }

    private void createGroup() {
        //Show dialog wait
        dialogWait.setIcon(R.drawable.ic_add_group_dialog)
                .setTitle(getString(R.string.registering))
                .setTopColorRes(R.color.colorPrimary)
                .show();

        final String idGroup = (StaticConfig.UID + System.currentTimeMillis()).hashCode() + "";
        Room room = new Room();
        for (String id : listIDChoose) {
            room.member.add(id);
        }
        room.groupInfo.put(getString(R.string.name_field), editTextGroupName.getText().toString());
        room.groupInfo.put(getString(R.string.admin), StaticConfig.UID);
        mDatabaseRef.child(getString(R.string.group_table) + "/" + idGroup).setValue(room)
                .addOnCompleteListener(task -> {
                    addRoomForUser(idGroup, 0);
                });
    }

    private void deleteRoomForUser(final String roomId, final int userIndex) {
        if (userIndex == listIDRemove.size()) {
            dialogWait.dismiss();
            Toast.makeText(this, getString(R.string.edit_group_success), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, null);
            AddGroupActivity.this.finish();
        } else {
            mDatabaseRef.child(getString(R.string.users) + "/" + listIDRemove.toArray()[userIndex])
                    .child(getString(R.string.group_field))
                    .child(roomId)
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        deleteRoomForUser(roomId, userIndex + 1);
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            dialogWait.dismiss();
                            new LovelyInfoDialog(AddGroupActivity.this) {
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
                                    .setIcon(R.drawable.ic_add_group_dialog)
                                    .setTitle(getString(R.string.failed))
                                    .setMessage(getString(R.string.cannot_connect_database))
                                    .setCancelable(false)
                                    .setConfirmButtonText(getString(R.string.ok))
                                    .show();
                        }
                    });
        }

    }

    private void addRoomForUser(final String roomId, final int userIndex) {
        if (userIndex == listIDChoose.size()) {
            if (!isEditGroup) {
                dialogWait.dismiss();
                Toast.makeText(this, getString(R.string.create_group_success), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK, null);
                AddGroupActivity.this.finish();
            } else {
                deleteRoomForUser(roomId, 0);
            }
        } else {
            mDatabaseRef.child(getString(R.string.users) + "/" + listIDChoose.toArray()[userIndex])
                    .child(getString(R.string.group_field))
                    .child(roomId).setValue(roomId)
                    .addOnCompleteListener(task -> {
                        addRoomForUser(roomId, userIndex + 1);
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    dialogWait.dismiss();
                    new LovelyInfoDialog(AddGroupActivity.this) {
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
                            .setIcon(R.drawable.ic_add_group_dialog)
                            .setTitle(getString(R.string.failed))
                            .setMessage(getString(R.string.create_group_failed))
                            .setCancelable(false)
                            .setConfirmButtonText(getString(R.string.ok))
                            .show();
                }
            });
        }
    }
}