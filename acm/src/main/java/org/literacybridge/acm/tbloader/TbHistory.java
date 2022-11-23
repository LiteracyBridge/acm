package org.literacybridge.acm.tbloader;

import org.json.simple.JSONObject;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.RecipientList.RecipientAdapter;
import org.literacybridge.core.tbloader.TbsCollected;
import org.literacybridge.core.tbloader.TbsDeployed;
import org.literacybridge.core.utils.CsvReader;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeCsv;

public class TbHistory {
    private static TbHistory instance;
    public static final String DEPLOYED_GLOBAL = "tbsdeployed-global.csv";
    public static final String COLLECTED_GLOBAL = "tbscollected-global.csv";
    public static final String DEPLOYED_LOCAL = "tbsdeployed-local.csv";
    public static final String COLLECTED_LOCAL = "tbscollected-local.csv";
    public static final String CHANGED = "tbschanged.csv";

    public enum HISTORY_SOURCE {
        NO_HISTORY, LOCAL, ONLINE;
        public boolean haveHistory() {return this != NO_HISTORY;}
    }

    private final EventListenerList listenerList = new EventListenerList();

    private final String programid;
    final ProgramSpec programSpec;
    private final File historyDir;

    private List<TbsDeployed> tbsDeployedLocal;
    private List<TbsCollected> tbsCollectedLocal;
    List<TbsDeployed> tbsDeployedGlobal;
    List<TbsCollected> tbsCollectedGlobal;
    // Some TBs have had serial number changes; map {from:to}
    private Map<String, String> tbsChanged;
    private String latestDeploymentName;
    int latestDeploymentNumber;

    private TbHistorySummarizer summarizer;
    private HISTORY_SOURCE historySource = HISTORY_SOURCE.NO_HISTORY;

    public static synchronized TbHistory getInstance() {
        if (instance == null) {instance = new TbHistory(TBLoader.getApplication().getProgram());}
        return instance;
    }

    public HISTORY_SOURCE historySource() {
        return this.historySource;
    }

    public boolean haveHistory() {
        return this.historySource.haveHistory();
    }

    public TbHistorySummarizer getSummarizer() {
        return summarizer;
    }

    public void setRelevantRecipients(Collection<RecipientAdapter> relevantRecipients) {
        if (haveHistory()) {
            summarizer.setRecipients(relevantRecipients);
            fireChangeEvent();
        }
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    private TbHistory(String programid) {
        this.programid = programid;
        historyDir = ACMConfiguration.getInstance().getPathProvider(programid).getLocalTbLoaderHistoryDir();
        programSpec = TBLoader.getApplication().getProgramSpec();
        summarizer = new TbHistorySummarizer(this, programSpec.getRecipients());
    }

    public void initializeHistory() {
        historySource = HISTORY_SOURCE.NO_HISTORY;
        try {
            latestDeploymentName = TBLoader.getApplication().getNewDeployment();
            latestDeploymentNumber = programSpec.getDeployment(latestDeploymentName).deploymentnumber;

            if (refreshGlobalHistory()) {
                historySource = HISTORY_SOURCE.ONLINE;
            } else if (loadSavedHistory()) {
                historySource = HISTORY_SOURCE.LOCAL;
            }
            if (loadLocalHistory()) {
                if (historySource == HISTORY_SOURCE.ONLINE) {
                    reconcileLocalHistory();
                }
            }
            summarizer.invalidate();
        } catch (Exception ignored) {}
    }

    private void fireChangeEvent() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
            listener.stateChanged(event);
        }
    }

    private void reconcileLocalHistory() {
        // Activity known globally
        Set<String> knownDeployments = tbsDeployedGlobal.stream()
            .map(tbd -> tbd.getTalkingbookid() + "+" + tbd.getDeployedtimestamp().toString())
            .collect(Collectors.toSet());
        Set<String> knownCollections = tbsCollectedGlobal.stream()
            .map(tbc -> tbc.getTalkingbookid() + "+" + tbc.getCollectedtimestamp().toString())
            .collect(Collectors.toSet());

        // Remove from list of locally known activity
        int size = tbsDeployedLocal.size();
        tbsDeployedLocal.removeIf(tbd -> knownDeployments.contains(tbd.getTalkingbookid() + "+" + tbd.getDeployedtimestamp()
            .toString()));
        if (tbsDeployedLocal.size() != size) {
            persistTbsDeployed();
        }

        size = tbsCollectedLocal.size();
        tbsCollectedLocal.removeIf(tbc -> knownCollections.contains(tbc.getTalkingbookid() + "+" + tbc.getCollectedtimestamp()
            .toString()));
        if (tbsCollectedLocal.size() != size) {
            persistTbsCollected();
        }

        // Build one list of (each) activity
        tbsDeployedGlobal.addAll(tbsDeployedLocal);
        tbsCollectedGlobal.addAll(tbsCollectedLocal);
    }

    private void applyTbIdChanges() {
        // What to do here?
//        Map<String, TbsCollected> collectedMap = tbsCollectedGlobal.stream().collect(Collectors.toMap(TbsCollected::getTalkingbookid, x->x));
//        Map<String, TbsDeployed> deployedMap = tbsDeployedGlobal.stream().collect(Collectors.toMap(TbsDeployed::getTalkingbookid, x->x));
//
//        // Eliminate the entries for which the talkingbookid has changed AND there is an entry for the new talingbookid.
//        // Ie, keep the entries for which the talkingbookid has NOT changed OR there is no entry for the new talkingbookid.
//        List<TbsCollected> newCollected = tbsCollectedGlobal.stream()
//            .filter(tbc -> !tbsChanged.containsKey(tbc.getTalkingbookid()) || !collectedMap.containsKey(tbsChanged.get(tbc.getTalkingbookid())))
//            .collect(Collectors.toList());
//
//        List<TbsDeployed> newDeployed = tbsDeployedGlobal.stream()
//            .filter(tbd -> !tbsChanged.containsKey(tbd.getTalkingbookid()) || !deployedMap.containsKey(tbsChanged.get(tbd.getTalkingbookid())))
//            .collect(Collectors.toList());
//
//        System.out.printf("Eliminated %d of %d collected and %d of %d deployed\n",
//            tbsCollectedGlobal.size()-newCollected.size(), tbsCollectedGlobal.size(),
//            tbsDeployedGlobal.size()-newDeployed.size(), tbsDeployedGlobal.size()
//        );

    }

    /**
     * Fetches the global deployment and collection history from server. Includes deployments and collections
     * from all users/laptops.
     *
     * @return True if the data was refreshed.
     */
    private boolean refreshGlobalHistory() {
        String host = "l0im73yun2.execute-api.us-west-2.amazonaws.com";
        String url = String.format("/prod/tb_depl_history?programid=%s", programid);
        String request = "https://" + host + url;
        long startTime = System.nanoTime();

        Authenticator authInstance = Authenticator.getInstance();
        if (!authInstance.isAuthenticated()) return false;
        JSONObject result = authInstance.getAwsInterface().authenticatedGetCall(request);
        long afterNet = System.nanoTime();

        tbsDeployedGlobal = new ArrayList<>();
        tbsCollectedGlobal = new ArrayList<>();
        tbsChanged = new HashMap<>();

        // Decode results from server
        try (StringReader tbsDeployedReader = new StringReader(result.get("tbsdeployed").toString());
             CsvReader csvReader = new CsvReader(tbsDeployedReader)) {
            for (Map<String, String> record : csvReader) {
                TbsDeployed tbsDeployed = new TbsDeployed(record);
                tbsDeployedGlobal.add(tbsDeployed);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (StringReader tbsCollectedReader = new StringReader(result.get("tbscollected").toString());
             CsvReader csvReader = new CsvReader(tbsCollectedReader)) {
            for (Map<String, String> record : csvReader) {
                TbsCollected tbsCollected = new TbsCollected(record);
                tbsCollectedGlobal.add(tbsCollected);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // [from,to] -> {from:to}
        try (StringReader tbsChangedReader = new StringReader(result.get("tbschanged").toString());
             CsvReader csvReader = new CsvReader(tbsChangedReader)) {
            for (Map<String, String> record : csvReader) {
                tbsChanged.put(record.get("from"), record.get("to"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long afterDecode = System.nanoTime();

        applyTbIdChanges();

        // Save the decoded data to local storage.
        File tbsDeployedFile = new File(historyDir, DEPLOYED_GLOBAL);
        if (!tbsDeployedFile.getParentFile().exists()) {
            tbsDeployedFile.getParentFile().mkdirs();
        }
        File tbsCollectedFile = new File(historyDir, COLLECTED_GLOBAL);
        File tbsChangedFile = new File(historyDir, CHANGED);
        try (Writer deployedWriter = new FileWriter(tbsDeployedFile);
             Writer collectedWriter = new FileWriter(tbsCollectedFile);
             Writer changedWriter = new FileWriter(tbsChangedFile)) {

            deployedWriter.write(TbsDeployed.header() + '\n');
            for (TbsDeployed tbd : tbsDeployedGlobal) {
                deployedWriter.write(tbd.toString());
                deployedWriter.write('\n');
            }

            collectedWriter.write(TbsCollected.header() + '\n');
            for (TbsCollected tbd : tbsCollectedGlobal) {
                collectedWriter.write(tbd.toString());
                collectedWriter.write('\n');
            }

            // {from:to} -> [from,to]
            changedWriter.write("from,to\n");
            for (Map.Entry<String, String> tbc : tbsChanged.entrySet()) {
                changedWriter.write(String.format("%s,%s\n", escapeCsv(tbc.getKey()), escapeCsv(tbc.getValue())));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        long afterWrite = System.nanoTime();

        // So, how long did all that take?
        System.out.printf("%d deployed, %d collected, %d changes\n",
            tbsDeployedGlobal.size(),
            tbsCollectedGlobal.size(),
            tbsChanged.size());
        System.out.printf("Total: %.3f ms, net: %.3f ms, decode: %.3f ms, save: %.3f ms\n",
            (afterWrite - startTime) / 1e6,
            (afterNet - startTime) / 1e6,
            (afterDecode - afterNet) / 1e6,
            (afterWrite - afterDecode) / 1e6);
        return true;
    }

    private boolean loadSavedHistory() {
        long startTime = System.nanoTime();
        tbsDeployedGlobal = new ArrayList<>();
        tbsCollectedGlobal = new ArrayList<>();

        try {
            File deployedFile = new File(historyDir, DEPLOYED_GLOBAL);
            if (deployedFile.exists()) {
                try (Reader tbsDeployedReader = new FileReader(deployedFile);
                     CsvReader csvReader = new CsvReader(tbsDeployedReader)) {
                    for (Map<String, String> record : csvReader) {
                        TbsDeployed tbsDeployed = new TbsDeployed(record);
                        tbsDeployedGlobal.add(tbsDeployed);
                    }
                }
            }
            File collectedFile = new File(historyDir, COLLECTED_GLOBAL);
            if (collectedFile.exists()) {
                try (Reader tbsCollectedReader = new FileReader(collectedFile);
                     CsvReader csvReader = new CsvReader(tbsCollectedReader)) {
                    for (Map<String, String> record : csvReader) {
                        TbsCollected tbsCollected = new TbsCollected(record);
                        tbsCollectedGlobal.add(tbsCollected);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            long endTime = System.nanoTime();
            System.out.printf("Load global history in %.3f ms, %d collected, %d deployed\n",
                (endTime - startTime) / 1e6,
                tbsCollectedGlobal.size(),
                tbsDeployedGlobal.size());
        }
        return true;
    }

    /**
     * Loads the local history. Note that there may be entries that also exist in the global list,
     * which need to be removed.
     *
     * @return True if the history was refreshed successfully, false otherwise.
     */
    private synchronized boolean loadLocalHistory() {
        long startTime = System.nanoTime();
        tbsDeployedLocal = new ArrayList<>();
        tbsCollectedLocal = new ArrayList<>();

        try {
            File deployedFile = new File(historyDir, DEPLOYED_LOCAL);
            if (deployedFile.exists()) {
                try (Reader tbsDeployedReader = new FileReader(deployedFile);
                     CsvReader csvReader = new CsvReader(tbsDeployedReader)) {
                    for (Map<String, String> record : csvReader) {
                        TbsDeployed tbsDeployed = new TbsDeployed(record);
                        tbsDeployedLocal.add(tbsDeployed);
                    }
                }
            }
            File collectedFile = new File(historyDir, COLLECTED_LOCAL);
            if (collectedFile.exists()) {
                try (Reader tbsCollectedReader = new FileReader(collectedFile);
                     CsvReader csvReader = new CsvReader(tbsCollectedReader)) {
                    for (Map<String, String> record : csvReader) {
                        TbsCollected tbsCollected = new TbsCollected(record);
                        tbsCollectedLocal.add(tbsCollected);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            long endTime = System.nanoTime();
            System.out.printf("Load local history in %.3f ms, %d collected, %d deployed\n",
                (endTime - startTime) / 1e6,
                tbsCollectedLocal.size(),
                tbsDeployedLocal.size());
        }
        return true;
    }

    /**
     * Writes the current local lsit of TbsDeployed to the file system.
     */
    private void persistTbsDeployed() {
        File tbsDeployedFile = new File(historyDir, DEPLOYED_LOCAL);
        if (!tbsDeployedFile.getParentFile().exists()) {
            tbsDeployedFile.getParentFile().mkdirs();
        }

        try (Writer deployedWriter = new FileWriter(tbsDeployedFile)) {
            deployedWriter.write(TbsDeployed.header() + '\n');
            for (TbsDeployed tbd : tbsDeployedLocal) {
                deployedWriter.write(tbd.toString());
                deployedWriter.write('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the current local list of TbsCollected to the file system..
     */
    private void persistTbsCollected() {
        File tbsCollectedFile = new File(historyDir, COLLECTED_LOCAL);
        if (!tbsCollectedFile.getParentFile().exists()) {
            tbsCollectedFile.getParentFile().mkdirs();
        }
        try (Writer collectedWriter = new FileWriter(tbsCollectedFile)) {

            collectedWriter.write(TbsCollected.header() + '\n');
            for (TbsCollected tbd : tbsCollectedLocal) {
                collectedWriter.write(tbd.toString());
                collectedWriter.write('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds one TbsDeployed record to the local collection.
     *
     * @param tbd The TbsDeployed record to be added.
     */
    synchronized public void addTbDeployed(TbsDeployed tbd) {
        if (haveHistory()) {
            long startTime = System.nanoTime();
            if (tbsDeployedLocal == null) {loadLocalHistory();}
            tbsDeployedGlobal.add(tbd);
            tbsDeployedLocal.add(tbd);
            persistTbsDeployed();
            long endTime = System.nanoTime();
            System.out.printf("addTbDeployed in %.3f\n", (endTime - startTime) / 1e6);
            summarizer.invalidate();
            fireChangeEvent();
        }
    }

    /**
     * Adds one TbCollected record to the local collection.
     *
     * @param tbc The TbCollected record to be added.
     */
    synchronized public void addTbCollected(TbsCollected tbc) {
        if (haveHistory()) {
            long startTime = System.nanoTime();
            if (tbsCollectedLocal == null) {loadLocalHistory();}
            tbsCollectedGlobal.add(tbc);
            tbsCollectedLocal.add(tbc);
            persistTbsCollected();
            long endTime = System.nanoTime();
            System.out.printf("addTbCollected in %.3f\n", (endTime - startTime) / 1e6);
            summarizer.invalidate();
            fireChangeEvent();
        }
    }

}
