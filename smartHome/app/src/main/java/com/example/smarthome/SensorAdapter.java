package com.example.smarthome;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.SensorViewHolder> {

    private List<Sensor> sensors;
    private Context context;

    public SensorAdapter(List<Sensor> sensors) {
        this.sensors = sensors;
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_sensor, parent, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
        Sensor sensor = sensors.get(position);
        holder.name.setText(sensor.getName());
        holder.value.setText(sensor.getValue());
        holder.icon.setImageResource(sensor.getIconRes());

        int color = sensor.isDanger() ? 
                ContextCompat.getColor(context, R.color.colorDanger) : 
                ContextCompat.getColor(context, R.color.colorNormal);
        
        holder.indicator.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    public static class SensorViewHolder extends RecyclerView.ViewHolder {
        TextView name, value;
        ImageView icon;
        View indicator;

        public SensorViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.sensor_name);
            value = itemView.findViewById(R.id.sensor_value);
            icon = itemView.findViewById(R.id.sensor_icon);
            indicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}