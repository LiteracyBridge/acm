package org.literacybridge.core.spec;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Program Specification wrapper.
 */
public class ProgramSpec {
    public static final String DEPLOYMENT_INFO_PROPERTIES_NAME = "deployment_info.properties";
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public static final String DEPLOYMENT_INFO_PROPERTIES_NAME_OLD = "deployment.properties";

    private final File programSpecDir;
    private final String resourceDirectory;

    private List<String> components = null;
    private List<Deployment> deployments = null;
    private RecipientList recipients = null;
    private Map<String, String> recipientsMap = null;
    private ContentSpec contentSpec = null;
    private Properties deploymentProperties = null;

    public ProgramSpec(File programSpecDir) {
        this.resourceDirectory = null;
        this.programSpecDir = programSpecDir;
    }

    /**
     * For testing purposes, it is possible to embed a program specification into
     * resources. This constructor gives access to those embedded program specs.
     * @param resourceDirectory containing the program specification resources.
     */
    public ProgramSpec(String resourceDirectory) {
        if (!resourceDirectory.startsWith("/")) {
            resourceDirectory = "/" + resourceDirectory;
        }
        this.resourceDirectory = resourceDirectory;
        this.programSpecDir = null;
    }

    /**
     * Opens the named program specification file or resource.
     * @param filenames one or more name of the program specification file, like "content.csv"
     * @return the first found file or resource, as a stream.
     */
    private InputStream getSpecStream(String... filenames) {
        for (String filename : filenames) {
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
                InputStream is = getClass().getResourceAsStream(resourceDirectory + '/' + filename);
                if (is != null) {
                    return is;
                }
            }
        }
        return null;
    }

    /**
     * Lazily loads the recipients from the program specification.
     * @return the RecipientList, or null if it can't be read.
     */
    public synchronized RecipientList getRecipients() {
        if (recipients == null) {
            try (InputStream is = getSpecStream(Recipient.FILENAME)) {
                if (is != null) {
                    final RecipientList result = new RecipientList();
                    Set<String> columnsInRecips = CsvReader.read(is, Recipient.columnNames, result::add);
                    result.setFoundColumns(columnsInRecips);
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
        RecipientList recipients = getRecipients();
        if (recipients.hasDeploymentsColumn()) {
            recipients.stream()
                .filter(r -> r.deployments == null || r.deployments.size()==0 || r.deployments.contains(deploymentNumber))
                .forEach(filteredRecipients::add);
        } else {
            Deployment deployment = getDeployment(deploymentNumber);
            StringFilter componentFilter = deployment.componentFilter;
            recipients.stream()
                    .filter(r -> componentFilter.test(r.component))
                    .forEach(filteredRecipients::add);
        }
        return filteredRecipients;
    }

    /**
     * Get the recipients whose components are configured to receive the given deployment, and which are
     * configured with the given language.
     * @param deploymentNumber of interest.
     * @param languagecode of interest.
     * @return the Recipients in that deployment, with that language.
     */
    public RecipientList getRecipientsForDeploymentAndLanguage(int deploymentNumber, String languagecode) {
        RecipientList filteredRecipients = new RecipientList();
        getRecipientsForDeployment(deploymentNumber).stream()
                .filter(r -> r.languagecode.equals(languagecode))
                .forEach(filteredRecipients::add);
        return filteredRecipients;
    }

    /**
     * Gets the languages configured for the recipients who should receive a given
     * deployment.
     * @param deploymentNumber of interest
     * @return a Set of the languages.
     */
    public Set<String> getLanguagesForDeployment(int deploymentNumber) {
        RecipientList recipients = getRecipientsForDeployment(deploymentNumber);
        return recipients.stream().map(r -> r.languagecode).collect(Collectors.toSet());
    }

    /**
     * Gets the variants used in a given deployment. This is the intersection of the variants
     * of the recipients of the deployment with the variants of the messages in the deployment.
     * @param deploymentNumber of interest.
     * @return a Set of the variants used in the deployment.
     */
    public Set<String> getVariantsForDeployment(int deploymentNumber) {
        RecipientList recipients = getRecipientsForDeployment(deploymentNumber);
        Set<String> recipientVariants = recipients.stream().map(r -> r.variant).collect(Collectors.toSet());
        Set<String> messageVariants = new HashSet<>();
        messageVariants.add("");
        for (ContentSpec.PlaylistSpec playlistSpec : getContentSpec().getDeployment(deploymentNumber).getPlaylistSpecs()) {
            for (ContentSpec.MessageSpec messageSpec: playlistSpec.getMessageSpecs()) {
                messageVariants.addAll(messageSpec.variantItems());
            }
        }
        messageVariants.retainAll(recipientVariants);
        return messageVariants;
    }

    /**
     * Get the variants used in a given deployment for messages of a given language. Like getVariantsForDeployment()
     * but also limited to a particular language.
     * @param deploymentNumber of interest.
     * @param languagecode of interest.
     * @return a Set of the variants in the deployment for messages in the given language.
     */
    public Set<String> getVariantsForDeploymentAndLanguage(int deploymentNumber, String languagecode) {
        RecipientList recipients = getRecipientsForDeploymentAndLanguage(deploymentNumber, languagecode);
        Set<String> recipientVariants = recipients.stream().map(r -> r.variant).collect(Collectors.toSet());
        recipientVariants.add("");
        // Find all of the message variants for that language in the deployment.
        Set<String> messageVariants = new HashSet<>();
        messageVariants.add("");
        for (ContentSpec.PlaylistSpec playlistSpec : getContentSpec().getDeployment(deploymentNumber).getPlaylistSpecsForLanguage(languagecode)) {
            for (ContentSpec.MessageSpec messageSpec: playlistSpec.getMessageSpecs()) {
                messageVariants.addAll(messageSpec.variantItems());
            }
        }
        messageVariants.retainAll(recipientVariants);
        return messageVariants;
    }

    /**
     * Gets the recipient_map, {recipientid : directoryname}
     * @return the map
     */
    public synchronized Map<String, String> getRecipientsMap() {
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
    public Set<String> getLanguageCodes() {
        return getRecipients().stream().map(r -> r.languagecode).collect(Collectors.toSet());
    }
    /**
     * Gets a list of used components in the Program. (That is, components which have recipient
     * members.)
     *
     * Loads the Recipients as a side effect.
     *
     * @return the list of components.
     */
    public synchronized List<String> getComponents() {
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
    public synchronized List<Deployment> getDeployments() {
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
    public synchronized ContentSpec getContentSpec() {
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

    /**
     * Gets the "deployment.properties" if it exists. This is part of the deployment, and is saved
     * with the program specification as of the time the deployment was created.
     * @return the Properties object. Empty if can't be found.
     */
    public synchronized Properties getDeploymentProperties() {
        if (deploymentProperties == null) {
            deploymentProperties = new Properties();

            try (InputStream fis = getSpecStream(DEPLOYMENT_INFO_PROPERTIES_NAME, DEPLOYMENT_INFO_PROPERTIES_NAME_OLD)) {
                if (fis != null) {
                    try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                        deploymentProperties.load(bis);
                    }
                }
            } catch (IOException e) {
                // Ignore and continue with empty deployment properties.
            }
        }
        return deploymentProperties;
    }
}
