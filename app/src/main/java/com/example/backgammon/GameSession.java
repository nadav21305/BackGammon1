package com.example.backgammon;

public class GameSession {

    public static long gameId = -1;
    public static int player1Id = -1;
    public static int player2Id = -1;
    public static boolean isAiGame = false; // האם משחק נגד בוט

    public static void reset() {
        gameId = -1;
        player1Id = -1;
        player2Id = -1;
        isAiGame = false;
    }
}
