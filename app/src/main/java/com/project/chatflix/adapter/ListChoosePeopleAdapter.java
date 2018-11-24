package com.project.chatflix.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.project.chatflix.R;
import com.project.chatflix.object.Group;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.utils.StaticConfig;
import java.util.Set;
import de.hdodenhof.circleimageview.CircleImageView;

public class ListChoosePeopleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ListFriend listFriend;
    private LinearLayout btnAddGroup;
    private Set<String> listIDChoose;
    private Set<String> listIDRemove;
    private boolean isEdit;
    private Group editGroup;

    public ListChoosePeopleAdapter(Context context, ListFriend listFriend, LinearLayout btnAddGroup, Set<String> listIDChoose, Set<String> listIDRemove, boolean isEdit, Group editGroup) {
        this.context = context;
        this.listFriend = listFriend;
        this.btnAddGroup = btnAddGroup;
        this.listIDChoose = listIDChoose;
        this.listIDRemove = listIDRemove;

        this.isEdit = isEdit;
        this.editGroup = editGroup;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_add_friend, parent, false);
        return new ItemFriendHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ItemFriendHolder) holder).txtName.setText(listFriend.getListFriend().get(position).name);
        ((ItemFriendHolder) holder).txtEmail.setText(listFriend.getListFriend().get(position).email);
        String avata = listFriend.getListFriend().get(position).avatar;
        final String id = listFriend.getListFriend().get(position).id;
        if (!avata.equals(StaticConfig.STR_DEFAULT_BASE64)) {
            byte[] decodedString = Base64.decode(avata, Base64.DEFAULT);
            ((ItemFriendHolder) holder).avata.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
        } else {
            ((ItemFriendHolder) holder).avata.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avatar));
        }
        ((ItemFriendHolder) holder).checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                listIDChoose.add(id);
                listIDRemove.remove(id);
            } else {
                listIDRemove.add(id);
                listIDChoose.remove(id);
            }
            if (listIDChoose.size() >= 3) {
                btnAddGroup.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
            } else {
                btnAddGroup.setBackgroundColor(context.getResources().getColor(R.color.grey_500));
            }
        });
        if (isEdit && editGroup.member.contains(id)) {
            ((ItemFriendHolder) holder).checkBox.setChecked(true);
        } else if (editGroup != null && !editGroup.member.contains(id)) {
            ((ItemFriendHolder) holder).checkBox.setChecked(false);
        }
    }

    @Override
    public int getItemCount() {
        return listFriend.getListFriend().size();
    }

    class ItemFriendHolder extends RecyclerView.ViewHolder {
        public TextView txtName, txtEmail;
        public CircleImageView avata;
        public CheckBox checkBox;

        public ItemFriendHolder(View itemView) {
            super(itemView);
            txtName = (TextView) itemView.findViewById(R.id.txtName);
            txtEmail = (TextView) itemView.findViewById(R.id.txtEmail);
            avata = (CircleImageView) itemView.findViewById(R.id.icon_avata);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkAddPeople);
        }
    }
}
