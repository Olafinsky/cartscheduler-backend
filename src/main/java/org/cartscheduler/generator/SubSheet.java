
package org.cartscheduler.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;



public class SubSheet {
    private final List<Cell> cells = new ArrayList<>();
    private int sizeX;
    private int sizeY;

    private SubSheet() {}

    public SubSheet(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                cells.add(new Cell(x, y));
            }
        }
    }

    public SubSheet copy() {
        SubSheet copySubSheet = new SubSheet();
        copySubSheet.sizeX = sizeX;
        copySubSheet.sizeY = sizeY;

        for (Cell originalCell : cells) {
            Cell copyCell = new Cell(originalCell.getX(), originalCell.getY());
            copyCell.setCandidate(originalCell.getCandidate());

            copySubSheet.cells.add(copyCell);
        }

        return copySubSheet;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public Cell getCell(int x, int y) {
        return cells.stream()
            .filter(c -> c.getX() == x)
            .filter(c -> c.getY() == y)
            .collect(Collectors.toList())
            .getFirst();
    }

     public List<Cell> getCells(Integer x, Integer y) {

        Stream<Cell> stream = cells.stream();

        if (x != null) {
            stream = stream.filter(c -> c.getX() == x);
        }

        if (y != null) {
            stream = stream.filter(c -> c.getY() == y);
        }

        return stream.collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String result = "";

        for (int y = 0; y < sizeY; y++) {
            result += String.format("%d: ", y);
            for (int x = 0; x < sizeX; x++) {
                Candidate candidate = getCell(x, y).getCandidate();
                result += String.format("[ %d ] ", candidate != null ? candidate.getId() : null);
            }
            result += String.format("%n");
        }


        return result;
    }
}

