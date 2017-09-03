package com.example.aznagy.paintproplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.aznagy.paintproplayer.R;

import com.example.aznagy.paintproplayer.model.Audio;

import java.util.Collections;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

    List<Audio> list = Collections.emptyList();
    Context context;
    int selected = -1;

    public RecyclerViewAdapter(List<Audio> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        holder.title.setText(list.get(position).getTitle());

        if (position == selected) {
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
        } else {
            holder.title.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void setSelectedIndex(int selected) {
        this.selected = selected;
    }
}

class ViewHolder extends RecyclerView.ViewHolder {

    TextView title;
    ImageView img;

    ViewHolder(View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        img = (ImageView) itemView.findViewById(R.id.play_pause);
    }
}