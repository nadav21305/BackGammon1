package com.example.backgammon;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#1a1a2e"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 60, 40, 60);

        // כותרת
        TextView title = new TextView(this);
        title.setText("My Profile");
        title.setTextSize(30f);
        title.setTextColor(Color.parseColor("#e2b96f"));
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 32);
        layout.addView(title);

        // שם משתמש
        TextView nameTv = new TextView(this);
        nameTv.setText(CurrentUser.username);
        nameTv.setTextSize(22f);
        nameTv.setTextColor(Color.WHITE);
        nameTv.setTypeface(null, Typeface.BOLD);
        nameTv.setGravity(Gravity.CENTER);
        nameTv.setPadding(0, 0, 0, 8);
        layout.addView(nameTv);

        // === כרטיס סטטיסטיקות ===
        Cursor statsCursor = db.getUserStats(CurrentUser.userId);
        if (statsCursor.moveToFirst()) {
            int wins   = statsCursor.getInt(statsCursor.getColumnIndexOrThrow("wins"));
            int losses = statsCursor.getInt(statsCursor.getColumnIndexOrThrow("losses"));
            int total  = statsCursor.getInt(statsCursor.getColumnIndexOrThrow("total_games"));
            float winRate = total > 0 ? (wins * 100f / total) : 0;

            LinearLayout statsCard = new LinearLayout(this);
            statsCard.setOrientation(LinearLayout.HORIZONTAL);
            statsCard.setBackgroundColor(Color.parseColor("#16213e"));
            statsCard.setPadding(30, 30, 30, 30);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.topMargin = 24;
            cardLp.bottomMargin = 24;
            statsCard.setLayoutParams(cardLp);

            addStatBox(statsCard, String.valueOf(wins),   "Wins",   "#4caf50");
            addStatBox(statsCard, String.valueOf(losses), "Losses", "#f44336");
            addStatBox(statsCard, String.format("%.0f%%", winRate), "Win Rate", "#e2b96f");

            layout.addView(statsCard);
        }
        statsCursor.close();

        // === היסטוריית משחקים אישית ===
        addSectionTitle(layout, "My Games");

        Cursor gamesCursor = db.getUserGames(CurrentUser.userId);
        boolean hasGames = false;
        while (gamesCursor.moveToNext()) {
            hasGames = true;
            String opponent = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("opponent"));
            String winner   = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("winner_name"));
            String date     = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("date"));
            boolean iWon    = winner != null && winner.equals(CurrentUser.username);

            addGameRow(layout, opponent, winner, date, iWon);
        }
        gamesCursor.close();

        if (!hasGames) {
            TextView empty = new TextView(this);
            empty.setText("No games yet. Play your first game!");
            empty.setTextColor(Color.parseColor("#888888"));
            empty.setTextSize(15f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 32, 0, 0);
            layout.addView(empty);
        }

        // כפתור Admin — רק אם admin
        if ("admin".equals(CurrentUser.role)) {
            Button adminBtn = new Button(this);
            adminBtn.setText("Admin Panel");
            adminBtn.setBackgroundColor(Color.parseColor("#e2b96f"));
            adminBtn.setTextColor(Color.parseColor("#1a1a2e"));
            adminBtn.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 120);
            btnLp.topMargin = 40;
            adminBtn.setLayoutParams(btnLp);
            adminBtn.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminActivity.class)));
            layout.addView(adminBtn);
        }

        // כפתור Play
        Button playBtn = new Button(this);
        playBtn.setText("Play Game");
        playBtn.setBackgroundColor(Color.parseColor("#4caf50"));
        playBtn.setTextColor(Color.WHITE);
        playBtn.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 120);
        playLp.topMargin = 16;
        playBtn.setLayoutParams(playLp);
        playBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        layout.addView(playBtn);

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addStatBox(LinearLayout parent, String value, String label, String color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        box.setLayoutParams(lp);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextSize(26f);
        val.setTextColor(Color.parseColor(color));
        val.setTypeface(null, Typeface.BOLD);
        val.setGravity(Gravity.CENTER);
        box.addView(val);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(12f);
        lbl.setTextColor(Color.parseColor("#aaaaaa"));
        lbl.setGravity(Gravity.CENTER);
        box.addView(lbl);

        parent.addView(box);
    }

    private void addSectionTitle(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18f);
        tv.setTextColor(Color.parseColor("#e2b96f"));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 32, 0, 12);
        parent.addView(tv);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#333355"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        p.bottomMargin = 16;
        divider.setLayoutParams(p);
        parent.addView(divider);
    }

    private void addGameRow(LinearLayout parent, String opponent,
                            String winner, String date, boolean iWon) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#16213e"));
        card.setPadding(28, 20, 28, 20);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 14;
        card.setLayoutParams(lp);

        // שורת תוצאה
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView vs = new TextView(this);
        vs.setText("vs " + (opponent != null ? opponent : "Computer"));
        vs.setTextSize(15f);
        vs.setTextColor(Color.WHITE);
        vs.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams vsLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        vs.setLayoutParams(vsLp);
        topRow.addView(vs);

        TextView result = new TextView(this);
        result.setText(iWon ? "WIN" : "LOSS");
        result.setTextSize(14f);
        result.setTextColor(iWon ? Color.parseColor("#4caf50") : Color.parseColor("#f44336"));
        result.setTypeface(null, Typeface.BOLD);
        topRow.addView(result);

        card.addView(topRow);

        // תאריך
        try {
            long ts = Long.parseLong(date);
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            TextView dateTv = new TextView(this);
            dateTv.setText(sdf.format(new java.util.Date(ts)));
            dateTv.setTextSize(12f);
            dateTv.setTextColor(Color.parseColor("#888888"));
            dateTv.setPadding(0, 6, 0, 0);
            card.addView(dateTv);
        } catch (Exception ignored) {}

        parent.addView(card);
    }
}
