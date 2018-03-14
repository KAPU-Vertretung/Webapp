package de.nikos410.kapu_vertretung.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SplanParser implements Parser{
    private final static int N = 410;
    
    private final static String[] IGNORED_CONTENTS = {"", "---", "+", "~"};
    
    private final OPCPackage pkg;
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    
    private int firstVertretungStart;
    private int firstHeaderStart;
    private int firstHeaderEnd;
    private String[] keys;
    
    public SplanParser (Path xlsxPath) {
        try {
            this.pkg = OPCPackage.open(xlsxPath.toFile());
            this.workbook = new XSSFWorkbook(this.pkg);
            this.sheet = this.workbook.getSheetAt(0);
        } catch (InvalidFormatException | IOException e) {
            throw new RuntimeException("Could not parse document.", e);
        }
    }

    public String parseToJSON() throws JSONException {
        try {
            this.firstVertretungStart = findFirstVertretung();
            this.firstHeaderStart = findFirstHeaderStart();
            this.firstHeaderEnd = findFirstHeaderEnd();
            this.keys = readKeys();
                    
            final int firstRow = sheet.getFirstRowNum();
            final int lastRow = sheet.getLastRowNum();
    
            final JSONObject json = new JSONObject("{}");
            
            String name = "Error";
            for (int seekRowNum = firstRow; seekRowNum < lastRow; seekRowNum++) {
               Row seekRow = sheet.getRow(seekRowNum);
               
               if (seekRow != null && !getStringCellValue(seekRow.getCell(0)).isEmpty() && !getStringCellValue(seekRow.getCell(0)).equalsIgnoreCase("stunde")) {
                   name = getStringCellValue(seekRow.getCell(0));
               }
               
               if (matchesHeaderEnd(seekRow)) {
                   seekRowNum++;
                   
                   // Die folgenden Reihen (bis zur nächsten Leerzeile gehören zu einer Klasse)
                   Row[] classRows = new Row[0];

                   Row activeRow = sheet.getRow(seekRowNum);
                   for (int i = 0; activeRow != null; i++) {
                       classRows = Arrays.copyOf(classRows, classRows.length+1);
                       
                       classRows[i] = activeRow;
                       
                       seekRowNum++;
                       activeRow = sheet.getRow(seekRowNum);
                   }
                   JSONArray classArray = parseClass(classRows);                                  
                   json.put(name, classArray);
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
    
    private JSONArray parseClass(Row[] rows) throws JSONException {
        // Das Array das die Vertretungen enthält
        JSONArray classArray = new JSONArray();
        
        // Map entspricht einer Vertretung
        LinkedHashMap<String, String> map = null;
        
        
        for (Row row : rows) {
            if (row.getCell(0) != null || !getStringCellValue(row.getCell(0)).isEmpty()) {
                if (map != null) {
                    JSONArray jsonArray = linkedMapToJsonArray(map);
                    classArray.put(jsonArray);
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
        
        JSONArray jsonArray = linkedMapToJsonArray(map);
        classArray.put(jsonArray);
        
        return classArray;
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
    
    private int findFirstVertretung() {
        final int firstRow = sheet.getFirstRowNum();
        final int lastRow = sheet.getLastRowNum();

        boolean lastRowEmpty = false;
        boolean emptyRowsFound = false;
        
        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++) {
           Row r = sheet.getRow(rowNum);
           
           if (r == null) {
               // Reihe ist leer
               if (lastRowEmpty) {
                   // aktuelle Reihe und letzte Reihe ist leer
                   emptyRowsFound = true;
               }
               else {
                   // aktuelle Reihe ist leer
                   lastRowEmpty = true;
               }
           }
           else {
               if (emptyRowsFound) {
                   return rowNum;
               }
               else {
                   lastRowEmpty = false;
               }
           }
        }
        return 0;
    }
    
    private int findFirstHeaderStart() {
        final int firstRow = this.firstVertretungStart;
        final int lastRow = sheet.getLastRowNum();
        
        boolean lastRowEmpty = false;
        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++) {
        Row r = sheet.getRow(rowNum);
               
            if (r == null) {
                // Reihe ist leer
                lastRowEmpty = true;
            }
            else {
               // Reihe ist nicht leer
                if (lastRowEmpty) {
                    return rowNum;
                }
                else {
                    lastRowEmpty = false;
                }
            }
        }
        
        throw new RuntimeException("Could not parse document.");
    }
    
    private int findFirstHeaderEnd() {
        // Erste Spalte iterieren um Feldüberschrift "Stunde zu finden"
        // Abfolge: Leer -> Stunde -> Zahl -> Leer
        
        final int firstRow = this.firstHeaderStart;
        final int lastRow = sheet.getLastRowNum();
        
        // 0: Leer, 1: Stunde, 2: Zahl, 3:Leer -> fertig!
        for (int rowNum = firstRow; rowNum <= lastRow; rowNum++) {
            Row r = sheet.getRow(rowNum);
            Cell c = r.getCell(0, Row.CREATE_NULL_AS_BLANK);
                
            if (c.getCellType() == Cell.CELL_TYPE_STRING && c.getStringCellValue().equalsIgnoreCase("stunde")) {
                return rowNum;
            }
        }
        
        throw new RuntimeException("Could not parse document.");
    }
    
    private String[] readKeys() {
        final int keyCount = sheet.getRow(this.findFirstHeaderEnd()).getLastCellNum();
        
        String[] keyArray = new String[keyCount]; 
        
        for (int rowNum = this.firstHeaderStart; rowNum <= this.firstHeaderEnd; rowNum++) {
            Row r = sheet.getRow(rowNum);
            
            int firstCellIndex = r.getFirstCellNum();
            int lastCellIndex = r.getLastCellNum();
            for (int cellNum = firstCellIndex; cellNum < lastCellIndex; cellNum++) {
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
    
    private boolean matchesHeaderEnd(Row row) {
        final Row headerEndRow = sheet.getRow(this.firstHeaderEnd);
        
        if (row == null) {
            return false;
        }
        
        int firstCellIndex = headerEndRow.getFirstCellNum();
        int lastCellIndex = headerEndRow.getLastCellNum();
        
        for (int cellNum = firstCellIndex; cellNum < lastCellIndex; cellNum++) {
            
            if ( !getStringCellValue(headerEndRow.getCell(cellNum)).equals(getStringCellValue(row.getCell(cellNum))) ) {
                return false;
            }
        }

        return true;
    }
    
    private boolean isContentIgnored(String content) {
        for (String ignoredContent : IGNORED_CONTENTS) {
            if (content.equals(ignoredContent)) {
                return true;
            }
        }
        return false;
    }
    
    private void printRow(Row row) {
        final int firstCellIndex = row.getFirstCellNum();
        final int lastCellIndex = row.getLastCellNum();
        
        for (int i = firstCellIndex; i < lastCellIndex; i++) {
            System.out.print(" | " + getStringCellValue(row.getCell(i)));
        }
        System.out.print('\n');
    }
}
