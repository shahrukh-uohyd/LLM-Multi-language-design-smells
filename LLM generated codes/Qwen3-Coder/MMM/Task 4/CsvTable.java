package com.example;

import java.util.*;

/**
 * Immutable representation of parsed CSV/TSV data with type inference.
 */
public final class CsvTable {
    private final String[] headers;
    private final Object[][] rows; // Each cell: String, Integer, Double, or Boolean
    private final char delimiter;
    
    CsvTable(String[] headers, Object[][] rows, char delimiter) {
        this.headers = headers != null ? headers.clone() : null;
        this.rows = rows.clone();
        for (int i = 0; i < this.rows.length; i++) {
            this.rows[i] = rows[i].clone();
        }
        this.delimiter = delimiter;
    }
    
    public int rowCount() { return rows.length; }
    public int columnCount() { return rows.length > 0 ? rows[0].length : 0; }
    
    public String[] headers() {
        return headers != null ? headers.clone() : null;
    }
    
    public char delimiter() { return delimiter; }
    
    public Object get(int row, int col) {
        if (row < 0 || row >= rows.length || col < 0 || col >= rows[row].length) {
            throw new IndexOutOfBoundsException();
        }
        return rows[row][col];
    }
    
    public String getString(int row, int col) {
        Object val = get(row, col);
        return val != null ? val.toString() : null;
    }
    
    public int getInt(int row, int col, int defaultValue) {
        Object val = get(row, col);
        if (val instanceof Integer) return (Integer)val;
        if (val instanceof Number) return ((Number)val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String)val); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }
    
    public double getDouble(int row, int col, double defaultValue) {
        Object val = get(row, col);
        if (val instanceof Double) return (Double)val;
        if (val instanceof Number) return ((Number)val).doubleValue();
        if (val instanceof String) {
            try { return Double.parseDouble((String)val); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }
    
    public boolean getBoolean(int row, int col, boolean defaultValue) {
        Object val = get(row, col);
        if (val instanceof Boolean) return (Boolean)val;
        if (val instanceof String) {
            String s = ((String)val).trim().toLowerCase();
            return "true".equals(s) || "yes".equals(s) || "1".equals(s);
        }
        return defaultValue;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                sb.append(headers[i]);
                if (i < headers.length - 1) sb.append(delimiter);
            }
            sb.append("\n");
        }
        
        for (Object[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                sb.append(row[i] != null ? row[i].toString() : "null");
                if (i < row.length - 1) sb.append(delimiter);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}