package org.literacybridge.acm.utils;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.AudioExporter;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.literacybridge.acm.Constants.CATEGORY_UNCATEGORIZED_FEEDBACK;
import static org.literacybridge.acm.config.AccessControlResolver.*;

/**
 * Utility to extract messages from an ACM database. In the simplest form, uncategorized user
 * feedback messages are extracted. With the argument "--categorized", everything except
 * uncategorized user feedback is extracted.
 *
 * By default, the messages are deleted after successful extraction.
 *
 * The design is from a desire for a utility to partition user feedback amongst several ACM
 * databases for processing, and to un-partition the feedback after processing. To that end, this
 * utility can be used thusly:
 *   --- user feedback is uploaded to a feedback database ---
 *   extractor --acm FB-DB --destination uncatdir
 *   importer --acm FB-DB-PARTITION uncatdir
 *   --- user feedback is categorized ---
 *   extractor --acm FB-DB-PARTITION --destination catdir --categorized
 *   importer --acm FB-DB catdir
 *   --- repeat as needed ---
 *
 * The messages to be extracted can be filtered by language and/or category id. The filters can
 * be an excludelist, an includelist, or both. To includelist a language, use "--language en", to
 * excludelist, use "--language !en". The filters are applied in the following order:
 *  -- if the language appears in the excludelist, it is excluded
 *  -- if the language appears in the includelist, it is included
 *  -- if the includelist is not empty, the language is excluded (that is, there is an includelist, but
 *      this language is not in it)
 *  -- otherwise the language is included
 * Multiple includelist and/or excludelist arguments are allowed.
 *
 * Category processing works similarly. However, messages can have multiple categories. Thus, if
 * ANY of a message's categories is excludelisted, the message is excludelisted, and if ANY of a
 * message's categories is includelisted, the message is includelisted. Excludelisting still overrides
 * includelisting. Use like --category 2-0 or --category !2-0. Multiple includelist and/or excludelist
 * arguments are allowed.
 *
 * A shortcut for "--category !9-0" is --categorized.
 *
 * Other arguments are:
 * --acm name       The ACM or project from which to extract messages.
 * --destination c  The directory into which to write the extracted messages, default "."
 * --max N          If N>1, extract at most N messages, if N<1, extract N fraction of messages
 *                  that meet the filter criteria.
 * --keep           Don't delete from the source database (ie, simply export).
 * --verbose        You know what this means, but it doesn't currently do much.
 * 
 */

public class MessageExtractor {
    private static final Logger logger = LoggerFactory.getLogger(MessageExtractor.class);

    public static void main(String[] args) throws IOException {
        new LogHelper().inDirectory("logs").withName("MessageExtractor.log").initialize();

        Params params = new Params();
        CmdLineParser parser = new CmdLineParser(params);
        try {
            parser.parseArgument(args);
        } catch (Exception e) {
            printUsage(parser);
            System.exit(100);
        }

        MessageExtractor extractor = new MessageExtractor(params);
        if (!extractor.validateCommandLineArgs()) {
            printUsage(parser);
            System.exit(100);
        }

        boolean result = extractor.doExtract();

        System.exit(result ? 0 : 4);
    }

    private final Params params;

    private String acmDirectoryName;
    private Set<String> languageIncludelist = new HashSet<>();
    private Set<String> languageExcludelist = new HashSet<>();
    private Set<String> categoryIdIncludelist = new HashSet<>();
    private Set<String> categoryIdExcludelist = new HashSet<>();
    private File destinationDirectory;

    private MessageExtractor(Params params) {
        this.params = params;
    }

    private void addItemToList(String item, Set<String> excludelist, Set<String> includelist) {
        item = item.toLowerCase();
        if (item.charAt(0) == '!') {
            excludelist.add(item.substring(1));
        } else {
            includelist.add(item);
        }
    }

    /**
     * Validates command line arguments, and prepares for processing.
     * @return true if no errors in command line arguments.
     */
    private boolean validateCommandLineArgs() {
        boolean ok = true;
        if (params.languages != null) {
            for (String language: params.languages) {
                addItemToList(language, languageExcludelist, languageIncludelist);
            }
        }
        if (params.categoryIds != null) {
            // Can not specifiy both "categorized" and specific categories.
            if (params.categorized || params.uncategorized) {
                ok = false;
                System.err.println("May not specify --category and either of --categorized or --uncategorized");
            }
            for (String categoryId: params.categoryIds) {
                addItemToList(categoryId, categoryIdExcludelist, categoryIdIncludelist);
            }
        } else {
            // No explicit categories specified, so use GENERAL_FEEDBACK ("9-0")
            if (params.categorized && params.uncategorized) {
                ok = false;
                System.err.println("May not specify both --categorized and --uncategorized");
            } else if (params.categorized) {
                categoryIdExcludelist.add(CATEGORY_UNCATEGORIZED_FEEDBACK);
            } else if (params.uncategorized){
                categoryIdIncludelist.add(CATEGORY_UNCATEGORIZED_FEEDBACK);
            }
        }
        destinationDirectory = new File(params.destination);
        if (!destinationDirectory.exists()) {
            ok = false;
            System.err.println(
                String.format("Directory '%s' does not exist", params.destination));
        }
        if (!destinationDirectory.isDirectory()) {
            ok = false;
            System.err.println(
                String.format("File '%s' is not a directory", params.destination));
        }
//        if (destinationDirectory.list().length > 0) {
//            ok = false;
//            System.err.println(String.format("Directory '%s' is not empty.", params.destination));
//        }
        acmDirectoryName = ACMConfiguration.getInstance().getPathProvider(params.acmName).getProgramHomeDir().getName();

        if (ok && params.verbose) {
            System.out.println(String.format("Extract from %s to %s, %f, %s files after export",
                acmDirectoryName, destinationDirectory, params.maxFiles, params.keep?"keep":"delete"));
            if (languageIncludelist.size()>0) {
                System.out.println(String.format("Include languages: %s", languageIncludelist));
            }
            if (languageExcludelist.size()>0) {
                System.out.println(String.format("Exclude languages: %s", languageExcludelist));
            }
            if (categoryIdIncludelist.size()>0) {
                System.out.println(String.format("Include categoryIds: %s", categoryIdIncludelist));
            }
            if (categoryIdExcludelist.size()>0) {
                System.out.println(String.format("Exclude categoryIds: %s", categoryIdExcludelist));
            }
        }
        
        return ok;
    }

    /**
     * Tests whether the given language should be included in the extract.
     * @param language ISO 639-3 code of the language, like "en", or "dga".
     * @return true if the language should be included.
     */
    private boolean includeLanguage(String language) {
        // null, "no language", can't be in either list, so it is neither excluded nor included.
        // Therefore, "no language" is included if there is no includelist.
        if (language == null) return languageIncludelist.size()==0;
        
        language = language.toLowerCase();

        // If there's an excludelist, and this language is in it, don't include the language.
        if (languageExcludelist.contains(language)) {
            return false;
        }
        // If there's an includelist, include this language only if it's in it.
        if (languageIncludelist.size()>0) {
            return languageIncludelist.contains(language);
        }
        // Otherwise, include everything
        return true;
    }

    /**
     * Tests whether the categories in the given list should be extracted. If any are excluded,
     * do not extract. If there is an includelist, but none are included, do not extract.
     * @param categoryList List of category ids to examine.
     * @return true if the category list represents an item to be extracted.
     */
    private boolean includedCategory(Iterable<Category> categoryList) {
        boolean atLeastOneIncludelisted = false;
        for (Category category : categoryList) {
            String catId = category.getId();
            // If there's an excludelist, and this id is in it, don't include the category.
            if (categoryIdExcludelist.contains(catId)) {
                return false;
            }
            // If there's an includelist, and this id is in it, remember that fact.
            atLeastOneIncludelisted |= categoryIdIncludelist.contains(catId);
        }
        // No category was excluded. If at least one was includd, or there's no includelist,
        // then include the category.
        return categoryIdIncludelist.size()==0 || atLeastOneIncludelisted;
    }

    private boolean doExtract() {
        try {
            CommandLineParams acmConfigParams = new CommandLineParams();
            acmConfigParams.disableUI = true;
            acmConfigParams.sandbox = this.params.keep;
            ACMConfiguration.initialize(acmConfigParams);
            ACMConfiguration.getInstance().setCurrentDB(acmDirectoryName);
            if (!ACMConfiguration.getInstance().setCurrentDB(acmDirectoryName)) {
                AccessStatus status = ACMConfiguration.getInstance().getCurrentDB().getDbAccessStatus();
                System.out.printf("Can't open db '%s': %s.\n", acmDirectoryName, status);
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        try {
            String announcement = params.keep ? "Exporting" : "Extracting";

            // Build list of items matching the extract criteria
            List<AudioItem> toExtract = new ArrayList<>();
            Collection<AudioItem> items = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getMetadataStore()
                .getAudioItems();

            for (AudioItem item : items) {
                // use getCategoryLeavesList() ?
                if (includeLanguage(item.getLanguageCode())
                    && includedCategory(item.getCategoryList())) {
                    toExtract.add(item);
                }
            }

            int maxToExtract = toExtract.size();
            if (params.maxFiles < maxToExtract) {
                if (params.maxFiles < 1.0) {
                    maxToExtract = (int)Math.ceil(maxToExtract * params.maxFiles);
                } else {
                    maxToExtract = (int)params.maxFiles;
                }
            }
            if (params.verbose) {
                System.out.println(String.format("%s %d of %d available, %d total", announcement, maxToExtract, toExtract.size(), items.size()));
            }


            // Perform the extraction
            int count = 0;
            List<AudioItem> extracted = new ArrayList<>();
            for (AudioItem item : toExtract) {
                count++;
                List<AudioItem> oneItem = Collections.singletonList(item);
                if (params.verbose) {
                    System.out.println(String.format("%s item %d of %d: %s", announcement, count, maxToExtract, item.getId()));
                }
                AudioExporter.getInstance().export(oneItem,
                    destinationDirectory,
                    AudioItemRepository.AudioFormat.A18,
                    false /*title*/,
                    true/*id*/);
                extracted.add(item);

                if (count >= maxToExtract) {
                    break;
                }
            }

            // Unless "--keep" was specified, remove the items from the ACM database.
            if (!params.keep) {
                if (params.verbose) {
                    System.out.println("Deleting items following successful extract.");
                }
                MetadataStore metadataStore = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
                for (AudioItem item : extracted) {
                    metadataStore.deleteAudioItem(item.getId());
                    metadataStore.commit(item);
                }
                ACMConfiguration.getInstance().closeCurrentDb(ACMConfiguration.DB_CLOSE_DISPOSITION.COMMIT);
            }

        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    private static void printUsage(CmdLineParser parser) {
        System.err.println(
            "java -cp acm.jar:lib/* org.literacybridge.acm.utils.MessageExporter");
        parser.printUsage(System.err);
    }

    private static final class Params {
        @Option(name="--verbose", aliases="-v", usage="Give verbose output.")
        boolean verbose;

        @Option(name="--destination", aliases="--dest", usage="Destination directory, default '.'")
        String destination = ".";

        @Option(name="--acm", usage="The ACM or project from which to extract messages.", metaVar="ACM", required=true)
        String acmName;

        @Option(name="--language", aliases="-l", usage="Language(s) to extract, ISO 639-3.", metaVar="code")
        List<String> languages;

        @Option(name="--category", aliases="-c", usage="Category(ies) to extract (eg '9-0') or to not extract (eg '!9-0').", metaVar="CAT")
        List<String> categoryIds;

        @Option(name="--categorized", usage="Means '--category !9-0'. Not allowed with '--category' or '--uncategorized' option.")
        Boolean categorized=false;

        @Option(name="--uncategorized", usage="Means '--category 9-0'. Not allowed with '--category' or '--categorized' option.")
        Boolean uncategorized=false;

        @Option(name="--max", usage="Maximum # of files to extract", metaVar="#files")
        float maxFiles = Integer.MAX_VALUE;

        @Option(name="--keep", usage="Keep message after extraction, default false. This implements an 'export' operation.")
        Boolean keep=false;
    }

}
