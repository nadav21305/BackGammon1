package com.example.backgammon;

import java.util.Set;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class BackgammonBoardView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GamePlay gamePlay = new GamePlay();
    private BotPlayer botPlayer;

    private float triangleWidth;
    private float barWidth;
    private float pieceRadius;

    private float topBaseY;
    private float bottomBaseY;
    private int highlightedPiece = -1;

    private float horizontalOffset = 56f;
    private float rightBoardOffset = 30f;
    private float verticalSpacingFactor = 1.7f;
    private float targetCircleFactor = 1.17f;

    private boolean diceAnimating = false;
    private long diceAnimationStart = 0;
    private int animDie1 = 1;
    private int animDie2 = 1;
    private boolean diceLeftSide = true;

    private boolean showTurnBanner = true;
    private long turnBannerStart = 0;
    private static final long TURN_BANNER_DURATION = 1500;

    private long gameId;
    private DatabaseHelper db;

    // מסך סיום — כפתורים
    private RectF btnNewGame;
    private RectF btnHome;
    private boolean showEndButtons = false;

    public interface GameListener {
        void onGameOver(String winner);
    }

    private GameListener listener;
    private boolean gameOverSent = false;

    public void setGameListener(GameListener listener) {
        this.listener = listener;
    }

    public BackgammonBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        turnBannerStart = System.currentTimeMillis();
    }

    public void setGameSession(long gameId, DatabaseHelper db) {
        this.gameId = gameId;
        this.db = db;
        gamePlay.setGameContext(gameId, db, getContext());


        if (GameSession.isAiGame) {
            botPlayer = new BotPlayer(gamePlay);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        barWidth = w * 0.015f;
        triangleWidth = (w - barWidth) / 12f * 0.867f;
        pieceRadius = triangleWidth * 0.45f;
        topBaseY = pieceRadius * 5f;
        bottomBaseY = h - pieceRadius * 5f;
        updateAllPiecePositions();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        drawLegalTargets(c);

        Piece[][] board = gamePlay.getBoard();
        for (Piece[] col : board)
            for (Piece p : col)
                if (p != null) drawPiece(c, p);

        drawBarPieces(c);
        drawBearOffButtons(c);
        drawDiceButton(c);
        drawDice(c);

        if (gamePlay.isGameOver()) {
            drawWinnerScreen(c);

            if (!gameOverSent) {
                gameOverSent = true;
                showEndButtons = true;

                if (listener != null) {
                    listener.onGameOver(gamePlay.getWinner());
                }

                invalidate();
            }


        } else {
            drawTurnBanner(c);
        }
    }

    private void drawWinnerScreen(Canvas c) {
        String winnerText = gamePlay.getWinner();
        boolean whiteWon = winnerText.contains("WHITE");


        paint.setColor(Color.argb(200, 0, 0, 0));
        paint.setStyle(Paint.Style.FILL);
        c.drawRect(0, 0, getWidth(), getHeight(), paint);


        paint.setColor(whiteWon ? Color.WHITE : Color.BLACK);
        paint.setTextSize(110f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setShadowLayer(
                12f,
                4f,
                4f,
                whiteWon ? Color.DKGRAY : Color.LTGRAY
        );

        c.drawText(
                winnerText,
                getWidth() / 2f,
                getHeight() / 2f - 80f,
                paint
        );

        paint.setShadowLayer(0, 0, 0, 0);
        paint.setFakeBoldText(false);

        if (!showEndButtons) return;

        float btnW = 280f;
        float btnH = 90f;
        float gap = 30f;

        float totalW = btnW * 2 + gap;
        float startX = getWidth() / 2f - totalW / 2f;
        float btnY = getHeight() / 2f + 30f;

        // NEW GAME
        btnNewGame = new RectF(
                startX,
                btnY,
                startX + btnW,
                btnY + btnH
        );

        paint.setColor(Color.parseColor("#4CAF50"));
        paint.setStyle(Paint.Style.FILL);
        c.drawRoundRect(btnNewGame, 20f, 20f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(36f);
        paint.setTextAlign(Paint.Align.CENTER);

        c.drawText(
                "NEW GAME",
                btnNewGame.centerX(),
                btnNewGame.centerY() + 12f,
                paint
        );

        // HOME
        float homeX = startX + btnW + gap;

        btnHome = new RectF(
                homeX,
                btnY,
                homeX + btnW,
                btnY + btnH
        );

        paint.setColor(Color.parseColor("#E2B96F"));
        c.drawRoundRect(btnHome, 20f, 20f, paint);

        paint.setColor(Color.BLACK);

        c.drawText(
                "HOME",
                btnHome.centerX(),
                btnHome.centerY() + 12f,
                paint
        );
    }

    private void startTurnBanner() {
        showTurnBanner = true;
        turnBannerStart = System.currentTimeMillis();
        invalidate();
    }


    private void drawTurnBanner(Canvas c) {
        if (!showTurnBanner) return;
        long elapsed = System.currentTimeMillis() - turnBannerStart;
        if (elapsed > TURN_BANNER_DURATION) { showTurnBanner = false; return; }

        float progress = 1f - (elapsed / (float) TURN_BANNER_DURATION);
        int alpha = (int) (255 * progress);

        String text = gamePlay.isWhiteTurn() ? "WHITE TURN" : "BLACK TURN";
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(75f);
        paint.setFakeBoldText(true);
        paint.setColor(gamePlay.isWhiteTurn() ? Color.WHITE : Color.BLACK);
        paint.setAlpha(alpha);
        c.drawText(text, getWidth() / 2f, getHeight() / 2f - 180f, paint);
        paint.setAlpha(255);
        paint.setFakeBoldText(false);

        postInvalidateDelayed(16);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_DOWN) return true;

        float x = e.getX();
        float y = e.getY();


        if (gamePlay.isGameOver()) {
            if (btnNewGame != null && btnNewGame.contains(x, y)) {
                handleNewGame();
                return true;
            }
            if (btnHome != null && btnHome.contains(x, y)) {
                handleGoHome();
                return true;
            }
            return true;
        }


        if (GameSession.isAiGame && !gamePlay.isWhiteTurn()) return true;


        float btnWidth = 150f, btnHeight = 60f;
        float btnX = getWidth() / 2f - btnWidth / 2f;
        float btnY = getHeight() - btnHeight - 30f;

        if (x >= btnX && x <= btnX + btnWidth && y >= btnY && y <= btnY + btnHeight) {
            boolean beforeTurn = gamePlay.isWhiteTurn();
            startDiceAnimation();
            postDelayed(() -> {
                if (beforeTurn != gamePlay.isWhiteTurn()) startTurnBanner();
                triggerBotIfNeeded();
            }, 1300);
            highlightedPiece = -1;
            updateAllPiecePositions();
            invalidate();
            return true;
        }


        float centerY = getHeight() / 2f;
        float buttonHalfWidth = 20f, buttonHalfHeight = 50f;
        float whiteX = horizontalOffset + 1f - buttonHalfWidth;
        if (x >= whiteX - buttonHalfWidth && x <= whiteX + buttonHalfWidth &&
                y >= centerY - buttonHalfHeight && y <= centerY + buttonHalfHeight) {
            boolean beforeTurn = gamePlay.isWhiteTurn();
            gamePlay.bearOffSelected(true);
            if (beforeTurn != gamePlay.isWhiteTurn()) {
                startTurnBanner();
                triggerBotIfNeeded();
            }
            highlightedPiece = -1;
            updateAllPiecePositions();
            invalidate();
            return true;
        }
        float blackX = getWidth() - horizontalOffset - 1f - buttonHalfWidth;
        if (x >= blackX - buttonHalfWidth && x <= blackX + buttonHalfWidth &&
                y >= centerY - buttonHalfHeight && y <= centerY + buttonHalfHeight) {
            boolean beforeTurn = gamePlay.isWhiteTurn();
            gamePlay.bearOffSelected(false);
            if (beforeTurn != gamePlay.isWhiteTurn()) {
                startTurnBanner();
                triggerBotIfNeeded();
            }
            highlightedPiece = -1;
            updateAllPiecePositions();
            invalidate();
            return true;
        }


        if (isTouchOnBar(x, y, true) && gamePlay.hasPiecesInBar(true)) {
            gamePlay.selectFromBar(true);
            highlightedPiece = -1;
            updateAllPiecePositions();
            invalidate();
            return true;
        }
        if (isTouchOnBar(x, y, false) && gamePlay.hasPiecesInBar(false)) {
            gamePlay.selectFromBar(false);
            highlightedPiece = -1;
            updateAllPiecePositions();
            invalidate();
            return true;
        }


        int target = findTargetByTouch(x, y);
        if (target != -1) {
            boolean beforeTurn = gamePlay.isWhiteTurn();
            gamePlay.movePiece(target);
            if (beforeTurn != gamePlay.isWhiteTurn()) {
                startTurnBanner();
                triggerBotIfNeeded();
            }
            highlightedPiece = -1;
            updateAllPiecePositions();
            invalidate();
            return true;
        }


        int pointClicked = findPieceByTouch(x, y);
        if (pointClicked != -1) {
            highlightedPiece = pointClicked;
            gamePlay.selectPiece(pointClicked);
        } else {
            highlightedPiece = -1;
            gamePlay.selectPiece(pointClicked);
        }

        updateAllPiecePositions();
        invalidate();
        return true;
    }


    private void triggerBotIfNeeded() {
        if (!GameSession.isAiGame) return;
        if (gamePlay.isWhiteTurn()) return;
        if (gamePlay.isGameOver()) return;

        postDelayed(() -> {
            if (gamePlay.isGameOver() || gamePlay.isWhiteTurn()) return;
            startBotDiceAnimation();
        }, 600);
    }

    private void startBotDiceAnimation() {
        diceLeftSide = !diceLeftSide;
        diceAnimating = true;
        diceAnimationStart = System.currentTimeMillis();

        post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - diceAnimationStart;
                if (elapsed < 800) {
                    animDie1 = 1 + (int) (Math.random() * 6);
                    animDie2 = 1 + (int) (Math.random() * 6);
                    invalidate();
                    postDelayed(this, 100);
                } else {
                    diceAnimating = false;
                    gamePlay.rollDice();
                    invalidate();

                    runBotMoves();
                }
            }
        });
    }

    private void runBotMoves() {
        if (gamePlay.isGameOver() || gamePlay.isWhiteTurn() || botPlayer == null) return;

        postDelayed(() -> {
            if (gamePlay.isGameOver() || gamePlay.isWhiteTurn()) {
                updateAllPiecePositions();
                invalidate();
                return;
            }

            boolean moved = botPlayer.makeMove();


            postDelayed(() -> {
                updateAllPiecePositions();
                invalidate();


                if (moved && !gamePlay.isGameOver() && !gamePlay.isWhiteTurn()) {
                    postDelayed(this::runBotMoves, 1000);
                } else if (gamePlay.isWhiteTurn()) {
                    startTurnBanner();
                }
            }, 1000);
        }, 500);
    }

    private void handleNewGame() {
        gamePlay.resetGame();

        gameOverSent = false;
        showEndButtons = false;

        btnNewGame = null;
        btnHome = null;

        if (db != null) {
            GameSession.gameId = db.createGame(
                    GameSession.player1Id,
                    GameSession.player2Id,
                    -1
            );

            gamePlay.setGameContext(
                    GameSession.gameId,
                    db,
                    getContext()
            );
        }

        if (GameSession.isAiGame) {
            botPlayer = new BotPlayer(gamePlay);
        }

        showTurnBanner = true;
        turnBannerStart = System.currentTimeMillis();

        updateAllPiecePositions();
        invalidate();
    }

    private void handleGoHome() {
        Context ctx = getContext();

        Intent intent = new Intent(
                ctx,
                ProfileActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK
        );

        ctx.startActivity(intent);
    }

    private void startDiceAnimation() {
        if (diceAnimating) return;
        diceLeftSide = !diceLeftSide;
        diceAnimating = true;
        diceAnimationStart = System.currentTimeMillis();

        post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - diceAnimationStart;
                if (elapsed < 1200) {
                    animDie1 = 1 + (int) (Math.random() * 6);
                    animDie2 = 1 + (int) (Math.random() * 6);
                    invalidate();
                    postDelayed(this, 100);
                } else {
                    diceAnimating = false;
                    gamePlay.rollDice();
                    invalidate();
                }
            }
        });
    }

    private void drawDice(Canvas c) {
        if (!gamePlay.isDiceRolled() && !diceAnimating) return;

        int d1 = diceAnimating ? animDie1 : gamePlay.getDice().getDie1();
        int d2 = diceAnimating ? animDie2 : gamePlay.getDice().getDie2();

        float size = 110f, gap = 30f;
        float boardHalfWidth = getWidth() / 2f;
        float centerX = diceLeftSide ? boardHalfWidth / 2f + 20f : boardHalfWidth + boardHalfWidth / 2f - 40f;
        float startX = centerX - (size * 2 + gap) / 2f;
        float y = getHeight() / 2f - size / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        c.drawRoundRect(startX, y, startX + size, y + size, 18f, 18f, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(52f);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(String.valueOf(d1), startX + size / 2f, y + size / 2f + 18f, paint);

        float x2 = startX + size + gap;
        paint.setColor(Color.WHITE);
        c.drawRoundRect(x2, y, x2 + size, y + size, 18f, 18f, paint);
        paint.setColor(Color.BLACK);
        c.drawText(String.valueOf(d2), x2 + size / 2f, y + size / 2f + 18f, paint);
    }

    private void drawDiceButton(Canvas c) {
        if (GameSession.isAiGame && !gamePlay.isWhiteTurn()) return;

        paint.setStyle(Paint.Style.FILL);
        float btnWidth = 150f, btnHeight = 60f;
        float x = getWidth() / 2f - btnWidth / 2f;
        float y = getHeight() - btnHeight - 30f;

        paint.setColor(Color.BLUE);
        c.drawRect(x, y, x + btnWidth, y + btnHeight, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(36f);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText("Roll Dice", x + btnWidth / 2f, y + btnHeight / 2f + 12f, paint);
    }

    private void drawLegalTargets(Canvas c) {
        if (gamePlay.getSelectedPiece() == null) return;
        for (int point : gamePlay.getLegalTargets()) drawTargetCircle(c, point, Color.GREEN);
        for (int point : gamePlay.getBlockedTargets()) drawTargetCircle(c, point, Color.RED);
    }

    private void drawTargetCircle(Canvas c, int point, int color) {
        float[] pos = calculateBasePosition(point);
        int stackHeight = getOccupiedCount(point);
        boolean isTop = point > 12;
        float spacing = stackHeight > 5 ? pieceRadius * 1.2f : pieceRadius * verticalSpacingFactor;
        float y = isTop ? topBaseY + stackHeight * spacing : bottomBaseY - stackHeight * spacing;
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(pos[0], y, pieceRadius * targetCircleFactor, paint);
    }

    private void drawBearOffButtons(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        float centerY = getHeight() / 2f;
        float buttonHalfWidth = 20f, buttonHalfHeight = 50f;

        float whiteX = horizontalOffset + 1f - buttonHalfWidth;
        paint.setColor(Color.LTGRAY);
        c.drawRect(whiteX - buttonHalfWidth, centerY - buttonHalfHeight,
                whiteX + buttonHalfWidth, centerY + buttonHalfHeight, paint);
        if (gamePlay.canBearOffSelected() && gamePlay.isWhiteTurn()) {
            paint.setColor(Color.GREEN);
            c.drawCircle(whiteX, centerY - 65f, 12f, paint);
        }

        float blackX = getWidth() - horizontalOffset - 1f - buttonHalfWidth;
        paint.setColor(Color.DKGRAY);
        c.drawRect(blackX - buttonHalfWidth, centerY - buttonHalfHeight,
                blackX + buttonHalfWidth, centerY + buttonHalfHeight, paint);
        if (gamePlay.canBearOffSelected() && !gamePlay.isWhiteTurn()) {
            paint.setColor(Color.GREEN);
            c.drawCircle(blackX, centerY - 65f, 12f, paint);
        }
    }

    private void drawBarPieces(Canvas c) {
        Piece[][] bar = gamePlay.getBar();
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float spacing = pieceRadius * 1.4f;
        drawBarStack(c, bar[0], centerX, centerY + spacing, 1);
        drawBarStack(c, bar[1], centerX, centerY - spacing, -1);
    }

    private void drawBarStack(Canvas c, Piece[] stack, float x, float baseY, int dir) {
        int slot = 0;
        for (Piece p : stack) {
            if (p == null) continue;
            float y = baseY + dir * slot * pieceRadius * 1.4f;
            p.setPosition(x, y);
            drawPiece(c, p);
            slot++;
        }
    }

    private void drawPiece(Canvas c, Piece p) {
        paint.setStyle(Paint.Style.FILL);
        if (highlightedPiece != -1 && p == gamePlay.getTopPieceAtPoint(highlightedPiece)) {
            paint.setColor(p.isWhite() ? Color.rgb(144, 238, 144) : Color.rgb(0, 100, 0));
        } else {
            paint.setColor(p.isWhite() ? Color.WHITE : Color.BLACK);
        }
        c.drawCircle(p.getX(), p.getY(), pieceRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        c.drawCircle(p.getX(), p.getY(), pieceRadius, paint);
        paint.setStyle(Paint.Style.FILL);
    }


    private int getOccupiedCount(int point) {
        int count = 0;
        for (Piece p : gamePlay.getBoard()[point - 1])
            if (p != null) count++;
        return count;
    }

    private boolean isTouchOnBar(float x, float y, boolean white) {
        int idx = white ? 1 : 0;
        for (Piece p : gamePlay.getBar()[idx]) {
            if (p == null) continue;
            float dx = x - p.getX(), dy = y - p.getY();
            if (dx * dx + dy * dy <= pieceRadius * pieceRadius * 2) return true;
        }
        return false;
    }

    private int findPieceByTouch(float x, float y) {
        Piece[][] board = gamePlay.getBoard();
        for (int t = 0; t < 24; t++) {
            for (Piece p : board[t]) {
                if (p == null) continue;
                float dx = x - p.getX(), dy = y - p.getY();
                if (dx * dx + dy * dy <= pieceRadius * pieceRadius * 2) return t + 1;
            }
        }
        return -1;
    }

    private int findTargetByTouch(float x, float y) {
        for (int p : gamePlay.getLegalTargets()) {
            float[] pos = calculateBasePosition(p);
            int stackHeight = getOccupiedCount(p);
            boolean isTop = p > 12;
            float spacing = stackHeight > 5 ? pieceRadius * 1.2f : pieceRadius * verticalSpacingFactor;
            float yPos = isTop ? topBaseY + stackHeight * spacing : bottomBaseY - stackHeight * spacing;
            float dx = x - pos[0], dy = y - yPos;
            if (dx * dx + dy * dy <= pieceRadius * pieceRadius * 2) return p;
        }
        return -1;
    }

    private void updateAllPiecePositions() {
        Piece[][] board = gamePlay.getBoard();
        for (int t = 0; t < 24; t++) {
            int slot = 0;
            for (int s = 0; s < board[t].length; s++) {
                if (board[t][s] == null) continue;
                float[] pos = calculatePiecePosition(t + 1, slot);
                board[t][s].setPosition(pos[0], pos[1]);
                slot++;
            }
        }
    }

    private float[] calculatePiecePosition(int point, int slot) {
        float centerX = calculateCenterX(point);
        boolean isTop = point > 12;
        int count = 0;
        for (Piece p : gamePlay.getBoard()[point - 1]) if (p != null) count++;
        float spacing = count > 5 ? pieceRadius * 1.2f : pieceRadius * verticalSpacingFactor;
        float y = isTop ? topBaseY + slot * spacing : bottomBaseY - slot * spacing;
        return new float[]{centerX, y};
    }

    private float[] calculateBasePosition(int point) {
        return new float[]{calculateCenterX(point), point > 12 ? topBaseY : bottomBaseY};
    }

    private float calculateCenterX(int point) {
        int safePoint = Math.max(1, Math.min(24, point));
        int index = safePoint > 12 ? safePoint - 13 : 12 - safePoint;
        float x = index * triangleWidth + triangleWidth / 2f + horizontalOffset;
        if (index >= 6) x += barWidth + rightBoardOffset;
        return x;
    }
}
