package com.example.backgammon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "backgammon.db";
    private static final int DB_VERSION = 5; // עדכון גרסה

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT UNIQUE," +
                        "password TEXT," +
                        "role TEXT DEFAULT 'player'," +
                        "wins INTEGER DEFAULT 0," +
                        "losses INTEGER DEFAULT 0)"
        );

        db.execSQL(
                "CREATE TABLE games (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "player1_id INTEGER," +
                        "player2_id INTEGER," +
                        "winner_id INTEGER," +
                        "date TEXT)"
        );

        db.execSQL(
                "CREATE TABLE moves (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "game_id INTEGER," +
                        "player_id INTEGER," +
                        "from_point INTEGER," +
                        "to_point INTEGER," +
                        "dice1 INTEGER," +
                        "dice2 INTEGER)"
        );

        // יצירת admin ברירת מחדל
        db.execSQL(
                "INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'admin')"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS games");
        db.execSQL("DROP TABLE IF EXISTS moves");
        onCreate(db);
    }

    // =========================
    // REGISTER
    // =========================
    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("password", password);
        v.put("role", "player");
        long result = db.insert("users", null, v);
        return result != -1;
    }

    // =========================
    // LOGIN
    // =========================
    public boolean loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM users WHERE username=? AND password=?",
                new String[]{username, password}
        );
        boolean ok = c.moveToFirst();
        c.close();
        return ok;
    }

    public int getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM users WHERE username=?",
                new String[]{username}
        );
        if (c.moveToFirst()) {
            int id = c.getInt(0);
            c.close();
            return id;
        }
        c.close();
        return -1;
    }

    public String getUserRole(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT role FROM users WHERE username=?",
                new String[]{username}
        );
        if (c.moveToFirst()) {
            String role = c.getString(0);
            c.close();
            return role;
        }
        c.close();
        return "player";
    }

    // =========================
    // WIN / LOSS
    // =========================
    public void addWin(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE users SET wins = wins + 1 WHERE id = ?", new Object[]{userId});
    }

    public void addLoss(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE users SET losses = losses + 1 WHERE id = ?", new Object[]{userId});
    }

    public void updateGameWinner(long gameId, int winnerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(
                "UPDATE games SET winner_id = ? WHERE id = ?",
                new Object[]{winnerId, gameId}
        );
    }

    // =========================
    // GAME
    // =========================
    public long createGame(int p1, int p2, int winner) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("player1_id", p1);
        v.put("player2_id", p2);
        v.put("winner_id", winner);
        v.put("date", String.valueOf(System.currentTimeMillis()));
        return db.insert("games", null, v);
    }

    // =========================
    // MOVE
    // =========================
    public void saveMove(long gameId, int playerId, int from, int to, int d1, int d2) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("game_id", gameId);
        v.put("player_id", playerId);
        v.put("from_point", from);
        v.put("to_point", to);
        v.put("dice1", d1);
        v.put("dice2", d2);
        db.insert("moves", null, v);
    }

    // =========================
    // JOIN QUERIES — Admin
    // =========================

    // כל המשתמשים + מספר משחקים (JOIN בין users ל-games)
    public Cursor getAllUsersWithStats() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT u.username, u.role, u.wins, u.losses, " +
                        "COUNT(g.id) AS total_games " +
                        "FROM users u " +
                        "LEFT JOIN games g ON (g.player1_id = u.id OR g.player2_id = u.id) " +
                        "GROUP BY u.id " +
                        "ORDER BY u.wins DESC",
                null
        );
    }

    // משחקים אחרונים עם שמות שחקנים ומנצח (JOIN משולש)
    public Cursor getRecentGamesWithPlayers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT p1.username AS player1, " +
                        "p2.username AS player2, " +
                        "w.username AS winner_name, " +
                        "g.date " +
                        "FROM games g " +
                        "LEFT JOIN users p1 ON g.player1_id = p1.id " +
                        "LEFT JOIN users p2 ON g.player2_id = p2.id " +
                        "LEFT JOIN users w  ON g.winner_id  = w.id " +
                        "ORDER BY g.date DESC " +
                        "LIMIT 20",
                null
        );
    }

    // =========================
    // JOIN QUERIES — Profile
    // =========================

    // סטטיסטיקות אישיות + מספר משחקים
    public Cursor getUserStats(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT u.wins, u.losses, " +
                        "COUNT(g.id) AS total_games " +
                        "FROM users u " +
                        "LEFT JOIN games g ON (g.player1_id = u.id OR g.player2_id = u.id) " +
                        "WHERE u.id = ?",
                new String[]{String.valueOf(userId)}
        );
    }

    // היסטוריית משחקים אישית עם שם היריב והמנצח
    public Cursor getUserGames(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT " +
                        "CASE WHEN g.player1_id = ? THEN p2.username ELSE p1.username END AS opponent, " +
                        "w.username AS winner_name, " +
                        "g.date " +
                        "FROM games g " +
                        "LEFT JOIN users p1 ON g.player1_id = p1.id " +
                        "LEFT JOIN users p2 ON g.player2_id = p2.id " +
                        "LEFT JOIN users w  ON g.winner_id  = w.id " +
                        "WHERE g.player1_id = ? OR g.player2_id = ? " +
                        "ORDER BY g.date DESC " +
                        "LIMIT 30",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId)
                }
        );
    }
}
