
package org.cartscheduler.generator;

import java.util.ArrayList;
import java.util.List;

public class Sheet {
    private final List<SubSheet> subSheetList = new ArrayList<>();

    private Sheet() {}

    public Sheet(int subSheetCount, int subSheetSizeX, int subSheetSizeY) {
        for (int i = 0; i < subSheetCount; i++) {
            subSheetList.add(new SubSheet(subSheetSizeX, subSheetSizeY));
        }
    }

    /**
     * Creates a sheet from sub-sheets which may have different dimensions.
     */
    public Sheet(List<SubSheet> subSheets) {
        subSheetList.addAll(subSheets);
    }

    public List<SubSheet> getSubSheets() {
        return subSheetList;
    }

    public Sheet copy() {
        Sheet copySheet = new Sheet();

        for (SubSheet subSheet : subSheetList) {
            copySheet.subSheetList.add(subSheet.copy());
        }

        return copySheet;
    }
}

