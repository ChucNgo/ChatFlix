package com.project.chatflix.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.project.chatflix.R;
import com.project.chatflix.activity.ChatActivity;
import com.project.chatflix.fragment.ChatFragment;
import com.project.chatflix.fragment.GroupFragment;
import com.project.chatflix.object.Group;
import com.project.chatflix.object.ListFriend;
import com.project.chatflix.utils.StaticConfig;

import java.util.ArrayList;
import java.util.HashMap;

public class ListGroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Group> listGroup;
    public static ListFriend listFriend = null;
    private Context context;

    public ListGroupsAdapter(Context context, ArrayList<Group> listGroup) {
        this.context = context;
        this.listGroup = listGroup;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new ItemGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final String groupName = listGroup.get(position).groupInfo.get(context.getString(R.string.name_field));
        if (groupName != null && groupName.length() > 0) {
            ((ItemGroupViewHolder) holder).txtGroupName.setText(groupName);
            ((ItemGroupViewHolder) holder).iconGroup.setText((groupName.charAt(0) + "").toUpperCase());
        }
        ((ItemGroupViewHolder) holder).btnMore.setOnClickListener(view -> {
            view.setTag(new Object[]{groupName, position});
            view.getParent().showContextMenuForChild(view);
        });
        ((RelativeLayout) ((ItemGroupViewHolder) holder).txtGroupName.getParent())
                .setOnClickListener(view -> {
                    listFriend = ChatFragment.dataListFriend;

                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, groupName);
                    ArrayList<CharSequence> idFriend = new ArrayList<>();
                    ChatActivity.bitmapAvataFriend = new HashMap<>();

                    for (String id : listGroup.get(position).member) {
                        idFriend.add(id);
                        String avatar = listFriend.getAvataById(id);

                        if (!avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                            byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                            ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                        } else if (avatar.equals(StaticConfig.STR_DEFAULT_BASE64)) {
                            ChatActivity.bitmapAvataFriend.put(id, BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avatar));
                        } else {
                            ChatActivity.bitmapAvataFriend.put(id, null);
                        }

                    }
                    intent.putCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID, idFriend);
                    intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, listGroup.get(position).id);
                    intent.putExtra(context.getString(R.string.kind_of_chat), context.getString(R.string.group_chat));
                    context.startActivity(intent);
                });
    }

    @Override
    public int getItemCount() {
        return listGroup.size();
    }

    class ItemGroupViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        public TextView iconGroup, txtGroupName;
        public ImageButton btnMore;

        public ItemGroupViewHolder(View itemView) {
            super(itemView);
            itemView.setOnCreateContextMenuListener(this);
            iconGroup = itemView.findViewById(R.id.icon_group);
            txtGroupName = itemView.findViewById(R.id.txtName);
            btnMore = itemView.findViewById(R.id.btnMoreAction);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            menu.setHeaderTitle((String) ((Object[]) btnMore.getTag())[0]);
            Intent data = new Intent();
            data.putExtra(GroupFragment.CONTEXT_MENU_KEY_INTENT_DATA_POS, (Integer) ((Object[]) btnMore.getTag())[1]);
            menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_EDIT, Menu.NONE, context.getString(R.string.edit_group)).setIntent(data);
            menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_DELETE, Menu.NONE, context.getString(R.string.delete_group)).setIntent(data);
            menu.add(Menu.NONE, GroupFragment.CONTEXT_MENU_LEAVE, Menu.NONE, context.getString(R.string.leave_group)).setIntent(data);
        }
    }
}
