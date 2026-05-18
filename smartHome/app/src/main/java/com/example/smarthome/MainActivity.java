package com.example.smarthome;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

/**
 * Main Activity for SmartHome UI.
 * Handles Navigation Drawer and Fragment switching.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup Drawer Toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.dashboard, R.string.dashboard);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Default Fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_dashboard);
        }

        // --- BOUTON IA DRAGGABLE (MONTER/DESCENDRE) ---
        FloatingActionButton fabAi = findViewById(R.id.fab_ai);
        
        fabAi.setOnTouchListener(new View.OnTouchListener() {
            private float dY;
            private boolean isMoved = false;
            private long downTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // Calculer l'écart entre le doigt et le haut du bouton
                        dY = view.getY() - event.getRawY();
                        downTime = System.currentTimeMillis();
                        isMoved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newY = event.getRawY() + dY;
                        
                        // Limiter le mouvement à la hauteur de l'écran
                        View parent = (View) view.getParent();
                        if (newY >= 0 && newY <= parent.getHeight() - view.getHeight()) {
                            view.setY(newY);
                            isMoved = true;
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Si le bouton n'a pas bougé et que c'était un clic court
                        if (!isMoved || (System.currentTimeMillis() - downTime) < 200) {
                            view.performClick();
                        }
                        return true;
                        
                    default:
                        return false;
                }
            }
        });

        // Action lors du clic sur le bouton IA
        fabAi.setOnClickListener(v -> {
            openAiAssistant();
        });

        // Modern way to handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });

        // --- NOTIFICATIONS ---
        NotificationHelper.createNotificationChannel(this);
        requestNotificationPermission();
    }

    private void openAiAssistant() {
        replaceFragment(new AiAssistantFragment());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.ai_assistant);
        }
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_ai_assistant);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            replaceFragment(new DashboardFragment());
            getSupportActionBar().setTitle(R.string.dashboard);
        } else if (id == R.id.nav_ai_assistant) {
            openAiAssistant();
        } else if (id == R.id.nav_settings) {
            replaceFragment(new SettingsFragment());
            getSupportActionBar().setTitle(R.string.settings);
        } else if (id == R.id.nav_about) {
            showAboutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setMessage(R.string.about_text)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}