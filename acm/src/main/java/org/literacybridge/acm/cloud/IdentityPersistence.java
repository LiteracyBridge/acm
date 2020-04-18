package org.literacybridge.acm.cloud;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class IdentityPersistence {
    private static final String DEFAULT_CREDENTIALS_NAME = "credentials.info";

    private final File credentialsFile;

    public IdentityPersistence() {
        ACMConfiguration acmConfiguration = ACMConfiguration.getInstance();
        credentialsFile = new File(acmConfiguration.getApplicationHomeDirectory(),
            DEFAULT_CREDENTIALS_NAME);
    }

    /**
     * Obfuscate a string based on another string.
     * @param str The string to obfuscate.
     * @param key The string with which to obfuscate.
     * @param unRotate If true, un-obfuscate.
     * @return The obfuscated (or un-obfuscated) string.
     */
    private static String rotate(String str, String key, boolean unRotate) {
        int m = unRotate ? -1 : 1;
        char[] strChars = str.toCharArray();
        char[] keyChars = key.toCharArray();
        int ik = 0;
        for (int is = 0; is < strChars.length; is++) {
            strChars[is] = (char)(strChars[is] + keyChars[ik] * m);
            if (++ik == keyChars.length) ik = 0;
        }
        return new String(strChars);
    }

    /**
     * Persists sign in credentials to some persistent store. Intended to be called after a
     * successful sign-in.
     * @param user The user id.
     * @param password The password.
     * @return True if the credentials were saved successfully, false otherwise.
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean saveSignInDetails(String user, String email, String password, Map<String,String> extraProperties) {
        // This is not intended to be "secure". It simply prevents casual browsing of the
        // password.
        Properties credentialProps = new Properties();
        credentialProps.setProperty("identity", user);
        credentialProps.setProperty("email", email);
        if (StringUtils.isNotEmpty(password)) {
            credentialProps.setProperty("secret", rotate(password, user, false));
        }
        extraProperties.forEach((k,v) -> credentialProps.put("@"+k, v));

        return writePropertiesFile(credentialProps);
    }

    @SuppressWarnings("UnusedReturnValue")
    boolean saveProjectList(Collection<String> projects) {
        String joined = String.join(",", projects);
        Properties props = readPropertiesFile();
        if (props != null) {
            props.setProperty("$projects", joined);
            return writePropertiesFile(props);
        }
        return false;
    }

    Collection<String> retrieveProjectList() {
        Properties props = readPropertiesFile();
        if (props != null) {
            String joined = props.getProperty("$projects", "");
            return Arrays.asList(joined.split(","));
        }
        return new ArrayList<>();
    }

    /**
     * Gets the extra properties for the user. These are the properties returned from
     * the authentication call, with things such as phone number, or custom greeting.
     * @return a Map of the key-value pairs.
     */
    Map<String,String> getExtraProperties() {
        Map<String, String> result = new HashMap<>();
        Properties credentialProps = readPropertiesFile();
        if (credentialProps != null) {
            credentialProps.forEach((k, v) -> {
                String ks = (String)k;
                String vs = (String)v;
                if (ks.charAt(0) == '@') result.put(ks.substring(1), vs);
            });
        }
        return result;
    }

    /**
     * Reads credentials from some persistent store. (Ideally, this would be the OS keychain,
     * but, oy, there's no portable access to that, and our security requirements aren't
     * THAT stringent.
     * @return a Pair, consisting of the user id and password.
     */
    SigninDetails retrieveSignInDetails() {
        SigninDetails result = null;
        Properties credentialProps = readPropertiesFile();
        if (credentialProps != null) {
            String user = credentialProps.getProperty("identity", "");
            String email = credentialProps.getProperty("email", "");
            String pwd = credentialProps.getProperty("secret", "");
            pwd = rotate(pwd, user, true);
            result = new SigninDetails(user, email, pwd);
        }
        return result;
    }

    /**
     * Reads a saved set of user info from the disk.
     * @return a Properties object with the user info.
     */
    private Properties readPropertiesFile() {
        if (!credentialsFile.exists()) return null;
        Properties result = null;

        try (FileInputStream fis = new FileInputStream(credentialsFile);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            Properties credentialProps = new Properties();
            credentialProps.load(isr);
            result = credentialProps;
        } catch (Exception e) {
            // ignore
        }
        return result;
    }

    private boolean writePropertiesFile(Properties props) {
        try (FileOutputStream fos = new FileOutputStream(credentialsFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            props.store(osw, null);
        } catch (Exception e) {
            // Ignore.
            return false;
        }
        return true;
    }

    /**
     * Removes any saved sign-in credentials.
     */
    void clearSignInDetails() {
        //noinspection ResultOfMethodCallIgnored
        credentialsFile.delete();
    }

    public static class SigninDetails {
        public final String identity;
        public final String email;
        public final String secret;

        public SigninDetails(String identity, String email, String secret) {
            this.identity = identity;
            this.email = email;
            this.secret = secret;
        }
    }
}
