package org.literacybridge.core.tbloader;

@SuppressWarnings("unused")
public class TbSrnAllocationInfo {
    public static final String TB_SRN_PRIMARY_END_NAME = "primaryend";
    public static final String TB_SRN_ID_NAME = "tbloaderid";
    public static final String TB_SRN_HEXID_NAME = "tbloaderidhex";
    public static final String TB_SRN_NEXTSRN_NAME = "nextsrn";
    public static final String TB_SRN_PRIMARY_BEGIN_NAME = "primarybegin";
    public static final String TB_SRN_BACKUP_BEGIN_NAME = "backupbegin";
    public static final String TB_SRN_BACKUP_END_NAME = "backupend";

    private int tbloaderid = -1;
    private String tbloaderidHex = null;
    private int nextSrn = 0;
    private int primaryBegin = 0;
    private int primaryEnd = 0;
    private int backupBegin = 0;
    private int backupEnd = 0;

    public TbSrnAllocationInfo() {}

    public TbSrnAllocationInfo(int tbloaderid,
                        String tbloaderidHex,
                        int nextSrn,
                        int primaryBegin,
                        int primaryEnd,
                        int backupBegin,
                        int backupEnd)
    {
        this.tbloaderid = tbloaderid;
        this.tbloaderidHex = tbloaderidHex;
        this.nextSrn = nextSrn;
        this.primaryBegin = primaryBegin;
        this.primaryEnd = primaryEnd;
        this.backupBegin = backupBegin;
        this.backupEnd = backupEnd;
    }

    /**
     * Constructor from another TbSrnInfo object.
     * @param other the other TbSrnInfo object.
     */
    public TbSrnAllocationInfo(TbSrnAllocationInfo other) {
        if (other != null) {
            this.tbloaderid = other.tbloaderid;
            this.tbloaderidHex = other.tbloaderidHex;
            this.nextSrn = other.nextSrn;
            this.primaryBegin = other.primaryBegin;
            this.primaryEnd = other.primaryEnd;
            this.backupBegin = other.backupBegin;
            this.backupEnd = other.backupEnd;
        }
    }

    /**
     * Sets the id of this block of SRNs.
     * @param tbloaderid The tbloader id, as an integer.
     * @param tbloaderidHex The tbloader id as a hex string. No leading 0x.
     */
    void setId(int tbloaderid, String tbloaderidHex) {
        this.tbloaderid = tbloaderid;
        this.tbloaderidHex = tbloaderidHex;
    }

    /**
     * Adds a block of available SRNs. Updates the tbloader id, if it has changed (that can
     * happen if we exhause the SRNs for a tbloader id).
     * @param id The decimal id returned by the allocation.
     * @param hexid Hex format of the id.
     * @param begin First allocated serial number.
     * @param end Last allocated +1 serial number (STL-ish end())
     * @return True if the allocation is well formed, false otherwise.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean applyReservation(int id, String hexid, int begin, int end) {
        if (begin > 0 && end > begin) {
            if (id != tbloaderid) {
                setId(id, hexid);
            }
            if (primaryBegin == 0 && backupBegin == 0) {
                // If we have neither block, split the block between primary and backup
                primaryBegin = begin;
                primaryEnd = begin + ((end - begin) / 2);
                backupBegin = primaryEnd;
                backupEnd = end;
                nextSrn = primaryBegin;
            } else if (primaryBegin == 0) {
                // else if we have no primary block, save block as primary
                primaryBegin = begin;
                primaryEnd = end;
                nextSrn = primaryBegin;
            } else if (backupBegin == 0) {
                // else if we have no backup block, save block as backup
                backupBegin = begin;
                backupEnd = end;
            } else {
                throw new IllegalStateException("Attempt to fill block, but no empty blocks.");
            }
            return true;
        }
        return false;
    }

    /**
     * Query if there is an available SRN.
     * @return true if there is an available SRN.
     */
    public boolean hasNext() {
        return (primaryBegin > 0 && primaryEnd > primaryBegin
            && nextSrn >= primaryBegin && nextSrn < primaryEnd);
    }

    /**
     * Query if there is a backup block.
     * @return true if there is a backup block.
     */
    public boolean hasBackup() {
        return (backupBegin > 0 && backupEnd > backupBegin);
    }

    /**
     * Allocate the next SRN. If the allocation exhausts the primary block, switch the
     * backup block. If the backup block is empty, no further allocations will be possible.
     * @return The next srn.
     */
    public int allocateNext() {
        int next = 0;
        if (primaryBegin > 0 && primaryEnd > primaryBegin && nextSrn >= primaryBegin
            && nextSrn < primaryEnd) {
            next = nextSrn++;
            // Did that exhaust the primary block?
            if (nextSrn >= primaryEnd) {
                // Yes, promote backup to primary
                primaryBegin = backupBegin;
                primaryEnd = backupEnd;
                backupBegin = 0;
                backupEnd = 0;
                nextSrn = primaryBegin;
            }
        }
        return next;
    }

    /**
     * Given a serial number as an integer, format it as a serial number string.
     * @param srn The serial number to format.
     * @return The formatted serial number, like "b-000C0123"
     */
    public String formatSrn(int srn) {
        assert(tbloaderidHex != null);
        return String.format("b-%s%04x", tbloaderidHex, srn);
    }

    /**
     * Query the number of available SRNs.
     * @return the number of available SRNs.
     */
    public int available() {
        int available = 0;
        if (primaryBegin > 0 && primaryEnd > primaryBegin) {
            available += primaryEnd - primaryBegin;
        }
        if (backupBegin > 0 && backupEnd > backupBegin) {
            available += backupEnd - backupBegin;
        }
        return available;
    }

    public String getTbloaderidHex() {
        return tbloaderidHex;
    }

    public int getTbloaderid() {
        return tbloaderid;
    }

    public int getNextSrn() {
        return nextSrn;
    }

    public int getPrimaryBegin() {
        return primaryBegin;
    }

    public int getPrimaryEnd() {
        return primaryEnd;
    }

    public int getBackupBegin() {
        return backupBegin;
    }

    public int getBackupEnd() {
        return backupEnd;
    }
}
