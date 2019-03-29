package org.literacybridge.core.spec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Program Specification wrapper.
 */
public class ProgramSpec {

    private final File programSpecDir;

    private List<String> components = null;
    private List<Deployment> deployments = null;
    private RecipientList recipients = null;
    private Map<String, String> recipientsMap = null;
    private Content content = null;

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

    /**
     * Get the recipients whose components are configured to receive the given deployment.
     * @param deploymentNumber of interest.
     * @return the Recipients in that deployment.
     */
    public RecipientList getRecipientsForDeployment(int deploymentNumber) {
        RecipientList filteredRecipients = new RecipientList();
        Deployment deployment = getDeployment(deploymentNumber);
        StringFilter componentFilter = deployment.componentFilter;
        getRecipients().stream()
            .filter(r -> componentFilter.test(r.component))
            .forEach(filteredRecipients::add);
        return filteredRecipients;
    }

    public Set<String> getLanguagesForDeployment(int deploymentNumber) {
        RecipientList recipients = getRecipientsForDeployment(deploymentNumber);
        Set<String> languages = recipients.stream().map(r -> r.language).collect(Collectors.toSet());
        return languages;
    }

    public Map<String, String> getRecipientsMap() {
        if (recipientsMap == null) {
            File csvFile = new File(programSpecDir, RecipientMap.FILENAME);
            if (csvFile.exists()) {
                try {
                    final Map<String, String> result = new HashMap<>();
                    CsvReader reader = new CsvReader(csvFile, RecipientMap.columnNames);
                    reader.read(record -> result.put(record.get(RecipientMap.columns.recipientid.name()),
                        record.get(RecipientMap.columns.directory.name())));
                    recipientsMap = result;
                } catch (IOException ignored) {
                }
            }
        }
        return recipientsMap;
    }

    /**
     * Gets a list of used components in the Program. (That is, components which have recipient
     * members.)
     *
     * Loads the Recipients as a side effect.
     *
     * @return the list of components.
     */
    public List<String> getComponents() {
        if (components == null) {
            this.components = getRecipients().stream()
                .map(r -> r.component)
                .distinct()
                .collect(Collectors.toList());
        }
        return components;
    }

    /**
     * Gets the list of Deployments defined in the Program Specification.
     * @return the Deployments.
     */
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

    public Deployment getDeployment(int deploymentNumber) {
        return getDeployments().stream()
            .filter(d -> d.deploymentnumber == deploymentNumber)
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets the content defined in the Program Specification.
     * @return an object describing the content for every Deployment, including Playlist membership.
     */
    public Content getContent() {
        if (content == null) {
            Content newContent = new Content();
            File csvFile = new File(programSpecDir, "content.csv");
            if (csvFile.exists()) {
                try (FileInputStream fis = new FileInputStream(csvFile)) {
                    CsvReader.read(fis,
                        Content.columnNames,
                        x -> newContent.addMessage(newContent.new Message(x)));
                    content = newContent;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content;
    }
}
