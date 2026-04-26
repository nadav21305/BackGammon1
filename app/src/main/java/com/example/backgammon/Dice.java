package com.example.backgammon;

import java.util.Random;

public class Dice {
    private final Random random = new Random();
    private int die1 = 1;
    private int die2 = 1;

    public void roll() {
        die1 = random.nextInt(6) + 1;
        die2 = random.nextInt(6) + 1;
    }

    public int getDie1() { return die1; }
    public int getDie2() { return die2; }
}