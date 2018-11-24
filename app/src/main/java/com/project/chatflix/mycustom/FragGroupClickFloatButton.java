package com.project.chatflix.mycustom;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.project.chatflix.activity.AddGroupActivity;

public class FragGroupClickFloatButton implements View.OnClickListener {

    Context context;

    public FragGroupClickFloatButton getInstance(Context context) {
        this.context = context;
        return this;
    }

    @Override
    public void onClick(View view) {
        context.startActivity(new Intent(context, AddGroupActivity.class));
    }
}
