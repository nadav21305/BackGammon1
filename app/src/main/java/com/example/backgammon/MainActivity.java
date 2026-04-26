package com.example.backgammon;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private BackgammonBoardView boardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        boardView = findViewById(R.id.boardView);

        // =========================
        // יצירת משחק אמיתי
        // =========================
        GameSession.player1Id = CurrentUser.userId;
        GameSession.player2Id = -1; // בעתיד מולטיפלייר

        GameSession.gameId = db.createGame(
                GameSession.player1Id,
                GameSession.player2Id,
                -1
        );

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
}