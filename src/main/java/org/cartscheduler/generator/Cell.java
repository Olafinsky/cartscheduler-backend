package org.cartscheduler.generator;

public class Cell {
    private Candidate candidate = null;
    private boolean fixed;

    private int x;
    private int y;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    /**
     * Marks this cell as an input imposed by the caller. The raw generator keeps
     * such a value unchanged while it searches values for the remaining cells.
     */
    public void setFixedCandidate(Candidate candidate) {
        this.candidate = candidate;
        this.fixed = true;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public boolean isFixed() {
        return fixed;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)", x, y);
    }
}

