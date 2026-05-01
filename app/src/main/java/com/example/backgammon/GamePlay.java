package com.example.backgammon;

import android.content.Context;
import java.util.HashSet;
import java.util.Set;

public class GamePlay {

    private final Piece[][] board = new Piece[24][15];
    private final Piece[][] bar = new Piece[2][15];
    private final Dice dice = new Dice();

    private int[] remainingMoves = new int[0];
    private final Set<Integer> legalTargets = new HashSet<>();
    private final Set<Integer> blockedTargets = new HashSet<>();
    private boolean diceRolled = false;
    private boolean isWhiteTurn = true;

    private int selectedPoint = -1;
    private Piece selectedPiece = null;
    private boolean selectedFromBar = false;

    public Piece getSelectedPiece() { return selectedPiece; }
    public boolean isSelectedFromBar() { return selectedFromBar; }
    public int getSelectedPoint() { return selectedPoint; }

    private int whiteBornOff = 0;
    private int blackBornOff = 0;
    private long gameId = -1;
    private DatabaseHelper db;
    private Context context;
    private boolean gameOver = false;
    private String winner = "";

    public GamePlay() { initPieces(); }


    private void initPieces() {
        for (int i = 0; i < 24; i++)
            for (int j = 0; j < 15; j++)
                board[i][j] = null;

        placePieces(13, 2, true);
        placePieces(7, 5, true);
        placePieces(5, 3, true);
        placePieces(24, 5, true);

        placePieces(12, 2, false);
        placePieces(18, 5, false);
        placePieces(20, 3, false);
        placePieces(1, 5, false);
    }

    private void placePieces(int point, int count, boolean white) {
        for (int i = 0; i < count; i++) {
            Piece p = new Piece();
            p.setColor(white ? 1 : -1);
            board[point - 1][i] = p;
        }
    }
    public void setGameContext(long gameId, DatabaseHelper db, Context context) {
        this.gameId = gameId;
        this.db = db;
        this.context = context;
    }


    public Piece[][] getBoard() { return board; }
    public Piece[][] getBar() { return bar; }
    public Set<Integer> getLegalTargets() { return legalTargets; }
    public Set<Integer> getBlockedTargets() { return blockedTargets; }
    public Dice getDice() { return dice; }
    public boolean isDiceRolled() { return diceRolled; }
    public boolean isWhiteTurn() { return isWhiteTurn; }
    public int getWhiteBornOff() { return whiteBornOff; }
    public int getBlackBornOff() { return blackBornOff; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }

    public int getEnemyCount(int point) {
        int count = 0;
        for (Piece p : board[point - 1])
            if (p != null && p.isWhite() != isWhiteTurn)
                count++;
        return count;
    }


    public void rollDice() {
        dice.roll();
        if (dice.getDie1() == dice.getDie2()) {
            int d = dice.getDie1();
            remainingMoves = new int[]{d, d, d, d};
        } else {
            remainingMoves = new int[]{dice.getDie1(), dice.getDie2()};
        }
        diceRolled = true;
        clearSelection();
        if (!hasAnyMove()) {
            endTurn();
        }
    }

    private int getEnemyCountSim(Piece[][] b, int point) {
        int count = 0;
        for (Piece p : b[point - 1])
            if (p != null && p.isWhite() != isWhiteTurn) count++;
        return count;
    }

    private void removeTopPieceSim(Piece[][] b, int point) {
        for (int i = 14; i >= 0; i--) {
            if (b[point - 1][i] != null) { b[point - 1][i] = null; return; }
        }
    }

    private void placePieceSim(Piece[][] b, int point, Piece p) {
        for (int i = 0; i < 15; i++) {
            if (b[point - 1][i] == null) { b[point - 1][i] = p; return; }
        }
    }


    public void selectPiece(int point) {
        if (!diceRolled) return;
        if (hasPiecesInBar(isWhiteTurn)) return;

        if (point < 1 || point > 24) {
            clearSelection();
            return;
        }

        Piece p = getTopPiece(point);

        // תיקון חשוב — אם לוחצים על אבן לא חוקית, ננקה בחירה קודמת
        if (p == null || p.isWhite() != isWhiteTurn) {
            clearSelection();
            return;
        }

        selectedPiece = p;
        selectedPoint = point;
        selectedFromBar = false;

        calculateLegalMoves();
    }

    public void selectFromBar(boolean white) {
        if (!diceRolled) return;
        if (white != isWhiteTurn) return;

        Piece p = peekFromBar(white);
        if (p == null) return;

        selectedPiece = p;
        selectedPoint = -1;
        selectedFromBar = true;
        calculateLegalMoves();
    }

    public void clearSelection() {
        selectedPiece = null;
        selectedPoint = -1;
        selectedFromBar = false;
        legalTargets.clear();
        blockedTargets.clear();
    }

    public int getBarEntry(boolean white, int move) { return calculateBarEntry(white, move); }
    public int getTargetPoint(int from, int move, boolean white) { return calculateTargetPoint(from, move, white); }
    public int[][] getDiceOrdersForView() { return getDiceOrders(); }


    private void calculateLegalMoves() {
        legalTargets.clear();
        blockedTargets.clear();
        if (selectedPiece == null) return;
        if (hasPiecesInBar(isWhiteTurn) && !selectedFromBar) return;

        if (selectedFromBar) {
            Piece[][] tempBoard = cloneBoard(board);
            for (int move : remainingMoves) {
                if (move == 0) continue;
                int target = calculateBarEntry(selectedPiece.isWhite(), move);
                if (target < 1 || target > 24) continue;
                if (getEnemyCountSim(tempBoard, target) < 2) {
                    legalTargets.add(target);
                } else {
                    blockedTargets.add(target);
                }
            }
            return;
        }

        int[][] orders = getDiceOrders();
        for (int[] order : orders) {
            simulateMoveOrder(order);
        }
    }

    private int[][] getDiceOrders() {
        if (remainingMoves.length == 4) {
            return new int[][]{remainingMoves.clone()};
        }
        return new int[][]{
                {remainingMoves[0], remainingMoves[1]},
                {remainingMoves[1], remainingMoves[0]}
        };
    }

    private Piece[][] cloneBoard(Piece[][] original) {
        Piece[][] copy = new Piece[24][15];
        for (int i = 0; i < 24; i++) {
            for (int j = 0; j < 15; j++) {
                if (original[i][j] != null) {
                    Piece p = new Piece();
                    p.setColor(original[i][j].isWhite() ? 1 : -1);
                    copy[i][j] = p;
                }
            }
        }
        return copy;
    }

    private void simulateMoveOrder(int[] order) {
        int currentPoint = selectedPoint;
        Piece[][] tempBoard = cloneBoard(board);

        for (int move : order) {
            if (move == 0) continue;
            int target = calculateTargetPoint(currentPoint, move, isWhiteTurn);
            if (target < 1 || target > 24) break;

            int enemyCount = getEnemyCountSim(tempBoard, target);
            if (enemyCount >= 2) {
                blockedTargets.add(target);
                break;
            }

            legalTargets.add(target);
            if (enemyCount == 1) removeTopPieceSim(tempBoard, target);
            removeTopPieceSim(tempBoard, currentPoint);
            placePieceSim(tempBoard, target, selectedPiece);
            currentPoint = target;
        }
    }


    public boolean hasPiecesInBar(boolean white) {
        int idx = white ? 1 : 0;
        for (Piece p : bar[idx])
            if (p != null) return true;
        return false;
    }

    public boolean hasPiecesInBarSim(Piece[][] simBar, boolean white) {
        int idx = white ? 1 : 0;
        for (Piece p : simBar[idx])
            if (p != null) return true;
        return false;
    }

    private void checkWinner() {
        if (whiteBornOff == 15) {
            gameOver = true;
            winner = "WHITE WINS";
        } else if (blackBornOff == 15) {
            gameOver = true;
            winner = "BLACK WINS";
        }
    }

    private int calculateBarEntry(boolean white, int move) {
        return white ? 12 + move : 13 - move;
    }

    public Piece getTopPieceAtPoint(int point) {
        if(point < 1 || point > 24) return null;
        Piece[] stack = board[point - 1];
        for (int i = stack.length - 1; i >= 0; i--)
            if (stack[i] != null) return stack[i];
        return null;
    }

    public int calculateTargetPoint(int from, int move, boolean white) {
        if (from < 1 || from > 24) return -1;
        int[] path = white ? whitePath() : blackPath();
        int index = -1;
        for (int i = 0; i < path.length; i++)
            if (path[i] == from) index = i;
        if (index == -1) return -1;
        int targetIndex = index + move;
        if (targetIndex >= path.length) return -1;
        return path[targetIndex];
    }

    private int[] whitePath() { return new int[]{13,14,15,16,17,18,19,20,21,22,23,24,1,2,3,4,5,6,7,8,9,10,11,12}; }
    private int[] blackPath() { return new int[]{12,11,10,9,8,7,6,5,4,3,2,1,24,23,22,21,20,19,18,17,16,15,14,13}; }

    private int calculateDistance(int from, int to) {
        if (from == -1) {
            for (int move = 1; move <= 6; move++)
                if (calculateBarEntry(isWhiteTurn, move) == to) return move;
            return 0;
        }
        int[] path = isWhiteTurn ? whitePath() : blackPath();
        int fromIndex = -1, toIndex = -1;
        for (int i = 0; i < path.length; i++) {
            if (path[i] == from) fromIndex = i;
            if (path[i] == to) toIndex = i;
        }
        if (fromIndex == -1 || toIndex == -1) return 0;
        return toIndex - fromIndex;
    }

    public void movePiece(int target) {
        if (gameOver) return;
        if (!legalTargets.contains(target)) return;
        if (getEnemyCount(target) >= 2) return;

        int distance = getDistanceFromSelection(target);
        if (!useMove(distance)) return;


        if (selectedFromBar) {
            popFromBar(isWhiteTurn());
        } else {
            removeTopPiece(selectedPoint);
        }

        hitEnemy(target);
        placePiece(target, selectedPiece);


        if (db != null && gameId != -1 && context != null) {
            db.saveMove(
                    gameId,
                    CurrentUser.userId,
                    selectedFromBar ? -1 : selectedPoint,
                    target,
                    dice.getDie1(),
                    dice.getDie2()
            );
        }

        clearSelection();


        checkWinner();


        if (!gameOver && (!hasMovesLeft() || !hasAnyMove())) {
            endTurn();
        }
    }

    private boolean useMove(int distance) {
        return useMoveRecursive(distance, remainingMoves);
    }

    public void resetGame() {
        for (int i = 0; i < 24; i++)
            for (int j = 0; j < 15; j++)
                board[i][j] = null;

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 15; j++)
                bar[i][j] = null;

        whiteBornOff = 0;
        blackBornOff = 0;
        gameOver = false;
        winner = "";
        diceRolled = false;
        isWhiteTurn = true;
        selectedPiece = null;
        selectedPoint = -1;
        selectedFromBar = false;
        legalTargets.clear();
        blockedTargets.clear();
        initPieces();
    }

    private boolean useMoveRecursive(int remaining, int[] moves) {
        if (remaining == 0) {
            updateDice();
            return true;
        }
        for (int i = 0; i < moves.length; i++) {
            if (moves[i] == 0 || moves[i] > remaining) continue;
            int m = moves[i];
            moves[i] = 0;
            if (useMoveRecursive(remaining - m, moves)) return true;
            moves[i] = m;
        }
        return false;
    }

    private void updateDice() {
        diceRolled = false;
        for (int m : remainingMoves)
            if (m != 0) diceRolled = true;
    }

    public void bearOffSelected(boolean white) {
        if (white != isWhiteTurn) return;
        if (hasPiecesInBar(white)) return;
        if (selectedPiece == null || selectedPoint == -1) return;
        if (selectedPiece.isWhite() != white) return;
        if (!allPiecesInHome(white)) return;

        int start = white ? 7 : 13;
        int end = white ? 12 : 18;
        if (selectedPoint < start || selectedPoint > end) return;

        int distance = getBearOffDistance(selectedPoint, white);
        int farthest = getFarthestPoint(white);
        if (farthest == -1) return;
        int farthestDistance = getBearOffDistance(farthest, white);

        for (int i = 0; i < remainingMoves.length; i++) {
            if (remainingMoves[i] == distance) {
                remainingMoves[i] = 0;
                updateDice();
                removeTopPiece(selectedPoint);
                if (white) whiteBornOff++; else blackBornOff++;
                checkWinner();
                clearSelection();


                if ((!hasMovesLeft() || !hasAnyMove()) && !gameOver) {
                    endTurn();
                }
                return;
            }
        }

        if (hasExactBearOffMove(white)) return;
        if (selectedPoint != farthest) return;

        for (int i = 0; i < remainingMoves.length; i++) {
            if (remainingMoves[i] > farthestDistance) {
                remainingMoves[i] = 0;
                updateDice();
                removeTopPiece(selectedPoint);
                if (white) whiteBornOff++; else blackBornOff++;
                checkWinner();
                clearSelection();

                if ((!hasMovesLeft() || !hasAnyMove()) && !gameOver) {
                    endTurn();
                }
                return;
            }
        }
    }

    public boolean canBearOffSelected() {
        if (selectedPiece == null || selectedPoint == -1) return false;
        boolean white = selectedPiece.isWhite();
        if (white != isWhiteTurn) return false;
        if (hasPiecesInBar(white)) return false;
        if (!allPiecesInHome(white)) return false;

        int start = white ? 7 : 13;
        int end = white ? 12 : 18;
        if (selectedPoint < start || selectedPoint > end) return false;

        int distance = getBearOffDistance(selectedPoint, white);

        for (int move : remainingMoves)
            if (move == distance) return true;

        int farthest = getFarthestPoint(white);
        if (selectedPoint == farthest && !hasExactBearOffMove(white)) {
            int farthestDistance = getBearOffDistance(farthest, white);
            for (int move : remainingMoves)
                if (move > farthestDistance) return true;
        }

        return false;
    }

    public boolean hasAnyMove() {
        if (hasPiecesInBar(isWhiteTurn)) {
            for (int move : remainingMoves) {
                if (move == 0) continue;
                int target = calculateBarEntry(isWhiteTurn, move);
                if (target >= 1 && target <= 24 && getEnemyCount(target) < 2)
                    return true;
            }
            return false;
        }

        for (int p = 1; p <= 24; p++) {
            Piece top = getTopPiece(p);
            if (top == null || top.isWhite() != isWhiteTurn) continue;
            for (int move : remainingMoves) {
                if (move == 0) continue;
                int target = calculateTargetPoint(p, move, isWhiteTurn);
                if (target >= 1 && target <= 24 && getEnemyCount(target) < 2)
                    return true;
            }
        }

        if (allPiecesInHome(isWhiteTurn)) {
            int farthest = getFarthestPoint(isWhiteTurn);
            for (int p = 1; p <= 24; p++) {
                Piece top = getTopPiece(p);
                if (top == null || top.isWhite() != isWhiteTurn) continue;
                int distance = getBearOffDistance(p, isWhiteTurn);
                for (int move : remainingMoves) {
                    if (move == 0) continue;
                    if (move == distance) return true;
                    if (p == farthest && move > distance) return true;
                }
            }
        }

        return false;
    }

    public void endTurn() {
        if (gameOver) return;
        isWhiteTurn = !isWhiteTurn;
        diceRolled = false;
        clearSelection();
    }

    private boolean hasExactBearOffMove(boolean white) {
        for (int move : remainingMoves) {
            if (move == 0) continue;
            for (int p = 1; p <= 24; p++) {
                Piece top = getTopPiece(p);
                if (top == null || top.isWhite() != white) continue;
                if (getBearOffDistance(p, white) == move) return true;
            }
        }
        return false;
    }

    private boolean allPiecesInHome(boolean white) {
        int start = white ? 7 : 13;
        int end = white ? 12 : 18;
        for (int i = 0; i < 24; i++) {
            int point = i + 1;
            if (point < start || point > end)
                for (Piece p : board[i])
                    if (p != null && p.isWhite() == white) return false;
        }
        return true;
    }

    private int getFarthestPoint(boolean white) {
        if (white) {
            for (int p = 7; p <= 12; p++) {
                Piece top = getTopPiece(p);
                if (top != null && top.isWhite()) {
                    return p;
                }
            }
        } else {
            for (int p = 18; p >= 13; p--) {
                Piece top = getTopPiece(p);
                if (top != null && !top.isWhite()) {
                    return p;
                }
            }
        }
        return -1;
    }

    private int getBearOffDistance(int point, boolean white) {
        return white ? 13 - point : point - 12;
    }


    private void placePiece(int point, Piece p) {
        for (int i = 0; i < board[point - 1].length; i++)
            if (board[point - 1][i] == null) { board[point - 1][i] = p; return; }
    }

    private void removeTopPiece(int point) {
        for (int i = 14; i >= 0; i--)
            if (board[point - 1][i] != null) { board[point - 1][i] = null; return; }
    }

    private void hitEnemy(int point) {
        if (getEnemyCount(point) == 1) {
            Piece enemy = getTopPiece(point);
            removeTopPiece(point);
            addToBar(enemy);
        }
    }

    public int[] getRemainingMoves() { return remainingMoves; }

    public int getDistanceFromSelection(int targetPoint) {
        int from = selectedFromBar ? -1 : selectedPoint;
        return calculateDistance(from, targetPoint);
    }

    private void addToBar(Piece p) {
        int idx = p.isWhite() ? 1 : 0;
        for (int i = 0; i < bar[idx].length; i++)
            if (bar[idx][i] == null) { bar[idx][i] = p; return; }
    }

    private Piece peekFromBar(boolean white) {
        int idx = white ? 1 : 0;
        for (Piece p : bar[idx]) if (p != null) return p;
        return null;
    }

    private void popFromBar(boolean white) {
        int idx = white ? 1 : 0;
        for (int i = 0; i < bar[idx].length; i++)
            if (bar[idx][i] != null) { bar[idx][i] = null; return; }
    }

    private Piece getTopPiece(int point) {
        if (point < 1 || point > 24) return null;
        for (int i = 14; i >= 0; i--)
            if (board[point - 1][i] != null) return board[point - 1][i];
        return null;
    }

    private boolean hasMovesLeft() {
        for (int m : remainingMoves) if (m != 0) return true;
        return false;
    }
}
