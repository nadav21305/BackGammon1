package com.example.backgammon;

public class Piece {

    private float x, y;
    private int pointIndex;
    private int color;

    public float getX() { return x; }
    public float getY() { return y; }
    public int getPointIndex() { return pointIndex; }
    public int getColor() { return color; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setPointIndex(int pointIndex) {
        this.pointIndex = pointIndex;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isWhite() {
        return color > 0;
    }
}