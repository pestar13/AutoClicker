package com.example.autoclicker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String PREFS = "autoclicker";
    public static final String KEY_INTERVAL = "interval_ms";
    public static final String KEY_COUNT = "repeat_count";
    public static final String KEY_TARGETS = "target_count";

    private TextView status;
    private EditText intervalInput, countInput, targetsInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        int pad = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText("Auto Clicker");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#1A237E"));
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, dp(8), 0, dp(16));
        root.addView(status);

        intervalInput = addField(root, "ระยะห่างแต่ละคลิก (มิลลิวินาที)",
                String.valueOf(p.getInt(KEY_INTERVAL, 500)));
        countInput = addField(root, "จำนวนรอบ (0 = ไม่จำกัด)",
                String.valueOf(p.getInt(KEY_COUNT, 0)));
        targetsInput = addField(root, "จำนวนจุดเป้าหมาย (1–10)",
                String.valueOf(p.getInt(KEY_TARGETS, 1)));

        Button save = addButton(root, "บันทึกการตั้งค่า");
        save.setOnClickListener(v -> saveSettings(true));

        Button access = addButton(root, "1) เปิดสิทธิ์ Accessibility");
        access.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Button show = addButton(root, "2) แสดงแผงควบคุมลอย");
        show.setOnClickListener(v -> {
            saveSettings(false);
            AutoClickService s = AutoClickService.instance;
            if (s == null) {
                Toast.makeText(this, "กรุณาเปิดสิทธิ์ Accessibility ก่อน", Toast.LENGTH_LONG).show();
                return;
            }
            s.showOverlay();
            moveTaskToBack(true);
        });

        Button hide = addButton(root, "ซ่อนแผงควบคุม / หยุด");
        hide.setOnClickListener(v -> {
            if (AutoClickService.instance != null) AutoClickService.instance.hideOverlay();
        });

        TextView help = new TextView(this);
        help.setTextSize(14);
        help.setPadding(0, dp(20), 0, 0);
        help.setText("วิธีใช้:\n"
                + "• ลากวงกลมเป้าหมาย (ตัวเลข) ไปวางจุดที่ต้องการให้คลิก\n"
                + "• ลากแผงควบคุมเพื่อย้ายตำแหน่ง\n"
                + "• ▶ เริ่ม  ■ หยุด  ✕ ปิดแผง\n"
                + "• ถ้ามีหลายจุด แอพจะคลิกเรียงตามหมายเลข 1, 2, 3…");
        root.addView(help);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = AutoClickService.instance != null;
        status.setText(on ? "สถานะ: ✅ เปิดสิทธิ์ Accessibility แล้ว"
                : "สถานะ: ❌ ยังไม่ได้เปิดสิทธิ์ Accessibility\nกดปุ่ม 1) แล้วเลือก \"Auto Clicker\" → เปิดใช้งาน");
        status.setTextColor(on ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
    }

    private void saveSettings(boolean toast) {
        int interval = Math.max(10, parse(intervalInput, 500));
        int count = Math.max(0, parse(countInput, 0));
        int targets = Math.min(10, Math.max(1, parse(targetsInput, 1)));
        intervalInput.setText(String.valueOf(interval));
        targetsInput.setText(String.valueOf(targets));
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_INTERVAL, interval)
                .putInt(KEY_COUNT, count)
                .putInt(KEY_TARGETS, targets)
                .apply();
        if (AutoClickService.instance != null) AutoClickService.instance.reloadSettings();
        if (toast) Toast.makeText(this, "บันทึกแล้ว", Toast.LENGTH_SHORT).show();
    }

    private int parse(EditText e, int def) {
        try { return Integer.parseInt(e.getText().toString().trim()); }
        catch (Exception ex) { return def; }
    }

    private EditText addField(LinearLayout root, String label, String value) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(14);
        t.setPadding(0, dp(8), 0, 0);
        root.addView(t);
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(value);
        root.addView(e);
        return e;
    }

    private Button addButton(LinearLayout root, String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        root.addView(b, lp);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
