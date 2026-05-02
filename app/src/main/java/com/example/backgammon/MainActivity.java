package com.example.backgammon;

import android.os.Bundle;
import android.media.MediaPlayer;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private BackgammonBoardView boardView;
    private MediaPlayer mediaPlayer; // הוספתי את ה-MediaPlayer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // יצירת ה-MediaPlayer והפעלתו
        mediaPlayer = MediaPlayer.create(this, R.raw.videoplayback);
        // ודא שיש קובץ בשם music.mp3 בתיקיית raw
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true); // לנגן בלולאה
            mediaPlayer.start(); // להתחיל לנגן
        }

        db = new DatabaseHelper(this);
        boardView = findViewById(R.id.boardView);

        GameSession.player1Id = CurrentUser.userId;
        GameSession.player2Id = -1;

        long gameId = db.createGame(
                GameSession.player1Id,
                GameSession.player2Id,
                -1
        );

        GameSession.gameId = gameId;

        boardView.setGameSession(GameSession.gameId, db);

        boardView.setGameListener(winner -> {
            int userId = CurrentUser.userId;
            boolean whiteWon = winner.contains("WHITE");

            if (CurrentUser.isWhitePlayer == whiteWon) {
                db.addWin(userId);
            } else {
                db.addLoss(userId);
            }

            db.updateGameWinner(GameSession.gameId, userId);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // שחרור ה-MediaPlayer כאשר הפעילות נסגרת
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}