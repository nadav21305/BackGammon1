package com.example.backgammon;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!"admin".equals(CurrentUser.role)) {
            finish();
            return;
        }

        db = new DatabaseHelper(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#1a1a2e"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 60, 40, 60);

        TextView title = new TextView(this);
        title.setText("Admin Panel");
        title.setTextSize(32f);
        title.setTextColor(Color.parseColor("#e2b96f"));
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);
        layout.addView(title);

        addSectionTitle(layout, "All Players");

        android.database.Cursor cursor = db.getAllUsersWithStats();
        while (cursor.moveToNext()) {
            String username  = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            String role      = cursor.getString(cursor.getColumnIndexOrThrow("role"));
            int wins         = cursor.getInt(cursor.getColumnIndexOrThrow("wins"));
            int losses       = cursor.getInt(cursor.getColumnIndexOrThrow("losses"));
            int totalGames   = cursor.getInt(cursor.getColumnIndexOrThrow("total_games"));
            addPlayerRow(layout, username, role, wins, losses, totalGames);
        }
        cursor.close();

        addSectionTitle(layout, "Recent Games");

        android.database.Cursor gamesCursor = db.getRecentGamesWithPlayers();
        while (gamesCursor.moveToNext()) {
            String p1     = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("player1"));
            String p2     = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("player2"));
            String winner = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("winner_name"));
            String date   = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("date"));
            addGameRow(layout, p1, p2, winner, date);
        }
        gamesCursor.close();

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addSectionTitle(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(20f);
        tv.setTextColor(Color.parseColor("#e2b96f"));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 40, 0, 16);
        parent.addView(tv);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#e2b96f"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        p.bottomMargin = 20;
        divider.setLayoutParams(p);
        parent.addView(divider);
    }

    private void addPlayerRow(LinearLayout parent, String username, String role,
                              int wins, int losses, int totalGames) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#16213e"));
        card.setPadding(30, 24, 30, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 16;
        card.setLayoutParams(lp);

        TextView nameRow = new TextView(this);
        nameRow.setText((username != null ? username : "Unknown") + "  [" + (role != null ? role : "player") + "]");
        nameRow.setTextSize(16f);
        nameRow.setTextColor("admin".equals(role)
                ? Color.parseColor("#e2b96f")
                : Color.WHITE);
        nameRow.setTypeface(null, Typeface.BOLD);
        card.addView(nameRow);

        TextView stats = new TextView(this);
        stats.setText("W: " + wins + "  L: " + losses + "  Total: " + totalGames);
        stats.setTextSize(14f);
        stats.setTextColor(Color.parseColor("#aaaaaa"));
        stats.setPadding(0, 8, 0, 0);
        card.addView(stats);

        parent.addView(card);
    }

    private void addGameRow(LinearLayout parent, String p1, String p2,
                            String winner, String date) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#16213e"));
        card.setPadding(30, 24, 30, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 16;
        card.setLayoutParams(lp);

        // תיקון: בדיקת null לפני equals
        String p1Name = (p1 != null) ? p1 : "Unknown";
        String p2Name = (p2 != null) ? p2 : "Computer";

        TextView players = new TextView(this);
        players.setText(p1Name + "  vs  " + p2Name);
        players.setTextSize(15f);
        players.setTextColor(Color.WHITE);
        players.setTypeface(null, Typeface.BOLD);
        card.addView(players);

        TextView winnerTv = new TextView(this);
        winnerTv.setText("Winner: " + (winner != null ? winner : "N/A"));
        winnerTv.setTextSize(13f);
        winnerTv.setTextColor(Color.parseColor("#4caf50"));
        winnerTv.setPadding(0, 6, 0, 0);
        card.addView(winnerTv);

        try {
            long ts = Long.parseLong(date);
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            String formatted = sdf.format(new java.util.Date(ts));
            TextView dateTv = new TextView(this);
            dateTv.setText(formatted);
            dateTv.setTextSize(12f);
            dateTv.setTextColor(Color.parseColor("#888888"));
            dateTv.setPadding(0, 4, 0, 0);
            card.addView(dateTv);
        } catch (Exception ignored) {}

        parent.addView(card);
    }
}