package com.example.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AutoClickService extends AccessibilityService {

    public static AutoClickService instance;

    private WindowManager wm;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private LinearLayout panel;
    private WindowManager.LayoutParams panelParams;
    private TextView playBtn, info;

    private final List<TextView> targets = new ArrayList<>();
    private final List<WindowManager.LayoutParams> targetParams = new ArrayList<>();

    private int intervalMs = 500, repeatCount = 0, targetCount = 1;
    private boolean running = false;
    private int stepIndex = 0, roundsDone = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        reloadSettings();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { stopClicking(); }

    @Override
    public void onDestroy() {
        hideOverlay();
        instance = null;
        super.onDestroy();
    }

    public void reloadSettings() {
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        intervalMs = p.getInt(MainActivity.KEY_INTERVAL, 500);
        repeatCount = p.getInt(MainActivity.KEY_COUNT, 0);
        int newTargets = p.getInt(MainActivity.KEY_TARGETS, 1);
        if (panel != null && newTargets != targetCount) {
            targetCount = newTargets;
            hideOverlay();
            showOverlay();
        } else {
            targetCount = newTargets;
        }
    }

    // ---------------- Overlay ----------------

    public void showOverlay() {
        if (panel != null) return;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;

        // Targets
        int size = dp(56);
        for (int i = 0; i < targetCount; i++) {
            TextView t = new TextView(this);
            t.setText(String.valueOf(i + 1));
            t.setTextColor(Color.WHITE);
            t.setTextSize(18);
            t.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Color.parseColor("#88E53935"));
            bg.setStroke(dp(3), Color.WHITE);
            t.setBackground(bg);

            WindowManager.LayoutParams lp = baseParams(size, size);
            lp.x = screenW / 2 - size / 2 + (i % 3 - 1) * dp(70);
            lp.y = screenH / 2 - size / 2 + (i / 3) * dp(70);
            makeDraggable(t, t, lp);
            wm.addView(t, lp);
            targets.add(t);
            targetParams.add(lp);
        }

        // Control panel
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(Color.parseColor("#E61A237E"));
        pbg.setCornerRadius(dp(14));
        panel.setBackground(pbg);

        TextView handle = panelButton("⠿");
        playBtn = panelButton("▶");
        TextView close = panelButton("✕");
        info = new TextView(this);
        info.setTextColor(Color.WHITE);
        info.setTextSize(12);
        info.setPadding(dp(6), 0, dp(6), 0);
        info.setText(intervalMs + "ms");

        panel.addView(handle);
        panel.addView(playBtn);
        panel.addView(info);
        panel.addView(close);

        panelParams = baseParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        panelParams.x = dp(16);
        panelParams.y = dp(120);
        makeDraggable(handle, panel, panelParams);

        playBtn.setOnClickListener(v -> { if (running) stopClicking(); else startClicking(); });
        close.setOnClickListener(v -> hideOverlay());

        wm.addView(panel, panelParams);
    }

    public void hideOverlay() {
        stopClicking();
        for (View t : targets) safeRemove(t);
        targets.clear();
        targetParams.clear();
        if (panel != null) safeRemove(panel);
        panel = null;
    }

    private void safeRemove(View v) {
        try { wm.removeView(v); } catch (Exception ignored) { }
    }

    private WindowManager.LayoutParams baseParams(int w, int h) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        return lp;
    }

    private TextView panelButton(String text) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(20);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(dp(44));
        b.setMinHeight(dp(44));
        return b;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void makeDraggable(View touchView, View moveView, WindowManager.LayoutParams lp) {
        touchView.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (running && moveView != panel) return false;
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX(); downY = e.getRawY();
                        startX = lp.x; startY = lp.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        lp.x = startX + Math.round(e.getRawX() - downX);
                        lp.y = startY + Math.round(e.getRawY() - downY);
                        try { wm.updateViewLayout(moveView, lp); } catch (Exception ignored) { }
                        return true;
                }
                return false;
            }
        });
    }

    private void setTargetsTouchable(boolean touchable) {
        for (int i = 0; i < targets.size(); i++) {
            WindowManager.LayoutParams lp = targetParams.get(i);
            if (touchable) lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            else lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            try { wm.updateViewLayout(targets.get(i), lp); } catch (Exception ignored) { }
        }
    }

    // ---------------- Clicking ----------------

    private void startClicking() {
        if (targets.isEmpty()) return;
        running = true;
        stepIndex = 0;
        roundsDone = 0;
        playBtn.setText("■");
        setTargetsTouchable(false);
        handler.postDelayed(clickStep, 300);
    }

    private void stopClicking() {
        running = false;
        handler.removeCallbacks(clickStep);
        if (playBtn != null) playBtn.setText("▶");
        if (info != null) info.setText(intervalMs + "ms");
        setTargetsTouchable(true);
    }

    private final Runnable clickStep = new Runnable() {
        @Override
        public void run() {
            if (!running || targets.isEmpty()) return;
            View t = targets.get(stepIndex);
            int[] loc = new int[2];
            t.getLocationOnScreen(loc);
            float x = loc[0] + t.getWidth() / 2f;
            float y = loc[1] + t.getHeight() / 2f;

            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, 40))
                    .build();

            dispatchGesture(gesture, new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription g) { next(); }
                @Override public void onCancelled(GestureDescription g) { next(); }
            }, null);
        }
    };

    private void next() {
        if (!running) return;
        stepIndex++;
        if (stepIndex >= targets.size()) {
            stepIndex = 0;
            roundsDone++;
            if (info != null) info.setText("รอบ " + roundsDone);
            if (repeatCount > 0 && roundsDone >= repeatCount) {
                stopClicking();
                return;
            }
        }
        handler.postDelayed(clickStep, intervalMs);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
