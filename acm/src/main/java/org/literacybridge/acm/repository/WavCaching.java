package org.literacybridge.acm.repository;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.BackgroundTaskManager.ExtendedSwingWorker;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;

public class WavCaching {

    private Set<AudioItem> uncachedAudioItems = new HashSet<AudioItem>();

    public void cacheNewA18Files() {
        findUncachedWaveFiles();
        Application.getApplication().getTaskManager().execute(new Task());
    }

    private void findUncachedWaveFiles() {
        System.out.print("Finding uncached wave files...");
        AudioItemRepository repository = ACMConfiguration.getCurrentDB()
                .getRepository();

        for (AudioItem audioItem : AudioItem.getFromDatabase()) {
            if (!repository.hasAudioItemFormat(audioItem, AudioFormat.WAV)) {
                uncachedAudioItems.add(audioItem);
            }
        }
        System.out.println("done");
    }

    private class Task extends ExtendedSwingWorker<Void, Void> {
        String itemBeingProcessed;

        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            int progress = 0;
            // Initialize progress property.
            setProgress(0);
            Iterator<AudioItem> it = uncachedAudioItems.iterator();
            while (it.hasNext() && !isCancelled()) {
                AudioItem item = it.next();
                try {
                    itemBeingProcessed = item.getUuid();
                    System.out.println("Converting " + itemBeingProcessed);
                    ACMConfiguration.getCurrentDB().getRepository()
                    .convert(item, AudioFormat.WAV);
                } catch (ConversionException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                progress++;
                setProgress((int) ((float) progress / uncachedAudioItems.size() * 100.0));
            }
            itemBeingProcessed = "done";
            return null;
        }

        @Override
        public String toString() {
            String s = "Converting ";
            if (itemBeingProcessed != null) {
                s += itemBeingProcessed;
            }
            return s;
        }
    }
}
