package org.literacybridge.acm.cloud;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.literacybridge.acm.config.ACMConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


public class UfKeyHelper {
    private static final String UF_KEY_INFO_NAME = "uf_key.info";
    private final File ufKeyInfoFile;
    private final File ufKeyInfoFileNew;

    private final Authenticator authInstance = Authenticator.getInstance();
    private final String programid;
    private final Properties ufKeyStore;

    /**
     * Manages the local store of public keys for the given program.
     *
     * @param programid for which to retrieve keys.
     */
    public UfKeyHelper(String programid) {
        this.programid = programid;
        // There is no need to sandbox these files, because they are essentially append-only.
        ufKeyInfoFile = new File(ACMConfiguration.getInstance()
            .getPathProvider(programid).getProgramHomeDir(), UF_KEY_INFO_NAME);
        ufKeyInfoFileNew = new File(ACMConfiguration.getInstance()
            .getPathProvider(programid).getProgramHomeDir(), UF_KEY_INFO_NAME + ".new");

        ufKeyStore = loadPropertiesFile();
    }

    public byte[] getPublicKeyFor(int deployment_num) {
        byte[] keyBytes = null;
        String deployment_str = Integer.toString(deployment_num);
        String encodedKey = ufKeyStore.getProperty(deployment_str);
        if (encodedKey == null) {
            String baseURL = Authenticator.UF_KEYS_HELPER_API;
            String requestURL = baseURL + "/publickey?programid=" + programid + "&deployment_num=" + deployment_str;
            JSONObject jsonResponse = authInstance.getAwsInterface().authenticatedGetCall(requestURL);
            if (jsonResponse != null && jsonResponse.containsKey("public_key")) {
                encodedKey = jsonResponse.get("public_key").toString();
                ufKeyStore.setProperty(deployment_str, encodedKey);
                storePropertiesFile(ufKeyStore);
            }
        }
        if (encodedKey != null) {
            keyBytes = Base64.decodeBase64(encodedKey.getBytes(StandardCharsets.UTF_8));
        }
        return keyBytes;
    }

    /**
     * Loads the UfKeyStore from the properties file.
     *
     * @return the Properties object, or empty properties if none or it can't be read.
     */
    private Properties loadPropertiesFile() {
        Properties ufKeyInfo = new Properties();
        if (ufKeyInfoFile.exists()) {
            Properties newUfKeyInfo = new Properties();
            try (FileInputStream fis = new FileInputStream(ufKeyInfoFile);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                newUfKeyInfo.load(isr);
                ufKeyInfo = newUfKeyInfo;
            } catch (Exception ignored) {
                // ignore and return empty properties
            }
        }
        return ufKeyInfo;
    }

    /**
     * Store the given properties file to the UF_KEY_INFO_NAME file (known internally as UfKeyStore).
     *
     * @param ufKeyInfo the Properties to write.
     * @return true if it was successfully saved, false otherwise.
     */
    private boolean storePropertiesFile(Properties ufKeyInfo) {
        boolean ok = false;
        if (ufKeyInfoFileNew.exists()) {
            //noinspection ResultOfMethodCallIgnored
            ufKeyInfoFileNew.delete();
        }
        try (FileOutputStream fos = new FileOutputStream(ufKeyInfoFileNew);
             OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            ufKeyInfo.store(osw, null);
            osw.flush();
            osw.close();
            //noinspection ResultOfMethodCallIgnored
            ufKeyInfoFile.delete();
            ok = ufKeyInfoFileNew.renameTo(ufKeyInfoFile);
        } catch (IOException ignored) {
            // Ignore and keep "false"
        }
        return ok;
    }


}
