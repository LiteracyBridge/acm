package org.literacybridge.core.tbdevice;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.core.fs.TbFile;

public class TbDeviceInfoUnknown extends TbDeviceInfo {
    protected TbDeviceInfoUnknown(TbFile tbRoot, String label) {
        super(tbRoot, StringUtils.defaultIfBlank(label, "Can't determine device"));
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
