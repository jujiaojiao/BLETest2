package com.example.administrator.bletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 2017/6/23.
 */

public class ListAdapter extends BaseAdapter {
    private List<BluetoothDevice> list;
    private Context context;
    public ListAdapter(List<BluetoothDevice> list,Context context){
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
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView==null){
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_listview, null);
            holder.item = ((TextView) convertView.findViewById(R.id.item));
            convertView.setTag(holder);
        }
        holder= (ViewHolder) convertView.getTag();
        holder.item.setPadding(40,20,20,20);
        holder.item.setTextSize(20);
        holder.item.setText(list.get(position).getName());
        return convertView;
    }
}

class ViewHolder {
    public TextView item;
}
