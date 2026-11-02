package org.cartscheduler.generator;

public class Candidate {

    private final long id;

    public Candidate(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "ID: " + this.id;
    }
}

