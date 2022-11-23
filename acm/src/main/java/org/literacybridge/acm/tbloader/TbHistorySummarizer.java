package org.literacybridge.acm.tbloader;

import org.literacybridge.core.spec.RecipientList.RecipientAdapter;
import org.literacybridge.core.tbloader.TbOperation;
import org.literacybridge.core.tbloader.TbsCollected;
import org.literacybridge.core.tbloader.TbsDeployed;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TbHistorySummarizer {
    private final TbHistory tbHistory;
    private Integer numSpecTbs;
    private Integer totalDeployed;
    private Integer numCollected;
    private Integer numToCollect;
    private Integer numDeployedLatest;
    private Integer numToDeploy;

    private Map<String, TbsDeployed> tbsDeployedAll;
    private Map<String, TbsDeployed> tbsDeployedLatest;
    private Map<String, TbsCollected> tbsCollected;
    private Map<String, TbOperation> latestOpPerTb;
    private Map<String, Integer> numTbsDeployedLatestPerRecipient;
    private Map<String, Integer> toDeployPerRecipient;
    private Map<String, Integer> toCollectPerRecipient;

    private Set<String> relevantRecipientIds;

    TbHistorySummarizer(TbHistory tbHistory, Collection<RecipientAdapter> relevantRecipients) {
        this.tbHistory = tbHistory;
        if (relevantRecipients == null) {
            this.relevantRecipientIds = new HashSet<>();
        } else {
            this.relevantRecipientIds = relevantRecipients.stream()
                .map(RecipientAdapter::getRecipientid)
                .collect(Collectors.toSet());
        }
    }

    void setRecipients(Collection<RecipientAdapter> relevantRecipients) {
        this.relevantRecipientIds = relevantRecipients.stream()
            .map(RecipientAdapter::getRecipientid)
            .collect(Collectors.toSet());
        invalidate();
    }

    void invalidate() {
        numSpecTbs = totalDeployed = numCollected = numToCollect = numDeployedLatest = numToDeploy = null;
        tbsDeployedAll = tbsDeployedLatest = null;
        tbsCollected = null;
        latestOpPerTb = null;
        numTbsDeployedLatestPerRecipient = toDeployPerRecipient = toCollectPerRecipient = null;
    }

    public List<RecipientAdapter> getRelevantRecipients() {
        return relevantRecipientIds.stream().map(r->tbHistory.programSpec.getRecipients().getRecipient(r))
            .collect(Collectors.toList());
    }

    public Integer getNumSpecTbs() {
        if (numSpecTbs == null) {
            numSpecTbs = tbHistory.programSpec.getRecipients().stream()
                .filter(r->relevantRecipientIds.contains(r.getRecipientid()))
                .map(RecipientAdapter::getNumtbs)
                .reduce(0, Integer::sum);
        }
        return numSpecTbs;
    }

    public Integer getTotalDeployed() {
        if (totalDeployed == null) {
            totalDeployed = getLatestOpPerTb().size();
        }
        return totalDeployed;
    }

    public Integer getNumCollected() {
        if (numCollected == null) {
            numCollected = (int) getLatestOpPerTb().values().stream()
                .filter(tbo -> tbo instanceof TbsCollected)
                .count();
        }
        return numCollected;
    }

    public Integer getNumToCollect() {
        if (numToCollect == null) {
            numToCollect = getToCollectPerRecipient().values().stream().reduce(0, Integer::sum);
        }
        return numToCollect;
    }

    public Integer getNumDeployedLatest() {
        if (numDeployedLatest == null) {
            numDeployedLatest = getNumTbsDeployedLatestPerRecipient().values().stream()
                .reduce(0, Integer::sum);
        }
        return numDeployedLatest;
    }

    public Integer getNumToDeploy() {
        if (numToDeploy == null) {
            numToDeploy = getToDeployPerRecipient().values().stream().reduce(0, Integer::sum);
        }
        return numToDeploy;
    }

    public Map<String, TbsCollected> getTbsCollected() {
        if (tbsCollected == null) {
            tbsCollected = new HashMap<>();
            getLatestOpPerTb().values().stream()
                .filter(tbo -> tbo instanceof TbsCollected)
                .forEach(tbc -> tbsCollected.put(tbc.getTalkingbookid(), (TbsCollected)tbc));
        }
        return tbsCollected;
    }

    /**
     * Helper function to get the latest operation for every TB in the program, filteed by a subset of recipientids.
     *
     * @return a map of {talkingbookId: TbOperation} of the lateset operation performed on the Talking Books.
     */
    private Map<String, TbOperation> getLatestOpPerTb() {
        // This gets the most recent deployment for every TB.
        if (latestOpPerTb == null) {
            latestOpPerTb = getTbsDeployedAll().values().stream()
                .collect(Collectors.toMap(TbsDeployed::getTalkingbookid, tbd -> tbd));
            // If the later operation for the TB was a collection, add it or replace the deployment.
            tbHistory.tbsCollectedGlobal.stream()
                .filter(tbc -> relevantRecipientIds.contains(tbc.getRecipientid()))
                .forEach(tbc -> {
                    TbOperation latestOp = latestOpPerTb.get(tbc.getTalkingbookid());
                    if (latestOp == null || latestOp.compareTo(tbc) < 0) {
                        latestOpPerTb.put(tbc.getTalkingbookid(), tbc);
                    }
                });
        }
        return latestOpPerTb;
    }

    /**
     * Gets all TBs that have ever been deployed in the program, latest
     * TbsDeployed record for each one.
     *
     * @return a Map of {talkingbookid : TbsDeployed}
     */
    public Map<String, TbsDeployed> getTbsDeployedAll() {
        if (tbsDeployedAll == null) {
            tbsDeployedAll = new HashMap<>();
            tbHistory.tbsDeployedGlobal.stream()
                .filter(tbd -> relevantRecipientIds.contains(tbd.getRecipientid()))
                .forEach(tbd -> {
                    TbOperation latestOp = tbsDeployedAll.get(tbd.getTalkingbookid());
                    if (latestOp == null || latestOp.compareTo(tbd) < 0) {
                        tbsDeployedAll.put(tbd.getTalkingbookid(), tbd);
                    }
                });
        }
        return tbsDeployedAll;
    }

    /**
     * Gets the TBs that have been updated with the latest deployment, latest
     * TbsDeployed record for each one.
     *
     * @return a Map of {talkingbookid : TbsDeployed}
     */
    public Map<String, TbsDeployed> getTbsDeployedLatest() {
        if (tbsDeployedLatest == null) {
            tbsDeployedLatest = new HashMap<>();
            tbHistory.tbsDeployedGlobal.stream()
                .filter(tbd -> relevantRecipientIds.contains(tbd.getRecipientid()))
                .filter(tbd -> tbHistory.programSpec.getDeployment(tbd.getDeployment()) != null &&
                    tbHistory.programSpec.getDeployment(tbd.getDeployment()).deploymentnumber == tbHistory.latestDeploymentNumber)
                .forEach(tbd -> {
                    TbOperation latestOp = tbsDeployedLatest.get(tbd.getTalkingbookid());
                    if (latestOp == null || latestOp.compareTo(tbd) < 0) {
                        tbsDeployedLatest.put(tbd.getTalkingbookid(), tbd);
                    }
                });
        }
        return tbsDeployedLatest;
    }

    /**
     * Gets the number of TBs updated with the latest deployment, per recipientid.
     *
     * @return a Map of {recipientid: integer} of TBs updated with latest deployment.
     */
    public Map<String, Integer> getNumTbsDeployedLatestPerRecipient() {
        if (numTbsDeployedLatestPerRecipient == null) {
            numTbsDeployedLatestPerRecipient = new HashMap<>();
            Map<String, TbsDeployed> tbsDeployedLatest = getTbsDeployedLatest();
            tbsDeployedLatest.values().forEach(tbd -> {
                Integer n = numTbsDeployedLatestPerRecipient.getOrDefault(tbd.getRecipientid(), 0) + 1;
                numTbsDeployedLatestPerRecipient.put(tbd.getRecipientid(), n);
            });
        }
        return numTbsDeployedLatestPerRecipient;
    }

    /**
     * @return a Map of {recipientid : numRemainingToDeploy} of recipients with pending deployments.
     */
    public synchronized Map<String, Integer> getToDeployPerRecipient() {
        if (toDeployPerRecipient == null) {
            // Start with the #TBs per recip, per the program spec.
            toDeployPerRecipient = relevantRecipientIds.stream()
                .collect(Collectors.toMap(id -> id,
                    id -> tbHistory.programSpec.getRecipients().getRecipient(id).getNumtbs()));
            // Reduce by the ones already deployed.
            getTbsDeployedLatest().values()
                .forEach(tbd -> {
                    Integer n = toDeployPerRecipient.getOrDefault(tbd.getRecipientid(), 0);
                    n = Math.max(0, n - 1);
                    toDeployPerRecipient.put(tbd.getRecipientid(), n);
                });
        }
        return toDeployPerRecipient;
    }

    /**
     * Gets the recipientids and number of TBs still pending collection.
     *
     * @return a Map of {recipientid: numRemainingToColledt} of recipients with pending collections.
     */
    public Map<String, Integer> getToCollectPerRecipient() {
        if (toCollectPerRecipient == null) {
            toCollectPerRecipient = new HashMap<>();
            // If the latest operation on a TB was a deployment, that TB needs collecting.
            getLatestOpPerTb().values()
                .forEach(tbo -> {
                    if (tbo instanceof TbsDeployed) {
                        Integer n = toCollectPerRecipient.getOrDefault(tbo.getRecipientid(), 0) + 1;
                        toCollectPerRecipient.put(tbo.getRecipientid(), n);
                    }
                });
        }
        return toCollectPerRecipient;
    }

}
