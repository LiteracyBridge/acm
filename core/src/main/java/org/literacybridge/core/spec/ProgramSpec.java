package org.literacybridge.core.spec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
    private final String resourceDirectory;

    private List<String> components = null;
    private List<Deployment> deployments = null;
    private RecipientList recipients = null;
    private Map<String, String> recipientsMap = null;
    private ContentSpec contentSpec = null;

    public ProgramSpec(File programSpecDir) {
        this.resourceDirectory = null;
        this.programSpecDir = programSpecDir;
    }

    public ProgramSpec(String resourceDirectory) {
        if (!resourceDirectory.startsWith("/")) {
            resourceDirectory = "/" + resourceDirectory;
        }
        this.resourceDirectory = resourceDirectory;
        this.programSpecDir = null;
    }

    private InputStream getSpecStream(String filename) {
        if (programSpecDir != null) {
            File csvFile = new File(programSpecDir, filename);
            if (csvFile.exists()) {
                try {
                    return new FileInputStream(csvFile);
                } catch (FileNotFoundException e) {
                    // Ignore
                }
            }
        } else {
            return getClass().getResourceAsStream(resourceDirectory + '/' + filename);
        }
        return null;
    }

    public RecipientList getRecipients() {
        if (recipients == null) {
            try (InputStream is = getSpecStream(Recipient.FILENAME)) {
                if (is != null) {
                    final RecipientList result = new RecipientList();
                    CsvReader.read(is, Recipient.columnNames, result::add);
                    recipients = result;
                }
            } catch (IOException ignored) {
                // Just leave recipients null
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
            try (InputStream is = getSpecStream(RecipientMap.FILENAME)) {
                if (is != null) {
                    final Map<String, String> result = new HashMap<>();
                    CsvReader.read(is,
                        RecipientMap.columnNames,
                        record -> result.put(record.get(RecipientMap.columns.recipientid.name()),
                            record.get(RecipientMap.columns.directory.name())));
                    recipientsMap = result;
                }
            } catch (IOException ignored) {
            }
        }
        return recipientsMap;
    }

    /**
     * A list of languages declared for any recipient in the project.
     * @return a list (in a set) of the ISO 639-3 language codes.
     */
    public Set<String> getLanguages() {
        return getRecipients().stream().map(r -> r.language).collect(Collectors.toSet());
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
            try (InputStream is = getSpecStream(Deployment.FILENAME)) {
                if (is != null) {
                    final List<Deployment> result = new ArrayList<>();
                    CsvReader.read(is, Deployment.columnNames, record -> {
                        try {
                            result.add(new Deployment(record));
                        } catch (ParseException ignored) {
                        }
                    });
                    deployments = result;
                }
            } catch (IOException ignored) {
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
     * Gets the contentSpec defined in the Program Specification.
     * @return an object describing the content for every Deployment, including Playlist membership.
     */
    public ContentSpec getContentSpec() {
        if (contentSpec == null) {
            ContentSpec newContentSpec = new ContentSpec();
            try (InputStream fis = getSpecStream("content.csv")) {
                if (fis != null) {
                    CsvReader.read(fis,
                        ContentSpec.columnNames,
                        x -> newContentSpec.addMessage(newContentSpec.new MessageSpec(x)));
                    contentSpec = newContentSpec;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return contentSpec;
    }
}
