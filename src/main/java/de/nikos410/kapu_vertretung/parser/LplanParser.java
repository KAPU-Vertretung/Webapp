package de.nikos410.kapu_vertretung.parser;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class LplanParser implements Parser{
    private final static int N = 410;

    private final static String[] IGNORED_CONTENTS = {"", "---", "+", "~"};

    private final OPCPackage pkg;
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    private int headerStart;
    private int headerEnd;
    private String[] keys;

    public LplanParser (Path xlsxPath) {
        try {
            this.pkg = OPCPackage.open(xlsxPath.toFile());
            this.workbook = new XSSFWorkbook(this.pkg);
            this.sheet = this.workbook.getSheetAt(0);
        } catch (InvalidFormatException | IOException e) {
            throw new RuntimeException("Could not parse document.", e);
        }
    }

    public String parseToJSON() {
        try {
            findHeaderStartAndEnd();
            this.keys = readKeys();

            final int firstSeekRowNum = this.headerEnd+1;
            final int lastSeekRowNum = sheet.getLastRowNum();

            final JSONObject json = new JSONObject("{}");

            for (int seekRowNum = firstSeekRowNum; seekRowNum <= lastSeekRowNum; seekRowNum++) {
                Row seekRow = sheet.getRow(seekRowNum);

                if (isTeacherRow(seekRow)) {
                    // Zeile beginnt mit einem String, also Lehrerkürzel -> Die nächsten Zeilen
                    // (bis zur nächsten Zeile die mit einem String beginnt) gehören zu einem Lehrer

                    // Name des Lehrers
                    String name = getStringCellValue(seekRow.getCell(0, Row.CREATE_NULL_AS_BLANK));

                    Row[] teacherRows = new Row[0];

                    seekRowNum++;
                    Row activeRow = sheet.getRow(seekRowNum);

                    int i = 0;
                    do {
                        // Array um 1 Element vergrößern
                        teacherRows = Arrays.copyOf(teacherRows, teacherRows.length+1);

                        teacherRows[i] = activeRow;

                        seekRowNum++;
                        i++;
                        activeRow = sheet.getRow(seekRowNum);
                    } while (seekRowNum <= lastSeekRowNum && !isTeacherRow(activeRow));
                    seekRowNum--;


                    JSONArray teacherArray = parseTeacher(teacherRows);
                    json.put(name, teacherArray);
                }
            }

            this.pkg.revert();
            return json.toString(4);
        }
        catch (JSONException e) {
            this.pkg.revert();
            e.printStackTrace();
            throw e;
        }
    }

    private boolean isTeacherRow (Row row) {

        return row != null && row.getFirstCellNum() == 0 && row.getLastCellNum() == 1 &&
                row.getCell(0, Row.CREATE_NULL_AS_BLANK).getCellType() == Cell.CELL_TYPE_STRING;
    }

    private void printRow(Row row) {
        final int firstCellIndex = row.getFirstCellNum();
        final int lastCellIndex = row.getLastCellNum();

        for (int i = firstCellIndex; i < lastCellIndex; i++) {
            System.out.print(" | " + getStringCellValue(row.getCell(i)));
        }
        System.out.print('\n');
    }

    private JSONArray parseTeacher(Row[] rows) throws JSONException {
        // Das Array das die Vertretungen enthält
        JSONArray teacherArray = new JSONArray();

        // Map entspricht einer Vertretung
        LinkedHashMap<String, String> map = null;


        for (Row row : rows) {
            if (row.getCell(0, Row.RETURN_BLANK_AS_NULL) != null) {

                if (map != null) {
                    JSONArray jsonArray = linkedMapToJsonArray(map);
                    teacherArray.put(jsonArray);
                }
                map = new LinkedHashMap<>();

            }

            // Parsen
            int firstCellIndex = row.getFirstCellNum();
            int lastCellIndex = row.getLastCellNum();
            for (int cellNum = firstCellIndex; cellNum < lastCellIndex; cellNum++) {
                Cell cell = row.getCell(cellNum);

                String cellContent = getStringCellValue(cell);

                if (!isContentIgnored(cellContent)) {
                    String key = this.keys[cellNum];

                    if (map.containsKey(key)) {
                        // An bestehenden Inhalt anhängen
                        String prevContent = map.get(key);
                        String newContent = prevContent + ' ' + cellContent;

                        map.put(key, newContent);
                    }
                    else {
                        // Neu einfügen
                        map.put(key, cellContent);
                    }
                }
            }
        }

        if (map != null) {
            JSONArray jsonArray = linkedMapToJsonArray(map);
            teacherArray.put(jsonArray);
        }

        return teacherArray;
    }

    private JSONArray linkedMapToJsonArray(LinkedHashMap<String, String> map) throws JSONException {
        JSONArray jsonArray = new JSONArray();

        for (String key : map.keySet()) {
            String value = map.get(key);

            JSONObject obj = new JSONObject();
            obj.put("key", key);
            obj.put("value", value);

            jsonArray.put(obj);
        }

        return jsonArray;
    }

    private void findHeaderStartAndEnd() {
        // Erste Spalte iterieren um Feldüberschrift "Lehrer" und "Stunde" zu finden
        // Abfolge: Leer -> Lehrer -> Stunde -> Lehrerkürzel (String)

        final int firstRow = sheet.getFirstRowNum();
        final int lastRow = sheet.getLastRowNum();

        int emptyRowNum = -1;
        boolean emptyRowFound = false;   // Leerzeile wurde gefunden
        boolean lehrerRowFound = false;  // Zeile mit Inhalt "Lehrer" wurde gefunden
        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++) {
            Row r = sheet.getRow(rowNum);

            if (r == null) {
                // Reihe ist leer
                emptyRowFound = true;
                emptyRowNum = rowNum;
            }
            else if (emptyRowFound) {
                Cell c = r.getCell(0, Row.CREATE_NULL_AS_BLANK);
                if (!lehrerRowFound) {
                    if (c.getCellType() == Cell.CELL_TYPE_STRING && c.getStringCellValue().equalsIgnoreCase("lehrer")) {
                        lehrerRowFound = true;
                    }
                }
                else if (c.getCellType() == Cell.CELL_TYPE_STRING && c.getStringCellValue().equalsIgnoreCase("stunde")) {
                    if (sheet.getRow(rowNum+1).getCell(0, Row.CREATE_NULL_AS_BLANK)
                            .getCellType() == Cell.CELL_TYPE_STRING) {
                        this.headerStart = emptyRowNum+1;
                        this.headerEnd = rowNum;
                    }
                }
            }
        }
    }

    private String[] readKeys() {
        final int keyCount = sheet.getRow(this.headerEnd).getLastCellNum();

        String[] keyArray = new String[keyCount];
        keyArray[0] = "Stunde"; // Ist immer gleich, Zeile "Lehrer" muss als Key ignoriert werden

        for (int rowNum = this.headerStart; rowNum <= this.headerEnd; rowNum++) {
            Row r = sheet.getRow(rowNum);

            int firstCellIndex = r.getFirstCellNum();
            int lastCellIndex = r.getLastCellNum();
            for (int cellNum = firstCellIndex+1; cellNum < lastCellIndex; cellNum++) {
                Cell c = r.getCell(cellNum, Row.CREATE_NULL_AS_BLANK);

                final String currentContent = keyArray[cellNum];
                keyArray[cellNum] = (currentContent == null || currentContent.isEmpty()) ? getStringCellValue(c) : currentContent+' '+getStringCellValue(c);
            }
        }

        return keyArray;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING: return cell.getStringCellValue();
            case Cell.CELL_TYPE_NUMERIC: return Integer.toString((int)cell.getNumericCellValue());
            default: return "";
        }
    }

    private boolean isContentIgnored(String content) {
        for (String ignoredContent : IGNORED_CONTENTS) {
            if (content.equals(ignoredContent)) {
                return true;
            }
        }
        return false;
    }
}
