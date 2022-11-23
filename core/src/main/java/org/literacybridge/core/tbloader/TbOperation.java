package org.literacybridge.core.tbloader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public abstract class TbOperation implements Comparable<TbOperation> {
    protected enum OP {COLLECTED, DEPLOYED}

    protected static final SimpleDateFormat preferredDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    protected static final SimpleDateFormat alternateDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'");
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(?i)(\\d{4})-?(\\d{2})-?(\\d{2})[t ](\\d{2}):?(\\d{2}):?(\\d{2})(\\.\\d{1,})?.*");

    protected Date tryParseDate(String value) {
        int len = value.length();
        Date result = null;
        if (len == 20 && value.charAt(19)=='Z') {
            try {
                result = alternateDateFormat.parse(value);
            } catch (ParseException ex) {
                System.out.printf("Could not parse %s as Date, falling back to regex\n", value);
            }
        } else if (len >= 19 && len <= 23) {
            String parsed;
            if (len == 19) parsed = value + ".000";
            else if (len < 23) parsed = value + "000".substring(0, 23-len);
            else parsed = value;
            try {
                result = preferredDateFormat.parse(parsed);
            } catch (ParseException ex) {
                System.out.printf("Could not parse %s -> %s as Date, falling back to regex\n", value, parsed);
            }
        }
        if (result == null) {
            Matcher m = DATE_PATTERN.matcher(value);
            if (m.matches()) {
                String normalized = String.format("%4s-%2s-%2s %2s:%2s:%2s",
                    m.group(1),
                    m.group(2),
                    m.group(3),
                    m.group(4),
                    m.group(5),
                    m.group(6));
                String fraction = (m.groupCount() == 7 && m.group(7) != null)
                                  ? (m.group(7) + "000").substring(0, 4) // group includes period
                                  : ".000";
                normalized += fraction;
                try {
                    result = preferredDateFormat.parse(normalized);
                } catch (ParseException ignored) {}
//            System.out.printf("%s -> %s -> %s\n", value, normalized, preferredDateFormat.format(result));
            }
        }
        if (result == null) {
            System.out.printf("Could not recognize date: %s\n", value);
        }
        return result;
    }

    protected Float tryParseFloat(String maybeFloat) {
        try {
            return Float.parseFloat(maybeFloat);
        } catch (Exception ignored) {
            return null;
        }
    }

    // Methods that all operations have.
    public abstract String getTalkingbookid();
    public abstract String getRecipientid();
    public abstract String getProject();
    public abstract String getDeployment();
    public abstract String getContentpackage();
    public abstract String getFirmware();
    public abstract String getLocation();
    public abstract String getUsername();
    public abstract String getTbcdid();
    public abstract String getAction();
    public abstract String getTesting();
    public abstract Float getLatitude();
    public abstract Float getLongitude();
    
    abstract public Date getOperationTimestamp();
    abstract OP getOperation();

    @Override
    public int compareTo(TbOperation o) {
        int dateCompare = getOperationTimestamp().compareTo(o.getOperationTimestamp());
        if (dateCompare != 0) return dateCompare;
        return getOperation().ordinal() - o.getOperation().ordinal();
    }
}
