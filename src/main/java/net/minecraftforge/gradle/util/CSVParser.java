package net.minecraftforge.gradle.util;

import com.google.common.collect.Sets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tools and utilities for parsing a CSV file and working with its contents.
 */
public class CSVParser {

    /**
     * Parses a CSV file and returns a {@link Result} object with the entries.
     */
    public static Result parse(File file) throws FileNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        List<String> lines = br.lines().collect(Collectors.toList());

        String header = lines.remove(0);
        String[] columns = header.split(",");

        List<Entry> entries = new ArrayList<>();
        for (String line : lines) {
            Map<String, String> data = new HashMap<>();
            parseLine(line, columns, data);
            entries.add(new Entry(data));
        }

        return new Result(Sets.newHashSet(columns), entries);
    }

    /**
     * Parses each element of a CSV line and adds it to the map.
     */
    private static void parseLine(String line, String[] columns, Map<String, String> data) {
        String current = null;
        int i = 0;
        for (String s : line.split(",")) {
            if (current == null) {
                if (s.startsWith("\"") && !s.endsWith("\"")) {
                    current = s;
                } else {
                    data.put(columns[i++], format(s));
                }
            } else {
                current += s;
                if (current.endsWith("\"")) {
                    data.put(columns[i++], format(current));
                    current = null;
                }
            }
        }
    }

    /**
     * Removes quotation marks around a string and turns escaped double quotes ("")
     * into regular double quotes (").
     */
    private static String format(String value) {
        if (value.startsWith("\"")) {
            value = value.substring(1, value.length() - 2);
        }
        return value.replace("\"\"", "\"");
    }

    /**
     * The result of parsing a CSV file. Contains the column names and list of entries.
     */
    public static class Result implements Iterable<Entry> {

        private final Set<String> columns;
        private final List<Entry> entries;

        private Result(Set<String> columns, List<Entry> entries) {
            this.columns = columns;
            this.entries = entries;
        }

        public Set<String> getColumns() {
            return columns;
        }

        public List<Entry> getEntries() {
            return entries;
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }

    }

    /**
     * Represents an entry in a CSV file.
     */
    public static class Entry {

        private final Map<String, String> data;

        private Entry(Map<String, String> data) {
            this.data = data;
        }

        public String get(String column) {
            return data.get(column);
        }

        public int getAsInt(String column) {
            return Integer.parseInt(get(column));
        }

    }

}
