
package org.cartscheduler.generator;

import java.util.ArrayList;
import java.util.List;



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
            if (originalCell.isFixed()) {
                copyCell.setFixedCandidate(originalCell.getCandidate());
            } else {
                copyCell.setCandidate(originalCell.getCandidate());
            }

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
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY) {
            throw new IndexOutOfBoundsException("Cell coordinates are outside the sub-sheet");
        }
        // Cells are created column by column in the constructor.
        return cells.get(x * sizeY + y);
    }

    public List<Cell> getCells(Integer x, Integer y) {
        if (x != null && y != null) {
            return List.of(getCell(x, y));
        }
        if (x == null && y == null) {
            return new ArrayList<>(cells);
        }

        List<Cell> result = new ArrayList<>();
        if (x != null) {
            for (int currentY = 0; currentY < sizeY; currentY++) {
                result.add(getCell(x, currentY));
            }
            return result;
        }

        for (int currentX = 0; currentX < sizeX; currentX++) {
            result.add(getCell(currentX, y));
        }
        return result;
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

