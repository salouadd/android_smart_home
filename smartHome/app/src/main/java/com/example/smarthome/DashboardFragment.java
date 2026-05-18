package com.example.smarthome;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DashboardFragment extends Fragment {

    private RecyclerView recyclerView;
    private SensorAdapter sensorAdapter;
    private List<Sensor> allSensors = new ArrayList<>();
    private List<Sensor> filteredSensors = new ArrayList<>();
    private Random random = new Random();
    
    private View overlay, rowLight, rowFan, rowAlarm, rowCoffee, rowShutters, rowWatering;
    private MaterialCardView devicesCard, cardAiInsight;
    private LinearLayout dashboardContainer;
    private TabLayout tabLayoutRooms;
    
    private ImageView iconLight, iconFan, iconAlarm;
    private TextView statusLight, statusFan, statusAlarm, tvAiInsight;
    private SwitchMaterial switchLight, switchFan, switchAlarm, switchCoffee, switchShutters, switchWatering;

    private LineChart combinedChart;
    private Spinner sensorSpinner;
    private List<Entry> tempEntries = new ArrayList<>(), humEntries = new ArrayList<>(), 
                         gasEntries = new ArrayList<>(), curEntries = new ArrayList<>();
    private int timeIndex = 0;
    private String currentRoom = "Salon";
    private GroqService groqService;
    private static final String API_KEY = "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";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        try {
            initViews(view);
            initSensors();
            setupRecyclerView();
            setupTabs();
            setupChart();
            setupGroq();
            setupListeners(view);
            
            // Premier rafraîchissement forcé
            view.postDelayed(this::refreshData, 200);
        } catch (Exception e) {
            if (getContext() != null) Toast.makeText(getContext(), "Erreur d'init: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return view;
    }

    private void initViews(View view) {
        tabLayoutRooms = view.findViewById(R.id.tab_layout_rooms);
        recyclerView = view.findViewById(R.id.rv_sensors);
        overlay = view.findViewById(R.id.view_light_overlay);
        devicesCard = view.findViewById(R.id.devices_card);
        dashboardContainer = view.findViewById(R.id.dashboard_container);
        iconLight = view.findViewById(R.id.icon_light);
        iconFan = view.findViewById(R.id.icon_fan);
        iconAlarm = view.findViewById(R.id.icon_alarm);
        statusLight = view.findViewById(R.id.status_light);
        statusFan = view.findViewById(R.id.status_fan);
        statusAlarm = view.findViewById(R.id.status_alarm);
        rowLight = view.findViewById(R.id.row_light);
        rowFan = view.findViewById(R.id.row_fan);
        rowAlarm = view.findViewById(R.id.row_alarm);
        rowCoffee = view.findViewById(R.id.row_coffee);
        rowShutters = view.findViewById(R.id.row_shutters);
        rowWatering = view.findViewById(R.id.row_watering);
        switchLight = view.findViewById(R.id.switch_light);
        switchFan = view.findViewById(R.id.switch_fan);
        switchAlarm = view.findViewById(R.id.switch_alarm);
        switchCoffee = view.findViewById(R.id.switch_coffee);
        switchShutters = view.findViewById(R.id.switch_shutters);
        switchWatering = view.findViewById(R.id.switch_watering);
        combinedChart = view.findViewById(R.id.combined_chart);
        sensorSpinner = view.findViewById(R.id.spinner_sensor_filter);
        cardAiInsight = view.findViewById(R.id.card_ai_insight);
        tvAiInsight = view.findViewById(R.id.tv_ai_insight);
    }

    private void setupRecyclerView() {
        sensorAdapter = new SensorAdapter(filteredSensors);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(sensorAdapter);
    }

    private void setupTabs() {
        if (tabLayoutRooms.getTabCount() == 0) {
            tabLayoutRooms.addTab(tabLayoutRooms.newTab().setText("Salon"));
            tabLayoutRooms.addTab(tabLayoutRooms.newTab().setText("Cuisine"));
            tabLayoutRooms.addTab(tabLayoutRooms.newTab().setText("Chambre"));
            tabLayoutRooms.addTab(tabLayoutRooms.newTab().setText("Jardin"));
        }
        tabLayoutRooms.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentRoom = tab.getText().toString();
                filterByRoom(currentRoom);
            }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
    }

    private void initSensors() {
        allSensors.clear();
        allSensors.add(new Sensor("Température", "--", android.R.drawable.ic_dialog_info, false, "Salon"));
        allSensors.add(new Sensor("Courant", "--", android.R.drawable.ic_menu_compass, false, "Salon"));
        allSensors.add(new Sensor("Gaz", "--", android.R.drawable.ic_dialog_alert, false, "Cuisine"));
        allSensors.add(new Sensor("Fumée", "Normal", android.R.drawable.ic_menu_help, false, "Cuisine"));
        allSensors.add(new Sensor("Humidité", "--", android.R.drawable.ic_menu_agenda, false, "Chambre"));
        allSensors.add(new Sensor("Luminosité", "--", android.R.drawable.btn_star_big_on, false, "Chambre"));
        allSensors.add(new Sensor("Humidité Sol", "--", android.R.drawable.ic_menu_today, false, "Jardin"));
        filterByRoom("Salon");
    }

    private void filterByRoom(String room) {
        filteredSensors.clear();
        for (Sensor s : allSensors) {
            if (s.getRoom().equalsIgnoreCase(room)) filteredSensors.add(s);
        }
        if (sensorAdapter != null) sensorAdapter.notifyDataSetChanged();
        
        if (rowLight != null) rowLight.setVisibility((room.equals("Salon") || room.equals("Chambre")) ? View.VISIBLE : View.GONE);
        if (rowFan != null) rowFan.setVisibility(room.equals("Salon") ? View.VISIBLE : View.GONE);
        if (rowAlarm != null) rowAlarm.setVisibility((room.equals("Jardin") || room.equals("Salon")) ? View.VISIBLE : View.GONE);
        if (rowCoffee != null) rowCoffee.setVisibility(room.equals("Cuisine") ? View.VISIBLE : View.GONE);
        if (rowShutters != null) rowShutters.setVisibility(room.equals("Chambre") ? View.VISIBLE : View.GONE);
        if (rowWatering != null) rowWatering.setVisibility(room.equals("Jardin") ? View.VISIBLE : View.GONE);

        updateSpinnerOptions();
    }

    private void updateSpinnerOptions() {
        List<String> options = new ArrayList<>();
        for (Sensor s : filteredSensors) options.add(s.getName());
        if (getContext() != null && sensorSpinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sensorSpinner.setAdapter(adapter);
            sensorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { updateChartDisplay(); }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
        }
    }

    private void setupListeners(View view) {
        Button btnRefresh = view.findViewById(R.id.btn_refresh);
        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> refreshData());

        if (switchLight != null) switchLight.setOnCheckedChangeListener((v, isChecked) -> {
            if (overlay != null) overlay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (iconLight != null) iconLight.setColorFilter(isChecked ? Color.YELLOW : null);
            if (statusLight != null) statusLight.setText(isChecked ? "ALLUMÉ" : "ÉTEINT");
        });
        if (switchFan != null) switchFan.setOnCheckedChangeListener((v, isChecked) -> {
            if (iconFan != null) {
                if (isChecked) {
                    iconFan.setColorFilter(Color.CYAN);
                    RotateAnimation r = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    r.setDuration(1000); r.setRepeatCount(Animation.INFINITE); iconFan.startAnimation(r);
                } else { iconFan.clearAnimation(); iconFan.setColorFilter(null); }
            }
            if (statusFan != null) statusFan.setText(isChecked ? "ACTIF" : "ARRÊT");
        });
        if (switchAlarm != null) switchAlarm.setOnCheckedChangeListener((v, isChecked) -> {
            if (iconAlarm != null) iconAlarm.setColorFilter(isChecked ? Color.RED : null);
            if (statusAlarm != null) statusAlarm.setText(isChecked ? "SÉCURISÉ" : "DÉSACTIVÉ");
            if (dashboardContainer != null) dashboardContainer.setBackgroundColor(isChecked ? Color.parseColor("#30FF0000") : Color.TRANSPARENT);
        });
        if (switchCoffee != null) switchCoffee.setOnCheckedChangeListener((v, c) -> {
            if (view.findViewById(R.id.status_coffee) != null) ((TextView)view.findViewById(R.id.status_coffee)).setText(c ? "EN PRÉPARATION..." : "ÉTEINTE");
        });
    }

    private void refreshData() {
        if (!isAdded()) return;
        int t = 18 + random.nextInt(15), h = 30 + random.nextInt(60), g = 40 + random.nextInt(160);
        double c = 0.5 + random.nextDouble() * 2.0;

        for (Sensor s : allSensors) {
            String n = s.getName().toLowerCase();
            if (n.contains("temp")) { s.setValue(t + "°C"); s.setDanger(t > 32); }
            else if (n.contains("gaz")) { s.setValue(g + " ppm"); s.setDanger(g > 170); }
            else if (n.contains("hum")) { s.setValue(h + "%"); s.setDanger(h > 85); }
            else if (n.contains("cour")) { s.setValue(String.format(Locale.US, "%.1f A", c)); s.setDanger(c > 2.2); }
            else if (n.contains("lum")) { s.setValue((400 + random.nextInt(400)) + " lux"); }
        }

        if (sensorAdapter != null) sensorAdapter.notifyDataSetChanged();
        storeDataPoint(tempEntries, (float)t); storeDataPoint(humEntries, (float)h);
        storeDataPoint(gasEntries, (float)g); storeDataPoint(curEntries, (float)c);
        timeIndex++;
        updateChartDisplay();
        analyzeWithAi(t, h, g, c);
        Toast.makeText(getContext(), "Données actualisées (" + currentRoom + ")", Toast.LENGTH_SHORT).show();
    }

    private void storeDataPoint(List<Entry> entries, float value) {
        entries.add(new Entry(timeIndex, value));
        if (entries.size() > 20) entries.remove(0);
    }

    private void setupChart() {
        if (combinedChart == null) return;
        combinedChart.getDescription().setEnabled(false);
        combinedChart.setBackgroundColor(Color.WHITE);
        combinedChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
    }

    private void updateChartDisplay() {
        if (sensorSpinner == null || combinedChart == null || sensorSpinner.getSelectedItem() == null) return;
        String sel = sensorSpinner.getSelectedItem().toString();
        List<Entry> e; String l; int clr;
        if (sel.contains("Temp")) { e = tempEntries; l = "T (°C)"; clr = Color.RED; }
        else if (sel.contains("Hum")) { e = humEntries; l = "H (%)"; clr = Color.BLUE; }
        else if (sel.contains("Gaz")) { e = gasEntries; l = "G (ppm)"; clr = Color.MAGENTA; }
        else { e = curEntries; l = "C (A)"; clr = Color.BLACK; }
        if (!e.isEmpty()) {
            LineDataSet ds = new LineDataSet(new ArrayList<>(e), l);
            ds.setColor(clr); ds.setCircleColor(clr); ds.setDrawFilled(true);
            ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            combinedChart.setData(new LineData(ds));
            combinedChart.invalidate();
        }
    }

    private void setupGroq() {
        Retrofit r = new Retrofit.Builder().baseUrl("https://api.groq.com/openai/v1/").addConverterFactory(GsonConverterFactory.create()).build();
        groqService = r.create(GroqService.class);
    }

    private void analyzeWithAi(int t, int h, int g, double c) {
        if (groqService == null || !isAdded()) return;
        cardAiInsight.setVisibility(View.VISIBLE);
        List<GroqRequest.Message> msgs = new ArrayList<>();
        msgs.add(new GroqRequest.Message("system", "IA Maison " + currentRoom + ". 1 conseil court."));
        msgs.add(new GroqRequest.Message("user", "Données: T=" + t + "C, H=" + h + "%, G=" + g + "ppm."));
        groqService.getChatCompletion("Bearer " + API_KEY, new GroqRequest("llama-3.1-8b-instant", msgs)).enqueue(new Callback<GroqResponse>() {
            @Override public void onResponse(Call<GroqResponse> call, Response<GroqResponse> r) {
                if (isAdded() && r.isSuccessful() && r.body() != null) tvAiInsight.setText(r.body().choices.get(0).message.content);
            }
            @Override public void onFailure(Call<GroqResponse> c, Throwable t) {}
        });
    }
}
