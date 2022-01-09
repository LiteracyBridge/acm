package org.literacybridge.core.tbdevice;

import org.literacybridge.core.tbloader.TBLoaderConstants;

public class TbDeviceInfoUnknown extends TbDeviceInfo {
    protected TbDeviceInfoUnknown() {
        super(null, "Can't determine device");
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
