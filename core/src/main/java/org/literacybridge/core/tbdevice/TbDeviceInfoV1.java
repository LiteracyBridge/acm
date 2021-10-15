package org.literacybridge.core.tbdevice;

import static org.literacybridge.core.tbloader.TBLoaderConstants.BINARY_STATS_ALTERNATIVE_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.BINARY_STATS_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NEED_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TALKING_BOOK_ID_PROPERTY;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderUtils;
import org.literacybridge.core.tbloader.TbFlashData;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TbDeviceInfoV1 extends TbDeviceInfo {
    private static final Logger LOG = Logger.getLogger(TbDeviceInfoV1.class.getName());

    private final String tbPrefix;
    private final TbFlashData tbFlashData;
    private boolean needNewSerialNumber = false;

    protected TbDeviceInfoV1(TbFile tbRoot, String label, String prefix) {
        super(tbRoot, label);
        this.tbPrefix = prefix;
        if (tbRoot == null) {
            tbFlashData = null;
        } else {
            tbFlashData = loadFlashData(tbRoot);
            loadSerialNumber();
        }
    }

    public DEVICE_VERSION getDeviceVersion() {
        return DEVICE_VERSION.TBv1;
    }

    /**
     * Reads and parses the flashData.bin file from a Talking Book.
     *
     * @return A TbFlashData structure from the Talking Book, or null if one can't be read.
     */
    private static TbFlashData loadFlashData(TbFile tbRoot) {
        TbFlashData tbFlashData = null;
        try {
            TbFile flashDataBin = tbRoot.open(BINARY_STATS_PATH);
            if (flashDataBin.exists()) {
                tbFlashData = new TbFlashData(flashDataBin);
            } else {
                flashDataBin = tbRoot.open(BINARY_STATS_ALTERNATIVE_PATH);
                if (flashDataBin.exists()) {
                    tbFlashData = new TbFlashData(flashDataBin);
                }
            }
        } catch (IOException ex) {
            // Ignore exception; no tbinfo, so return null;
        }

        // Weird little bit of logic here. Wonder what it means...
        if (tbFlashData != null && tbFlashData.getCountReflashes() == -1) {
            tbFlashData = null;
        }

        return tbFlashData;
    }

    private void loadSerialNumber() {
        getSerialNumber();
        // See if we need to allocate a new serial number. If we do, just mark it, don't actually
        // allocate one until we're sure we'll use it.
        if (!TBLoaderUtils.isSerialNumberFormatGood2(serialNumber)) {
            // We will allocate a new-style serial number before we update the tbDeviceInfo.
            serialNumber = NEED_SERIAL_NUMBER;
        }
        needNewSerialNumber = TBLoaderUtils.newSerialNumberNeeded(tbPrefix, serialNumber);
    }

    /**
     * Looks in the TalkingBook's system directory for files with a ".rev" or ".img" extension. If
     * exactly one *.rev file exists, the basename is returned as the revision. If no .rev files,
     * but there is exactly one *.img file, that basename is returned as the revision.
     *
     * @return The file's name found (minus extension), or UNKNOWN if no file found, or if the file name consists
     * only of the extension (eg, a file named ".img" will return UNKNOWN).
     */
    protected String getFirmwareVersion() {
        String rev = TBLoaderConstants.UNKNOWN;

        if (tbSystem.exists()) {
            String[] revNames = tbSystem.list((dir, name) -> name.length() > 4 && (name.toLowerCase().endsWith(".rev")));
            if (revNames.length == 1) {
                rev = FilenameUtils.removeExtension(revNames[0]).toLowerCase();
            } else if (revNames.length == 0) {
                String[] imgNames = tbSystem.list((dir, name) -> name.length() > 4 && (name.toLowerCase().endsWith(".img")));
                if (imgNames.length == 1) {
                    rev = FilenameUtils.removeExtension(imgNames[0]).toLowerCase();
                }
            }
        }

        return rev;
    }


    /**
     * Look in the tbstats structure for updateDate. If none, parse the synchDir, if there is one.
     *
     * @return The last sync date, or UNKNOWN if not available.
     */
    protected String getLastUpdateDate() {
        String synchDir = getSynchDir();
        String lastUpdate = TBLoaderConstants.UNKNOWN;

        if (tbFlashData != null && tbFlashData.getUpdateDate() != -1)
            lastUpdate = tbFlashData.getUpdateYear() + "/" + tbFlashData.getUpdateMonth() + "/"
                + tbFlashData.getUpdateDate();
        else {
            if (synchDir != null) {
                int y = synchDir.indexOf('y');
                int m = synchDir.indexOf('m');
                int d = synchDir.indexOf('d');
                lastUpdate =
                    synchDir.substring(0, y) + "/" + synchDir.substring(y + 1, m) + "/"
                        + synchDir.substring(m + 1, d);
            }
        }

        return lastUpdate;
    }

    /**
     * Returns the cached TbInfo, if there is one, but doesn't try to read from the file system.
     *
     * @return The TbInfo, or null if there is one, or it hasn't been loaded yet.
     */
    @Override
    public TbFlashData getFlashData() {
        return tbFlashData;
    }


    /**
     * Looks in the TalkingBook's system directory for a file named "last_updated.txt",
     * and reads the first line from it. Stores any value so read into TBLoader.lastSynchDir.
     *
     * @return The "last synch dir", which has the update date and time encoded into it.
     */
    protected String getSynchDir() {
        String lastSynchDir = null;

        TbFile lastUpdate = tbSystem.open("last_updated.txt");
        if (lastUpdate.exists()) {
            try (InputStream fstream = lastUpdate.openFileInputStream();
                 DataInputStream in = new DataInputStream(fstream);
                 BufferedReader br = new BufferedReader(new InputStreamReader(in))
            ) {
                String strLine;
                if ((strLine = br.readLine()) != null) {
                    lastSynchDir = strLine;
                }
            } catch (Exception e) { //Catch and ignore exception if any
                LOG.log(Level.WARNING, "Ignoring error: ", e.getMessage());
            }
        }

        return lastSynchDir;
    }

    public boolean isSerialNumberFormatGood(String srn) {
        return TBLoaderUtils.isSerialNumberFormatGood(tbPrefix, srn);
    }
    public boolean newSerialNumberNeeded() {return needNewSerialNumber;}


    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }


    /**
     * Looks in several places for the Talking Book ID, aka Serial Number.
     * <p>
     * Lookes in deployment.properties, in the flash data, and for a file named *.srn.
     *
     * @return The the first serial number found.
     */
    public String getSerialNumber() {
        if (serialNumber == null) {
            serialNumber = getProperty(TALKING_BOOK_ID_PROPERTY);
            String src = "properties";

            // If we didn't have it in properties, look for the flash data or marker file(s).
            if (serialNumber.equalsIgnoreCase(TBLoaderConstants.UNKNOWN)) {
                if (getFlashData() != null
                    && TBLoaderUtils.isSerialNumberFormatGood(tbPrefix, getFlashData().getSerialNumber())) {
                    serialNumber = getFlashData().getSerialNumber();
                    src = "flash";
                } else {
                    serialNumber = getSerialNumberFromFileSystem(tbRoot);
                    src = "marker";
                }
            } else {
                if (getFlashData() != null
                    && TBLoaderUtils.isSerialNumberFormatGood(tbPrefix, getFlashData().getSerialNumber())) {
                    String flashSerialNumber = getFlashData().getSerialNumber();
                    if (TBLoaderUtils.isSerialNumberFormatGood2(serialNumber) &&
                        TBLoaderUtils.isSerialNumberFormatGood2(flashSerialNumber) &&
                        !serialNumber.equalsIgnoreCase(flashSerialNumber)) {
                        // Flash and properties mismatch. Do not trust either.
                        serialNumber = NEED_SERIAL_NUMBER;
                    }
                }
            }
            serialNumber = serialNumber.toUpperCase();
            LOG.log(Level.FINE, String.format("TBL!: Got serial number (%s) from %s.", serialNumber, src));
        }
        return serialNumber;
    }


}