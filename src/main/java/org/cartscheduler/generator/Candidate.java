package org.cartscheduler.generator;

public class Candidate {

    private final int id;

    public Candidate(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "ID: " + this.id;
    }
}

