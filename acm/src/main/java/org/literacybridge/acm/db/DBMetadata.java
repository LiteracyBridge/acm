package org.literacybridge.acm.db;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import org.literacybridge.acm.store.LBMetadataIDs;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStatisticsField;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.RFC3066LanguageCode;

public class DBMetadata extends Metadata {
    private PersistentMetadata mMetadata;

    private boolean isRefreshing = false;

    public DBMetadata() {
        super();
        mMetadata = new PersistentMetadata();
    }

    public DBMetadata(PersistentMetadata metadata) {
        super();
        mMetadata = metadata;
        refreshFieldsFromPersistenceObject();
    }

    public Integer getId() {
        return mMetadata.getId();
    }

    @Override
    public <F> void setMetadataField(MetadataField<F> field, MetadataValue<F> value) {
        super.setMetadataField(field, value);
        addMetadataToPersistenceObject(field, value);
    }

    @Override
    public void setStatistic(MetadataStatisticsField statisticsField, String deviceId, int bootCycleNumber, Integer count) {
        // look for existing statistics
        boolean found = false;
        for (PersistentAudioItemStatistic statistic : mMetadata.getPersistentAudioItemStatistics()) {
            if (statistic.getDeviceID().equals(deviceId)) {
                // only update if the new bootCycleNumber is higher
                if (bootCycleNumber >= statistic.getBootCycleNumber()) {
                    statistic.setStatistic(statisticsField, count);
                    statistic.setBootCycleNumber(bootCycleNumber);
                }
                found = true;
                break;
            }
        }
        if (!found) {
            PersistentAudioItemStatistic statistic = new PersistentAudioItemStatistic(deviceId, bootCycleNumber);
            statistic.setStatistic(statisticsField, count);
            mMetadata.addPersistentAudioItemStatistic(statistic);
        }
    }

    public Integer getStatistic(MetadataStatisticsField statisticsField) {
        Integer sum = 0;
        for (PersistentAudioItemStatistic statistic : mMetadata.getPersistentAudioItemStatistics()) {
            sum += statistic.getStatistic(statisticsField);
        }
        return sum;
    }

    public void removeStatistic(MetadataStatisticsField statisticsField, String deviceId) {
        for (PersistentAudioItemStatistic statistic : mMetadata.getPersistentAudioItemStatistics()) {
            if (statistic.getDeviceID().equals(deviceId)) {
                statistic.setStatistic(statisticsField, 0);
                statistic.commit();
                return;
            }
        }
    }


    public Metadata commit() {
        mMetadata = mMetadata.<PersistentMetadata> commit();
        return this;
    }

    @Override
    public Metadata commit(EntityManager em) {
        mMetadata = mMetadata.<PersistentMetadata> commit(em);
        return this;
    }

    public void destroy() {
        mMetadata.destroy();
    }

    public Metadata refresh() {
        mMetadata = mMetadata.<PersistentMetadata> refresh();
        refreshFieldsFromPersistenceObject();
        return this;
    }

    private <F> void addMetadataToPersistenceObject(MetadataField<F> field,
            MetadataValue<F> value) {
        if (isRefreshing == true) {
            return;
        }
        if (field == MetadataSpecification.DC_IDENTIFIER) {
            mMetadata.setDc_identifier(value.getValue().toString());
        } else if (field == MetadataSpecification.DC_PUBLISHER) {
            mMetadata.setDc_publisher(value.getValue().toString());
        } else if (field == MetadataSpecification.DC_RELATION) {
            mMetadata.setDc_relation(value.getValue().toString());
        } else if (field == MetadataSpecification.DC_SOURCE) {
            mMetadata.setDc_source(value.getValue().toString());
        } else if (field == MetadataSpecification.DC_TITLE) {
            mMetadata.setDc_title(value.getValue().toString());
        } else if (field == MetadataSpecification.DTB_REVISION) {
            mMetadata.setDtb_revision(value.getValue().toString());
        } else if (field == MetadataSpecification.DC_LANGUAGE) {
            // TODO ugly, needs to be implemented properly
            mMetadata.setPersistentLocale(mMetadata
                    .getPersistentLocalizedAudioItem().getPersistentLocale());
        } else if (field == MetadataSpecification.LB_DURATION) {
            mMetadata.setDuration(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_MESSAGE_FORMAT) {
            mMetadata.setMessage_format(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_TARGET_AUDIENCE) {
            mMetadata.setTarget_audience(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_DATE_RECORDED) {
            mMetadata.setDate_recorded(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_KEYWORDS) {
            mMetadata.setKeywords(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_TIMING) {
            mMetadata.setTiming(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_PRIMARY_SPEAKER) {
            mMetadata.setPrimary_speaker(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_GOAL) {
            mMetadata.setGoal(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_ENGLISH_TRANSCRIPTION) {
            mMetadata.setEnglish_transcription(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_NOTES) {
            mMetadata.setNotes(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_BENEFICIARY) {
            mMetadata.setBeneficiary(value.getValue().toString());
        } else if (field == MetadataSpecification.LB_STATUS) {
            mMetadata.setNoLongerUsed((Integer) value.getValue());
        }
    }

    private void refreshFieldsFromPersistenceObject() {
        try {
            isRefreshing = true;
            this.clear();
            setMetadataField(MetadataSpecification.DC_IDENTIFIER,
                    new MetadataValue<String>(mMetadata.getDc_identifier()));
            setMetadataField(MetadataSpecification.DC_PUBLISHER,
                    new MetadataValue<String>(mMetadata.getDc_publisher()));
            setMetadataField(MetadataSpecification.DC_RELATION,
                    new MetadataValue<String>(mMetadata.getDc_relation()));
            setMetadataField(MetadataSpecification.DC_SOURCE,
                    new MetadataValue<String>(mMetadata.getDc_source()));
            setMetadataField(MetadataSpecification.DC_TITLE,
                    new MetadataValue<String>(mMetadata.getDc_title()));
            setMetadataField(MetadataSpecification.DTB_REVISION,
                    new MetadataValue<String>(mMetadata.getDtb_revision()));
            setMetadataField(MetadataSpecification.LB_DATE_RECORDED,
                    new MetadataValue<String>(mMetadata.getDate_recorded()));
            setStatisticsField(MetadataSpecification.LB_APPLY_COUNT);
            setStatisticsField(MetadataSpecification.LB_COMPLETION_COUNT);
            setStatisticsField(MetadataSpecification.LB_COPY_COUNT);
            setStatisticsField(MetadataSpecification.LB_OPEN_COUNT);
            setStatisticsField(MetadataSpecification.LB_SURVEY1_COUNT);
            setStatisticsField(MetadataSpecification.LB_NOHELP_COUNT);
            setMetadataField(MetadataSpecification.DC_LANGUAGE,
                    new MetadataValue<RFC3066LanguageCode>(
                            (mMetadata.getPersistentLocale() == null || mMetadata.getPersistentLocale().getLanguage() == null)
                            ? null : new RFC3066LanguageCode(mMetadata.getPersistentLocale().getLanguage())));
            setMetadataField(MetadataSpecification.LB_DURATION,
                    new MetadataValue<String>(mMetadata.getDuration()));
            setMetadataField(MetadataSpecification.LB_MESSAGE_FORMAT,
                    new MetadataValue<String>(mMetadata.getMessage_format()));
            setMetadataField(MetadataSpecification.LB_TARGET_AUDIENCE,
                    new MetadataValue<String>(mMetadata.getTarget_audience()));
            setMetadataField(MetadataSpecification.LB_KEYWORDS,
                    new MetadataValue<String>(mMetadata.getKeywords()));
            setMetadataField(MetadataSpecification.LB_TIMING,
                    new MetadataValue<String>(mMetadata.getTiming()));
            setMetadataField(MetadataSpecification.LB_PRIMARY_SPEAKER,
                    new MetadataValue<String>(mMetadata.getPrimary_speaker()));
            setMetadataField(MetadataSpecification.LB_GOAL,
                    new MetadataValue<String>(mMetadata.getGoal()));
            setMetadataField(MetadataSpecification.LB_ENGLISH_TRANSCRIPTION,
                    new MetadataValue<String>(mMetadata.getEnglish_transcription()));
            setMetadataField(MetadataSpecification.LB_NOTES,
                    new MetadataValue<String>(mMetadata.getNotes()));
            setMetadataField(MetadataSpecification.LB_BENEFICIARY,
                    new MetadataValue<String>(mMetadata.getBeneficiary()));
            setMetadataField(MetadataSpecification.LB_STATUS,
                    new MetadataValue<Integer>(mMetadata.getNoLongerUsed()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            isRefreshing = false;
        }
    }

    private final void setStatisticsField(MetadataStatisticsField statisticsField) {
        int count = getStatistic(statisticsField);
        setMetadataField(statisticsField, count == 0 ? null : new MetadataValue<Integer>(count));
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<MetadataField<?>> fieldsIterator = LBMetadataIDs.FieldToIDMap.keySet().iterator();
        while (fieldsIterator.hasNext()) {
            MetadataField<?> field = fieldsIterator.next();
            String valueList = getCommaSeparatedList(this, field);
            if (valueList != null) {
                builder.append(field.getName() + " = " + valueList + "\n");
            }
        }
        return builder.toString();
    }

    public static <T> String getCommaSeparatedList(Metadata metadata, MetadataField<T> field) {
        StringBuilder builder = new StringBuilder();
        List<MetadataValue<T>> fieldValues = metadata.getMetadataValues(field);
        if (fieldValues != null) {
            for (int i = 0; i < fieldValues.size(); i++) {
                builder.append(fieldValues.get(i).getValue());
                if (i != fieldValues.size() - 1) {
                    builder.append(", ");
                }
            }
            return builder.toString();
        } else {
            return null;
        }
    }

}
