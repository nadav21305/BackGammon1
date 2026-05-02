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

        if (gamePlay.hasPiecesInBar(isWhite)) {
            return makeMoveFromBar(isWhite);
        }

        if (tryBearOff(isWhite)) {
            return true;
        }

        return makeBestBoardMove(isWhite);
    }

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

        int bestTarget = chooseBestBarTarget(targets, isWhite);
        gamePlay.movePiece(bestTarget);
        return true;
    }

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

    private boolean makeBestBoardMove(boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();

        int bestScore = Integer.MIN_VALUE;
        int bestFrom  = -1;
        int bestTo    = -1;

        for (int point = 1; point <= 24; point++) {
            gamePlay.clearSelection();

            Piece top = getTopPiece(board, point);
            if (top == null || top.isWhite() != isWhite) continue;

            gamePlay.selectPiece(point);
            if (gamePlay.getSelectedPiece() == null) continue;

            List<Integer> targets = new ArrayList<>(gamePlay.getLegalTargets());

            for (int target : targets) {
                int score = evaluateMove(point, target, isWhite);
                if (score > bestScore) {
                    bestScore = score;
                    bestFrom  = point;
                    bestTo    = target;
                }
            }
        }

        if (bestFrom == -1 || bestTo == -1) {
            gamePlay.clearSelection();
            return false;
        }

        gamePlay.clearSelection();
        gamePlay.selectPiece(bestFrom);
        if (gamePlay.getSelectedPiece() == null) {
            gamePlay.clearSelection();
            return false;
        }

        gamePlay.movePiece(bestTo);
        return true;
    }

    private int evaluateMove(int from, int to, boolean isWhite) {
        int score = 0;
        Piece[][] board = gamePlay.getBoard();

        int ownAtFrom    = countOwnPieces(board, from, isWhite);
        int ownAtTarget  = countOwnPieces(board, to, isWhite);
        int ownAfter     = ownAtTarget + 1;
        int enemyAtTo    = gamePlay.getEnemyCount(to);

        if (ownAfter == 2) {
            score += 120;
        } else if (ownAfter >= 3) {
            score += 60;
        }

        if (enemyAtTo == 1) {
            int risk         = estimateExposureRisk(to, isWhite);
            int enemyInBar   = countPiecesInBar(!isWhite);
            int enemyFarFromHome = countEnemyFarFromHome(isWhite);
            int blockingBonus = ownAfter >= 2 ? 40 : 0;

            int hitValue = 80 + blockingBonus;


            if (enemyInBar > 0) hitValue += 20;


            if (enemyFarFromHome > 0) hitValue += 15;


            hitValue -= risk * 12;

            if (hitValue > 0) {
                score += hitValue;
            }
        }

        // עונש על פתיחת מקום שהיה מוגן
        if (ownAtFrom == 2) {

            score -= 80;
        }


        if (ownAfter == 1) {
            int risk = estimateExposureRisk(to, isWhite);
            score -= 70 + risk * 15;
        }

        if (ownAtFrom == 1 && enemyAtTo != 1) {

        }

        int[] path   = isWhite ? whitePath() : blackPath();
        int fromIdx  = getPathIndex(path, from);
        int toIdx    = getPathIndex(path, to);
        score += (toIdx - fromIdx) * 2;


        boolean inHome = isWhite ? (to >= 7 && to <= 12) : (to >= 13 && to <= 18);
        if (inHome) score += 25;


        score += evaluatePrimeBonus(to, isWhite);


        if (isWhite  && gamePlay.hasPiecesInBar(false)) score += 10;
        if (!isWhite && gamePlay.hasPiecesInBar(true))  score += 10;


        if (isEndGame(isWhite)) {
            score += (toIdx - fromIdx) * 5;
        }

        return score;
    }

    private int chooseBestBarTarget(List<Integer> targets, boolean isWhite) {
        int bestTarget = targets.get(0);
        int bestScore  = Integer.MIN_VALUE;

        for (int target : targets) {
            int score = 0;
            Piece[][] board = gamePlay.getBoard();

            int ownCount = countOwnPieces(board, target, isWhite);


            if (ownCount == 1) score += 100;
            else if (ownCount >= 2) score += 50;


            int risk = estimateExposureRisk(target, isWhite);
            score -= risk * 12;


            int[] path = isWhite ? whitePath() : blackPath();
            score += getPathIndex(path, target);

            if (score > bestScore) {
                bestScore  = score;
                bestTarget = target;
            }
        }
        return bestTarget;
    }

    private int estimateExposureRisk(int point, boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();
        int risk = 0;

        for (int dice = 1; dice <= 6; dice++) {
            int enemyFrom = isWhite ? point + dice : point - dice;
            if (enemyFrom < 1 || enemyFrom > 24) continue;
            if (countOwnPieces(board, enemyFrom, !isWhite) >= 1) risk++;
        }
        return risk;
    }

    private int countPiecesInBar(boolean isWhite) {
        Piece[][] bar = gamePlay.getBar();
        int idx = isWhite ? 1 : 0;
        int count = 0;
        for (Piece p : bar[idx]) if (p != null) count++;
        return count;
    }

    private int countEnemyFarFromHome(boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();
        int count = 0;

        for (int i = 1; i <= 24; i++) {
            for (Piece p : board[i - 1]) {
                if (p == null || p.isWhite() == isWhite) continue;
                // יריב רחוק מהבית שלו
                boolean enemyFar = !isWhite
                        ? (i >= 13 && i <= 24)
                        : (i >= 1  && i <= 12);
                if (enemyFar) count++;
            }
        }
        return count;
    }

    private int evaluatePrimeBonus(int newPoint, boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();
        int bonus = 0;

        int consecutive = 0;
        int maxConsecutive = 0;

        for (int offset = -5; offset <= 5; offset++) {
            int p = newPoint + offset;
            if (p < 1 || p > 24) { consecutive = 0; continue; }
            if (countOwnPieces(board, p, isWhite) >= 2) {
                consecutive++;
                maxConsecutive = Math.max(maxConsecutive, consecutive);
            } else {
                consecutive = 0;
            }
        }

        if (maxConsecutive >= 2) bonus += 20;
        if (maxConsecutive >= 3) bonus += 35;
        if (maxConsecutive >= 4) bonus += 60;
        if (maxConsecutive >= 5) bonus += 90;
        if (maxConsecutive >= 6) bonus += 150; // prime מלא

        return bonus;
    }

    private boolean isEndGame(boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();
        for (int i = 1; i <= 24; i++) {
            for (Piece p : board[i - 1]) {
                if (p == null || p.isWhite() != isWhite) continue;
                if (isWhite  && i > 12) return false;
                if (!isWhite && i < 13) return false;
            }
        }
        return true;
    }

    private Piece getTopPiece(Piece[][] board, int point) {
        if (point < 1 || point > 24) return null;
        for (int i = 14; i >= 0; i--)
            if (board[point - 1][i] != null) return board[point - 1][i];
        return null;
    }

    private int countOwnPieces(Piece[][] board, int point, boolean isWhite) {
        if (point < 1 || point > 24) return 0;
        int count = 0;
        for (Piece p : board[point - 1])
            if (p != null && p.isWhite() == isWhite) count++;
        return count;
    }

    private int getPathIndex(int[] path, int point) {
        for (int i = 0; i < path.length; i++)
            if (path[i] == point) return i;
        return 0;
    }

    private int[] whitePath() {
        return new int[]{13,14,15,16,17,18,19,20,21,22,23,24,1,2,3,4,5,6,7,8,9,10,11,12};
    }

    private int[] blackPath() {
        return new int[]{12,11,10,9,8,7,6,5,4,3,2,1,24,23,22,21,20,19,18,17,16,15,14,13};
    }
}