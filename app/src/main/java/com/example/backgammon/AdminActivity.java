package com.example.backgammon;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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
        title.setTextSize(30f);
        title.setTextColor(Color.parseColor("#e2b96f"));
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);
        layout.addView(title);


        addSectionTitle(layout, "All Users");
        addTableHeader(layout, new String[]{"Username", "Role", "Wins", "Losses", "Games"});

        Cursor usersCursor = db.getAllUsersWithStats();
        while (usersCursor.moveToNext()) {
            String username   = usersCursor.getString(usersCursor.getColumnIndexOrThrow("username"));
            String role       = usersCursor.getString(usersCursor.getColumnIndexOrThrow("role"));
            int wins          = usersCursor.getInt(usersCursor.getColumnIndexOrThrow("wins"));
            int losses        = usersCursor.getInt(usersCursor.getColumnIndexOrThrow("losses"));
            int totalGames    = usersCursor.getInt(usersCursor.getColumnIndexOrThrow("total_games"));

            addTableRow(layout, new String[]{
                    username, role,
                    String.valueOf(wins),
                    String.valueOf(losses),
                    String.valueOf(totalGames)
            }, "admin".equals(role));
        }
        usersCursor.close();


        addSectionTitle(layout, "Recent Games");
        addTableHeader(layout, new String[]{"Player 1", "Player 2", "Winner", "Date"});

        Cursor gamesCursor = db.getRecentGamesWithPlayers();
        while (gamesCursor.moveToNext()) {
            String p1     = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("player1"));
            String p2     = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("player2"));
            String winner = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("winner_name"));
            String date   = gamesCursor.getString(gamesCursor.getColumnIndexOrThrow("date"));


            String dateFormatted = date;
            try {
                long ts = Long.parseLong(date);
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault());
                dateFormatted = sdf.format(new java.util.Date(ts));
            } catch (Exception ignored) {}

            addTableRow(layout, new String[]{
                    p1 != null ? p1 : "?",
                    p2 != null ? p2 : "AI",
                    winner != null ? winner : "?",
                    dateFormatted
            }, false);
        }
        gamesCursor.close();

        scroll.addView(layout);
        setContentView(scroll);
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

    private void addTableHeader(LinearLayout parent, String[] cols) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#0f3460"));
        row.setPadding(16, 14, 16, 14);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = 2;
        row.setLayoutParams(rowLp);

        for (String col : cols) {
            TextView tv = new TextView(this);
            tv.setText(col);
            tv.setTextSize(13f);
            tv.setTextColor(Color.parseColor("#e2b96f"));
            tv.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            row.addView(tv);
        }
        parent.addView(row);
    }

    private void addTableRow(LinearLayout parent, String[] cols, boolean highlight) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(highlight
                ? Color.parseColor("#1e3a5f")
                : Color.parseColor("#16213e"));
        row.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = 2;
        row.setLayoutParams(rowLp);

        for (String col : cols) {
            TextView tv = new TextView(this);
            tv.setText(col != null ? col : "-");
            tv.setTextSize(13f);
            tv.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            row.addView(tv);
        }
        parent.addView(row);
    }
}
