package org.literacybridge.core.spec;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Program Specification wrapper.
 */
public class ProgramSpec {

    private final File programSpecDir;

    private List<Deployment> deployments = null;
    private RecipientList recipients = null;
    private Map<String, String> recipientsMap = null;

    public ProgramSpec(File programSpecDir) {
        this.programSpecDir = programSpecDir;
    }

    public RecipientList getRecipients() {
        if (recipients == null) {
            File csvFile = new File(programSpecDir, Recipient.FILENAME);
            if (csvFile.exists()) {
                try {
                    final RecipientList result = new RecipientList();
                    CsvReader reader = new CsvReader(csvFile, Recipient.columnNames);
                    reader.read(result::add);
                    recipients = result;
                } catch (IOException ignored) {
                    // Just leave recipients null
                }
            }
        }
        return recipients;
    }

    public Map<String, String> getRecipientsMap() {
        if (recipientsMap == null) {
            File csvFile = new File(programSpecDir, RecipientMap.FILENAME);
            if (csvFile.exists()) {
                try {
                    final Map<String, String> result = new HashMap<>();
                    CsvReader reader = new CsvReader(csvFile, RecipientMap.columnNames);
                    reader.read(record -> result.put(record.get(RecipientMap.columns.recipientid.name()), record.get(RecipientMap.columns.directory.name())));
                    recipientsMap = result;
                } catch (IOException ignored) {
                }
            }
        }
        return recipientsMap;
    }
    
    public List<Deployment> getDeployments() {
        if (deployments == null) {
            File csvFile = new File(programSpecDir, Deployment.FILENAME);
            if (csvFile.exists()) {
                try {
                    final List<Deployment> result = new ArrayList<>();
                    CsvReader reader = new CsvReader(csvFile, Deployment.columnNames);
                    reader.read(record -> {
                        try {
                            result.add(new Deployment(record));
                        } catch (ParseException ignored) {
                        }
                    });
                    deployments = result;
                } catch (IOException ignored) {
                }
            }
        }
        return deployments;
    }

}
