package com.example.backgammon;

import java.util.ArrayList;
import java.util.List;

public class BotPlayer {

    private final GamePlay gamePlay;

    public BotPlayer(GamePlay gamePlay) {
        this.gamePlay = gamePlay;
    }

    public boolean makeMove() {
        if (!gamePlay.isDiceRolled()) return false;
        if (gamePlay.isGameOver()) return false;

        boolean isWhite = gamePlay.isWhiteTurn();

        // 1. אם יש אבנים בכלא — חייב להוציא קודם
        if (gamePlay.hasPiecesInBar(isWhite)) {
            return makeMoveFromBar(isWhite);
        }

        // 2. תוספת חשובה! הבוט מנסה להוציא אבנים הביתה (Bear Off)
        if (tryBearOff(isWhite)) {
            return true;
        }

        // 3. מחפש את המהלך הטוב ביותר מהלוח
        return makeBestBoardMove(isWhite);
    }

    // ===============================
    // יציאה מה-BAR
    // ===============================
    private boolean makeMoveFromBar(boolean isWhite) {
        gamePlay.selectFromBar(isWhite);

        if (gamePlay.getSelectedPiece() == null) {
            gamePlay.clearSelection();
            return false;
        }

        List<Integer> targets = new ArrayList<>(gamePlay.getLegalTargets());
        if (targets.isEmpty()) {
            gamePlay.clearSelection();
            return false;
        }

        int bestTarget = chooseSafestTarget(targets, isWhite);
        gamePlay.movePiece(bestTarget);
        return true;
    }

    // ===============================
    // הוצאת אבנים (Bear Off) לבוט
    // ===============================
    private boolean tryBearOff(boolean isWhite) {
        for (int point = 1; point <= 24; point++) {
            gamePlay.selectPiece(point);
            if (gamePlay.getSelectedPiece() != null && gamePlay.canBearOffSelected()) {
                gamePlay.bearOffSelected(isWhite);
                return true;
            }
        }
        gamePlay.clearSelection();
        return false;
    }

    // ===============================
    // מהלך מהלוח
    // ===============================
    private boolean makeBestBoardMove(boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();

        int bestScore = Integer.MIN_VALUE;
        int bestFrom = -1;
        int bestTo = -1;

        for (int point = 1; point <= 24; point++) {
            Piece top = getTopPiece(board, point);
            if (top == null || top.isWhite() != isWhite) continue;

            gamePlay.selectPiece(point);
            if (gamePlay.getSelectedPiece() == null) continue;

            List<Integer> targets = new ArrayList<>(gamePlay.getLegalTargets());

            for (int target : targets) {
                int score = evaluateMove(point, target, isWhite);
                if (score > bestScore) {
                    bestScore = score;
                    bestFrom = point;
                    bestTo = target;
                }
            }

            // זה מה שגרם לקריסה - עכשיו זה משתמש בפונקציה הציבורית הבטוחה!
            gamePlay.clearSelection();
        }

        if (bestFrom == -1 || bestTo == -1) return false;

        gamePlay.selectPiece(bestFrom);
        if (gamePlay.getSelectedPiece() == null) return false;
        gamePlay.movePiece(bestTo);
        return true;
    }

    // ===============================
    // פונקציית הערכה — לב ה-AI
    // ===============================
    private int evaluateMove(int from, int to, boolean isWhite) {
        int score = 0;
        Piece[][] board = gamePlay.getBoard();

        int enemyCount = gamePlay.getEnemyCount(to);
        if (enemyCount == 1) score += 50;

        if (isWhite) {
            if (to >= 7 && to <= 12) score += 20;
        } else {
            if (to >= 13 && to <= 18) score += 20;
        }

        int ownCount = countOwnPieces(board, to, isWhite);
        if (ownCount == 1) score += 30;

        int fromCount = countOwnPieces(board, from, isWhite);
        if (fromCount == 1) score -= 25;

        int[] path = isWhite ? whitePath() : blackPath();
        int fromIdx = getPathIndex(path, from);
        int toIdx = getPathIndex(path, to);
        score += (toIdx - fromIdx) * 2;

        if (isWhite && gamePlay.hasPiecesInBar(false)) score += 5;
        if (!isWhite && gamePlay.hasPiecesInBar(true)) score += 5;

        return score;
    }

    private int chooseSafestTarget(List<Integer> targets, boolean isWhite) {
        int bestTarget = targets.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (int target : targets) {
            int score = 0;
            Piece[][] board = gamePlay.getBoard();

            int ownCount = countOwnPieces(board, target, isWhite);
            score += ownCount * 20;

            int[] path = isWhite ? whitePath() : blackPath();
            score += getPathIndex(path, target);

            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }
        return bestTarget;
    }

    private Piece getTopPiece(Piece[][] board, int point) {
        if(point < 1 || point > 24) return null;
        for (int i = 14; i >= 0; i--)
            if (board[point - 1][i] != null) return board[point - 1][i];
        return null;
    }

    private int countOwnPieces(Piece[][] board, int point, boolean isWhite) {
        int count = 0;
        for (Piece p : board[point - 1])
            if (p != null && p.isWhite() == isWhite) count++;
        return count;
    }

    private int getPathIndex(int[] path, int point) {
        for (int i = 0; i < path.length; i++)
            if (path[i] == point) return i;
        return 0; // מניעת החזרת -1 שעושה בעיות מתמטיות
    }

    private int[] whitePath() {
        return new int[]{13,14,15,16,17,18,19,20,21,22,23,24,1,2,3,4,5,6,7,8,9,10,11,12};
    }

    private int[] blackPath() {
        return new int[]{12,11,10,9,8,7,6,5,4,3,2,1,24,23,22,21,20,19,18,17,16,15,14,13};
    }
}
