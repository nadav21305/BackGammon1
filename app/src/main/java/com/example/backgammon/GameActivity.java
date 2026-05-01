package com.example.backgammon;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText("Welcome to Backgammon!\n(הכנס כאן את לוח המשחק)");
        tv.setTextSize(24f);
        tv.setGravity(Gravity.CENTER);
        setContentView(tv);
    }
}