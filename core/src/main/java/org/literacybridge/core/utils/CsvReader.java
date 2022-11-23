package org.literacybridge.core.utils;

import com.opencsv.CSVReader;
import org.apache.commons.io.input.BOMInputStream;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Yet another Csv Reader. Perhaps this should be named YACR.
 *
 * This uses openCsv to parse a CSV from a file, a stream, or a reader. The rows are available via an iterator,
 * and are returned row-at-a-time as a {key:value} Map.
 *
 * Usage:
 *     File csvFle = new File(...);
 *     try (csvReader = new CsvReader(csvFile)) {
 *         for (Map<String,String> row: csvReader) {
 *             // do something with row
 *         }
 *     }
 */
public class CsvReader implements Iterable<Map<String,String>>, Closeable {
    private final CSVReader csvReader;
    private boolean closed = false;
    // Headers, from the first line.
    private final String[] headers;

    public CsvReader(File csvFile) throws IOException {
        this(Files.newInputStream(csvFile.toPath()));
    }
    public CsvReader(InputStream csvStream) throws IOException {
        this(new InputStreamReader(csvStream instanceof BOMInputStream ? csvStream : new BOMInputStream(csvStream)));
    }
    public CsvReader(Reader reader) throws IOException {
        csvReader = new CSVReader(reader);
        headers = csvReader.readNext();
    }

    @Override
    public Iterator<Map<String, String>> iterator() {
        if (closed) throw new IllegalStateException("Attempt to iterate a closed "+CsvReader.class.getName());
        return new CsvIterator();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (csvReader != null) csvReader.close();
    }

    private class CsvIterator implements Iterator<Map<String,String>> {
        private CsvIterator() {

        }

        @Override
        public boolean hasNext() {
            try {
                return csvReader.peek() != null;
            } catch (IOException ignored) { }
            return false;
        }

        @Override
        public Map<String, String> next() {
            String[] values = null;
            try {
                values = csvReader.readNext();
            } catch (IOException ignored) { }
            // If there is no next record, let the caller know.
            if (values == null) {
                throw new NoSuchElementException();
            }
            // Create the Map from the headers and values.
            Map<String,String> result = new LinkedHashMap<>();
            for (int i=0; i<headers.length; ++i) {
                result.put(headers[i], values[i]);
            }
            return result;
        }
    }

}
