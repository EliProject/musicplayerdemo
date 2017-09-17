package com.example.eli.musicplayerdemo;

/**
 * Created by liyuanqin on 17-9-7.
 */

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MusicPlayerAdapter extends BaseAdapter {

    private Context context;

    private List<MediaActivity.MusicPlayers> list;

    public MusicPlayerAdapter(Context context, List<MediaActivity.MusicPlayers> list) {

        this.context = context;
        this.list = list;

    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.music_player_item, null);
            holder = new ViewHolder();

            convertView.setTag(holder);

            holder.title = (TextView) convertView.findViewById(R.id.music_player_title);
            holder.icon = (ImageView) convertView.findViewById(R.id.music_player_icon);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.title.setTextColor(Color.BLACK);
        holder.title.setText(list.get(position).title);
        holder.icon.setImageDrawable(list.get(position).icon);

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
    }

}
