package org.literacybridge.core.tbdevice;

import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class TbDeviceInfoV2 extends TbDeviceInfo {


    protected TbDeviceInfoV2(TbFile tbRoot, String label) {
        super(tbRoot, label);
    }

    @Override
    public boolean isSerialNumberFormatGood(String srn) {
        return srn.equals(getSerialNumber());
    }

    @Override
    public boolean newSerialNumberNeeded() {
        return false;
    }

    @Override
    public String getSerialNumber() {
        if (serialNumber == null) {
            TbFile deviceId = tbSystem.open("device_ID.txt");
            try (InputStream fis = deviceId.openFileInputStream();
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader br = new BufferedReader(isr)) {
                serialNumber = br.readLine().trim();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return serialNumber;
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
        String firmwareVersion = null; 
        TbFile firmwareId = tbSystem.open("firmware_ID.txt");
        try (InputStream fis = firmwareId.openFileInputStream();
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            firmwareVersion = br.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return firmwareVersion;
    }

    @Override
    public DEVICE_VERSION getDeviceVersion() {
        return DEVICE_VERSION.TBv2;
    }
}
