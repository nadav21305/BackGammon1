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

        int bestTarget = chooseSafestTarget(targets, isWhite);
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
        int bestFrom = -1;
        int bestTo = -1;

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
                    bestFrom = point;
                    bestTo = target;
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

        int enemyCount = gamePlay.getEnemyCount(to);


        if (enemyCount == 1) {
            score += 80;

            if (isPositionDangerous(to, isWhite)) {
                score -= 40;
            }
        }


        int ownAfter = countOwnPieces(board, to, isWhite) + 1;
        if (ownAfter >= 2) score += 40;


        int fromCount = countOwnPieces(board, from, isWhite);
        if (fromCount == 1) score -= 60;

        if (ownAfter == 1) score -= 40;


        int[] path = isWhite ? whitePath() : blackPath();
        int fromIdx = getPathIndex(path, from);
        int toIdx = getPathIndex(path, to);
        score += (toIdx - fromIdx) * 3;


        if (isEndGame(isWhite)) {
            score += (toIdx - fromIdx) * 5;
        }


        if (isWhite && gamePlay.hasPiecesInBar(false)) score += 15;
        if (!isWhite && gamePlay.hasPiecesInBar(true)) score += 15;

        // 🧠 RISK
        if (isPositionDangerous(to, isWhite)) score -= 50;


        score -= evaluateFutureRisk(to, isWhite);


        int neighbors =
                countOwnPieces(board, to - 1, isWhite) +
                        countOwnPieces(board, to + 1, isWhite);
        score += neighbors * 10;


        if (hasStrongBlock(to, isWhite)) score += 60;

        return score;
    }


    private int evaluateFutureRisk(int to, boolean isWhite) {
        int risk = 0;

        for (int dice = 1; dice <= 6; dice++) {
            int enemyFrom = isWhite ? to + dice : to - dice;

            if (enemyFrom >= 1 && enemyFrom <= 24) {
                int enemyCount = countOwnPieces(gamePlay.getBoard(), enemyFrom, !isWhite);
                if (enemyCount > 0) {
                    risk += 15;
                }
            }
        }
        return risk;
    }


    private boolean hasStrongBlock(int point, boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();

        int count = 0;
        for (int i = point; i <= point + 2 && i <= 24; i++) {
            if (countOwnPieces(board, i, isWhite) >= 2) {
                count++;
            }
        }
        return count >= 2;
    }

    private boolean isEndGame(boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();

        for (int i = 1; i <= 24; i++) {
            for (Piece p : board[i - 1]) {
                if (p != null && p.isWhite() == isWhite) {
                    if (isWhite && i > 6) return false;
                    if (!isWhite && i < 19) return false;
                }
            }
        }
        return true;
    }

    private boolean isPositionDangerous(int point, boolean isWhite) {
        Piece[][] board = gamePlay.getBoard();

        for (int i = 1; i <= 24; i++) {
            int enemyCount = countOwnPieces(board, i, !isWhite);
            if (enemyCount == 0) continue;

            int distance = Math.abs(i - point);
            if (distance >= 1 && distance <= 6) return true;
        }
        return false;
    }

    private int chooseSafestTarget(List<Integer> targets, boolean isWhite) {
        int bestTarget = targets.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (int target : targets) {
            int score = evaluateMove(target, target, isWhite);

            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }
        return bestTarget;
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