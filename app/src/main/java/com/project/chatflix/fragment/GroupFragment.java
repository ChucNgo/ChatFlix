package com.project.chatflix.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.chatflix.R;
import com.project.chatflix.activity.AddGroupActivity;
import com.project.chatflix.adapter.ListGroupsAdapter;
import com.project.chatflix.database.GroupDB;
import com.project.chatflix.mycustom.FragGroupClickFloatButton;
import com.project.chatflix.object.Group;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class GroupFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView recyclerListGroups;
    public FragGroupClickFloatButton onClickFloatButton;
    private ArrayList<Group> listGroup;
    private ListGroupsAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    public static final int CONTEXT_MENU_DELETE = 1;
    public static final int CONTEXT_MENU_EDIT = 2;
    public static final int CONTEXT_MENU_LEAVE = 3;
    public static final int REQUEST_EDIT_GROUP = 0;
    public static final String CONTEXT_MENU_KEY_INTENT_DATA_POS = "pos";

    LovelyProgressDialog progressDialog, waitingLeavingGroup;
    private DatabaseReference mDatabaseRef;

    public GroupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_group, container, false);
        initViews(layout);

        return layout;
    }

    private void initViews(View layout) {
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        listGroup = new ArrayList<>();
        recyclerListGroups = layout.findViewById(R.id.recycleListGroup);
        mSwipeRefreshLayout = layout.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerListGroups.setLayoutManager(layoutManager);
        adapter = new ListGroupsAdapter(getContext(), listGroup);
        recyclerListGroups.setAdapter(adapter);
        mSwipeRefreshLayout.setRefreshing(true);
        getListGroup();

        onClickFloatButton = new FragGroupClickFloatButton();
        progressDialog = new LovelyProgressDialog(getContext())
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_delete_group)
                .setTitle(getActivity().getString(R.string.deleting))
                .setTopColorRes(R.color.colorAccent);

        waitingLeavingGroup = new LovelyProgressDialog(getContext())
                .setCancelable(false)
                .setIcon(R.drawable.ic_dialog_delete_group)
                .setTitle(getActivity().getString(R.string.group_leaving))
                .setTopColorRes(R.color.colorAccent);
    }

    private void getListGroup() {
        mDatabaseRef.child(getActivity().getString(R.string.users) + "/" + StaticConfig.UID + "/" + getActivity().getString(R.string.group_field))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            HashMap mapListGroup = (HashMap) dataSnapshot.getValue();
                            Iterator iterator = mapListGroup.keySet().iterator();
                            while (iterator.hasNext()) {
                                String idGroup = (String) mapListGroup.get(iterator.next().toString());
                                Group newGroup = new Group();
                                newGroup.id = idGroup;
                                listGroup.add(newGroup);
                            }
                            getGroupInfo(0);
                        } else {
                            mSwipeRefreshLayout.setRefreshing(false);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_GROUP && resultCode == Activity.RESULT_OK) {
            listGroup.clear();
            ListGroupsAdapter.listFriend = null;
            GroupDB.getInstance(getContext()).dropDB();
            getListGroup();
        }
    }

    private void getGroupInfo(final int indexGroup) {
        if (indexGroup == listGroup.size()) {
            adapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        } else {
            mDatabaseRef.child(getString(R.string.group_table) + "/" + listGroup.get(indexGroup).id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                HashMap mapGroup = (HashMap) dataSnapshot.getValue();
                                ArrayList<String> member = (ArrayList<String>) mapGroup.get(getActivity().getString(R.string.member));
                                HashMap mapGroupInfo = (HashMap) mapGroup.get(getActivity().getString(R.string.group_info));
                                for (String idMember : member) {
                                    listGroup.get(indexGroup).member.add(idMember);
                                }
                                listGroup.get(indexGroup).groupInfo.put(getActivity().getString(R.string.name_field), (String) mapGroupInfo.get(getActivity().getString(R.string.name_field)));
                                listGroup.get(indexGroup).groupInfo.put(getActivity().getString(R.string.admin), (String) mapGroupInfo.get(getActivity().getString(R.string.admin)));
                            }
                            GroupDB.getInstance(getContext()).addGroup(listGroup.get(indexGroup));
                            getGroupInfo(indexGroup + 1);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }
    }

    @Override
    public void onRefresh() {
        listGroup.clear();
        ListGroupsAdapter.listFriend = null;
        GroupDB.getInstance(getContext()).dropDB();
        adapter.notifyDataSetChanged();
        getListGroup();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case CONTEXT_MENU_DELETE:
                int posGroup = item.getIntent().getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1);
                if ((listGroup.get(posGroup).groupInfo.get(getActivity().getString(R.string.admin))).equals(StaticConfig.UID)) {
                    Group group = listGroup.get(posGroup);
                    listGroup.remove(posGroup);
                    if (group != null) {
                        deleteGroup(group, 0);
                    }
                } else {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.you_are_not_admin), Toast.LENGTH_LONG).show();
                }
                break;
            case CONTEXT_MENU_EDIT:
                int posGroup1 = item.getIntent().getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1);
                if ((listGroup.get(posGroup1).groupInfo.get(getActivity().getString(R.string.admin))).equals(StaticConfig.UID)) {
                    Intent intent = new Intent(getContext(), AddGroupActivity.class);
                    intent.putExtra(getActivity().getString(R.string.group_id), listGroup.get(posGroup1).id);
                    startActivityForResult(intent, REQUEST_EDIT_GROUP);
                } else {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.you_are_not_admin), Toast.LENGTH_LONG).show();
                }

                break;

            case CONTEXT_MENU_LEAVE:
                int position = item.getIntent().getIntExtra(CONTEXT_MENU_KEY_INTENT_DATA_POS, -1);
                if ((listGroup.get(position).groupInfo.get(getActivity().getString(R.string.admin))).equals(StaticConfig.UID)) {
                    Toast.makeText(getActivity(), getActivity().getString(R.string.admin_cannot_leave_group), Toast.LENGTH_LONG).show();
                } else {
                    waitingLeavingGroup.show();
                    Group groupLeaving = listGroup.get(position);
                    leaveGroup(groupLeaving);
                }
                break;
        }

        return super.onContextItemSelected(item);
    }

    public void deleteGroup(final Group group, final int index) {
        if (index == group.member.size()) {
            mDatabaseRef.child(getString(R.string.group_table) + "/" + group.id).removeValue()
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        GroupDB.getInstance(getContext()).deleteGroup(group.id);
                        listGroup.remove(group);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), getActivity().getString(R.string.group_deleted), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        new LovelyInfoDialog(getContext())
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_dialog_delete_group)
                                .setTitle(getActivity().getString(R.string.failed))
                                .setMessage(getActivity().getString(R.string.error_occured_please_try_again))
                                .setCancelable(false)
                                .setConfirmButtonText(getActivity().getString(R.string.ok))
                                .show();
                    })
            ;
        } else {
            mDatabaseRef
                    .child(getActivity().getString(R.string.users) + "/" + group.member.get(index) + "/" + getString(R.string.group_field) + "/" + group.id)
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        deleteGroup(group, index + 1);
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        new LovelyInfoDialog(getContext())
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_dialog_delete_group)
                                .setTitle(getActivity().getString(R.string.failed))
                                .setMessage(getActivity().getString(R.string.error_occured_please_try_again))
                                .setCancelable(false)
                                .setConfirmButtonText(getActivity().getString(R.string.ok))
                                .show();
                    })
            ;
        }

    }

    public void leaveGroup(final Group group) {
        mDatabaseRef.child(getString(R.string.group_table) + "/" + group.id + "/" + getActivity().getString(R.string.member))
                .orderByValue().equalTo(StaticConfig.UID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getValue() == null) {
                            //email not found
                            waitingLeavingGroup.dismiss();
                            GroupDB.getInstance(getContext()).deleteGroup(group.id);
                            new LovelyInfoDialog(getContext())
                                    .setTopColorRes(R.color.colorAccent)
                                    .setTitle(getActivity().getString(R.string.error))
                                    .setMessage(getActivity().getString(R.string.error_occured_please_try_again))
                                    .show();
                        } else {
                            String memberIndex = "";
                            ArrayList<String> result = ((ArrayList<String>) dataSnapshot.getValue());
                            for (int i = 0; i < result.size(); i++) {
                                if (result.get(i) != null) {
                                    memberIndex = String.valueOf(i);
                                }
                            }

                            mDatabaseRef.child(getActivity().getString(R.string.users)).child(StaticConfig.UID)
                                    .child(getString(R.string.group_table)).child(group.id).removeValue();
                            mDatabaseRef.child(getString(R.string.group_table) + "/" + group.id)
                                    .child(getActivity().getString(R.string.member))
                                    .child(memberIndex).removeValue()
                                    .addOnCompleteListener(task -> {
                                        waitingLeavingGroup.dismiss();

                                        listGroup.remove(group);
                                        adapter.notifyDataSetChanged();
                                        new LovelyInfoDialog(getContext())
                                                .setTopColorRes(R.color.colorAccent)
                                                .setTitle(getActivity().getString(R.string.success))
                                                .setMessage(getActivity().getString(R.string.group_leaving_successfully))
                                                .show();
                                    })
                                    .addOnFailureListener(e -> {
                                        waitingLeavingGroup.dismiss();
                                        new LovelyInfoDialog(getContext())
                                                .setTopColorRes(R.color.colorAccent)
                                                .setTitle(getActivity().getString(R.string.error))
                                                .setMessage(getActivity().getString(R.string.error_occured_please_try_again))
                                                .show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //email not found
                        waitingLeavingGroup.dismiss();
                        new LovelyInfoDialog(getContext())
                                .setTopColorRes(R.color.colorAccent)
                                .setTitle(getActivity().getString(R.string.error))
                                .setMessage(getActivity().getString(R.string.error_occured_please_try_again))
                                .show();
                    }
                });

    }
}
