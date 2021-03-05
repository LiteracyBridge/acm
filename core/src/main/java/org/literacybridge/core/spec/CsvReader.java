package org.literacybridge.core.spec;

//import org.literacybridge.core.spec.CSVParser.CSVParser;
//import org.literacybridge.core.spec.CSVParser.CSVParserBuilder;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvReader {
    /**
     * Interface describing the handler for individual records.
     */
    public interface Handler {
        /**
         * Called to handle a single record.
         * @param record to be processed, as a Map<Column,Value>.
         */
        void handle(Map<String, String> record);
    }

    private File csvFile;
    private String[] columns;

    private CSVParser parser = new CSVParserBuilder().build();

    /**
     * Create a csv file reader on the given file, with the list or relevant columns.
     * @param csvFile to be processed.
     * @param columns of interest.
     */
    CsvReader(File csvFile, String[] columns) {
        this.csvFile = csvFile;
        this.columns = columns;
    }

    /**
     * Read the csv file and give each record to the handler.
     * @param handler for each record.
     * @throws IOException if the file can't be read.
     */
    void read(Handler handler) throws IOException {
        if (csvFile.exists()) {
            try (FileInputStream fix = new FileInputStream(csvFile)) {
                read(fix, columns, handler);
            }
        }
    }

    /**
     * Worker to read a csv from a stream.
     * @param csvStream of the csv data.
     * @param columns of interest.
     * @param handler to be called with each record.
     * @return a Set of the names of the columns found in the .csv header, or an empty set if there is an error.
     */
    static Set<String> read(InputStream csvStream, String[] columns, Handler handler) throws IOException {
        List<String[]> entries = null;
        BOMInputStream bis = new BOMInputStream(csvStream);
        try (Reader ir = new InputStreamReader(bis, StandardCharsets.UTF_8);
            CSVReader reader = new CSVReader(ir)) {

            Map<String, Integer> indices;
            String[] nextLine;
            nextLine = reader.readNext();
            if (nextLine != null) {
                indices = indicesOfColumns(columns, nextLine);
                while ((nextLine = reader.readNext()) != null) {
                    Map<String, String> record = new HashMap<>();
                    for (Map.Entry<String, Integer> e : indices.entrySet()) {
                        int ix = e.getValue();
                        String value = "";
                        if (ix < nextLine.length)
                            value = nextLine[ix];
                        // If the value is enclosed in quotes, drop the quotes.
                        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        record.put(e.getKey(), value);
                    }
                    handler.handle(record);
                }
                return indices.keySet();
            }
        }
        return new HashSet<>();
    }

    /**
     * Given a list of columns of interest, and a csv header line, determine the column number of
     * each column of interest.
     * @param columns of interest.
     * @param csvLine of headers.
     * @return Map of column indices of columns of interest.
     */
    private static Map<String, Integer> indicesOfColumns(String[] columns, String[] csvLine) {
        List<String> columnsOfInterest = Arrays.asList(columns);
        Map<String, Integer> indices = new HashMap<>();
        for (int ix=0; ix<csvLine.length; ix++) {
            if (columnsOfInterest.contains(csvLine[ix])) {
                indices.put(csvLine[ix], ix);
            }
        }
        return indices;
    }

    public String[] columnNames(Class<? extends Enum<?>> e) {
        return Arrays.toString(e.getEnumConstants()).replaceAll("^.|.$", "").split(", ");
    }


}
