package org.literacybridge.core.spec;

//import org.literacybridge.core.spec.CSVParser.CSVParser;
//import org.literacybridge.core.spec.CSVParser.CSVParserBuilder;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvReader {
    public interface Handler {
        void handle(Map<String, String> record);
    }

    private File csvFile;
    private String[] columns;

    private CSVParser parser = new CSVParserBuilder().build();

    CsvReader(File csvFile, String[] columns) {
        this.csvFile = csvFile;
        this.columns = columns;
    }

    void read(Handler handler) throws IOException {
        if (csvFile.exists()) {
            //read file into stream, try-with-resources
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                String line = br.readLine();
                Map<String, Integer> indices = null;
                if (line != null) {
                    String[] parts = parser.parseLine(line.trim());
                    indices = indicesOfColumns(columns, parts);
                }
                while ((line = br.readLine()) != null) {
                    String[] parts = parser.parseLine(line.trim());

                    Map<String,String> record = new HashMap<>();
                    for (Map.Entry<String,Integer> e : indices.entrySet()) {
                        String value = parts[e.getValue()];
                        // If the value is enclosed in quotes, drop the quotes.
                        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length()-1);
                        }
                        record.put(e.getKey(), value);
                    }
                    handler.handle(record);
                }
            }
        }
    }

    private Map<String, Integer> indicesOfColumns(String[] columns, String[] line) {
        Set<String> list = new HashSet<>();
        Map<String, Integer> indices = new HashMap<>();
        for (int ix=0; ix<columns.length; ix++) {
            list.add(columns[ix]);
        }
        for (int ix=0; ix<line.length; ix++) {
            if (list.contains(line[ix])) {
                indices.put(line[ix], ix);
            }
        }
        return indices;
    }

    public String[] columnNames(Class<? extends Enum<?>> e) {
        return Arrays.toString(e.getEnumConstants()).replaceAll("^.|.$", "").split(", ");
    }


}
