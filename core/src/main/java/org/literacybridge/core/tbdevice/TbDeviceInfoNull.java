package org.literacybridge.core.tbdevice;

import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.TBLoaderConstants;

public class TbDeviceInfoNull extends TbDeviceInfo {
    protected TbDeviceInfoNull() {
        super(null, TBLoaderConstants.NO_DRIVE);
    }

    @Override
    public boolean isSerialNumberFormatGood(String srn) {
        return false;
    }

    @Override
    public boolean newSerialNumberNeeded() {
        return false;
    }

    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    protected String getSynchDir() {
        return null;
    }

    @Override
    protected String getLastUpdateDate() {
        return null;
    }

    @Override
    protected String getFirmwareVersion() {
        return null;
    }

    @Override
    public DEVICE_VERSION getDeviceVersion() {
        return DEVICE_VERSION.NONE;
    }
}
