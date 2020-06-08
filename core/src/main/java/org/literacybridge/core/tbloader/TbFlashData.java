package org.literacybridge.core.tbloader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.literacybridge.core.fs.TbFile;

public class TbFlashData {
  private static final Logger LOG = Logger.getLogger(TbFlashData.class.getName());
  private static final int MAX_MESSAGES = 40;

  // struct SystemData
  // int structType
  private boolean debug = false;
  private String serialNumber;
  private String deploymentNumber;
  private short countReflashes;
  private String community;
  private String imageName;
  private short updateDate = -1;
  private short updateMonth = -1;
  private short updateYear = -1;

  // struct SystemCounts2
  // short structType
  private short periods;
  private short cumulativeDays;
  private short corruptionDay;
  private short powerups;
  private short lastInitVoltage;
  private RotationTiming[] rotations = new RotationTiming[5];

  // struct NORmsgMap
  // short structType
  private short totalMessages;
  private String[] msgIdMap = new String[MAX_MESSAGES]; // 40 messages, 20 chars

  // struct NORallMsgStats
  private short profileOrder;
  private String profileName;
  private short profileTotalMessages;
  private short profileTotalRotations;
  private NORmsgStats[][] stats = new NORmsgStats[MAX_MESSAGES][5];
  private DataInputStream f;

  public short getCountReflashes() {
    return countReflashes;
  }

  public String getCommunity() {
    return community;
  }

  public String getDeploymentNumber() {
    return deploymentNumber;
  }

  public String getImageName() {
    return imageName;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public NORmsgStats[][] getStats() {
    return stats;
  }

  public RotationTiming[] getRotations() {
    return rotations;
  }

  public short getUpdateDate() {
    return updateDate;
  }

  public short getUpdateMonth() {
    return updateMonth;
  }

  public short getUpdateYear() {
    return updateYear;
  }

  public short getProfileTotalRotations() {
    return profileTotalRotations;
  }

  public short getPeriods() {
    return periods;
  }

  public short getCumulativeDays() {
    return cumulativeDays;
  }

  public short getCorruptionDay() {
    return corruptionDay;
  }

  public short getPowerups() {
    return powerups;
  }

  public short getLastInitVoltage() {
    return lastInitVoltage;
  }

  public short getTotalMessages() {
    return totalMessages;
  }

  public class RotationTiming {
    private final short rotationNumber;
    private final short startingPeriod;
    private final short hoursAfterLastUpdate;
    private final short initVoltage;

    private RotationTiming() throws IOException {
      f.skipBytes(2);
      // System.out.println("pointer:"+f.getFilePointer());
      this.rotationNumber = readShort();
      this.startingPeriod = readShort();
      this.hoursAfterLastUpdate = readShort();
      this.initVoltage = readShort();
      // System.out.println("pointer:"+f.getFilePointer());
    }

    public short getRotationNumber() {
      return rotationNumber;
    }

    public short getStartingPeriod() {
      return startingPeriod;
    }

    public short getHoursAfterLastUpdate() {
      return hoursAfterLastUpdate;
    }

    public short getInitVoltage() {
      return initVoltage;
    }
  }

  public class NORmsgStats {
    // short structType
    private final short indexMsg;
    private final short numberRotation;
    private final short numberProfile;
    private final short countStarted;
    private final short countQuarter;
    private final short countHalf;
    private final short countThreequarters;
    private final short countCompleted;
    private final short countApplied;
    private final short countUseless;
    private final int totalSecondsPlayed;

    private NORmsgStats() throws IOException {
      f.skipBytes(2);
      this.indexMsg = readShort();
      this.numberProfile = readShort();
      this.numberRotation = readShort();
      this.countStarted = readShort();
      this.countQuarter = readShort();
      this.countHalf = readShort();
      this.countThreequarters = readShort();
      this.countCompleted = readShort();
      this.countApplied = readShort();
      this.countUseless = readShort();
      this.totalSecondsPlayed = readUnsignedShort();
    }

    public short getIndexMsg() {
      return indexMsg;
    }

    public short getNumberRotation() {
      return numberRotation;
    }

    public short getNumberProfile() {
      return numberProfile;
    }

    public short getCountStarted() {
      return countStarted;
    }

    public short getCountQuarter() {
      return countQuarter;
    }

    public short getCountHalf() {
      return countHalf;
    }

    public short getCountThreequarters() {
      return countThreequarters;
    }

    public short getCountCompleted() {
      return countCompleted;
    }

    public short getCountApplied() {
      return countApplied;
    }

    public short getCountUseless() {
      return countUseless;
    }

    public int getTotalSecondsPlayed() {
      return totalSecondsPlayed;
    }
  }

  public TbFlashData(TbFile flashData)
      throws IOException {
    InputStream in = flashData.openFileInputStream();
    if (in == null) {
      System.out.print("No flash binary file to analyze.");
      this.countReflashes = -1;
      return;
    }
    f = new DataInputStream(in);
    f.skipBytes(2);
    this.countReflashes = readShort();
    this.serialNumber = readString(12);
    this.deploymentNumber = readString(20);
    this.community = readString(40);
    this.imageName = readString(20);
    this.updateDate = readShort();
    this.updateMonth = readShort();
    this.updateYear = readShort();

    f.skipBytes(2);
    this.periods = readShort();
    this.cumulativeDays = readShort();
    this.corruptionDay = readShort();
    this.powerups = readShort();
    this.lastInitVoltage = readShort();
    for (int i = 0; i < 5; i++) {
      rotations[i] = new RotationTiming();
    }

    f.skipBytes(2);
    this.totalMessages = readShort();
    for (int i = 0; i < 40; i++) {
      if (i < this.totalMessages)
        this.msgIdMap[i] = readString(20);
      else
        f.skipBytes(40);
    }

    f.skipBytes(2);
    this.profileOrder = readShort();
    this.profileName = readString(20);
    this.profileTotalMessages = readShort();
    if (debug)
      System.out.print("About to read totalrotations:");
    this.profileTotalRotations = readShort();
    for (int m = 0; m < this.totalMessages; m++) {
      for (int r = 0; r < 5; r++) {
        this.stats[m][r] = new NORmsgStats();
      }
    }
    f.close();
    LOG.log(Level.INFO, "TBL!: FOUND FLASH STATS\n" + this.toString());
  }

  private short readShort() throws IOException {
    long sum = 0;
    int b, i;
    short ret;

    for (int l = 0; l < 2; l++) {
      b = f.readByte() & 0xFF; // remove sign
      // System.out.print(" b:"+b);
      i = b << (8 * l);
      sum += (0xFFFF & i);
    }
    ret = (short) sum;
    return ret;
  }

  private int readUnsignedShort() throws IOException {
    long sum = 0;
    int b, i;
    int ret;

    for (int l = 0; l < 2; l++) {
      b = f.readByte() & 0xFF; // remove sign
      // System.out.print(" b:"+b);
      i = b << (8 * l);
      sum += (0xFFFF & i);
    }
    ret = (int) sum;
    return ret;
  }

  private String readString(int maxChars) throws IOException {
    char[] c = new char[maxChars];
    boolean endString = false;
    for (int i = 0; i < maxChars; i++) {
      c[i] = (char) f.readByte();
      if (endString)
        c[i] = 0;
      else if (c[i] == 0)
        endString = true;
      f.readByte();
    }
    return new String(c).trim();
  }

  public long totalPlayedSecondsPerMsg(int msg) {
    long totalSec = 0;
    for (int r = 0; r < 5; r++) {
      totalSec += this.stats[msg][r].totalSecondsPlayed;
    }
    return totalSec;
  }

  public long totalPlayedSecondsPerRotation(int rotation) {
    long totalSec = 0;
    for (int m = 0; m < this.totalMessages; m++) {
      totalSec += this.stats[m][rotation].totalSecondsPlayed;
    }
    return totalSec;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    String NEW_LINE = System.getProperty("line.separator");

    s.append("Serial Number : " + this.serialNumber + NEW_LINE);
    s.append("Reflashes     : " + this.countReflashes + NEW_LINE);
    s.append("Deployment    : " + this.deploymentNumber + NEW_LINE);
    s.append("Image         : " + this.imageName + NEW_LINE);
    s.append("Profile       : " + this.profileName + NEW_LINE);
    s.append("Community     : " + this.community + NEW_LINE);
    s.append("Last Updated  : " + this.updateYear + "/" + this.updateMonth + "/"
        + this.updateDate + NEW_LINE);
    s.append("Powered Days  : " + this.cumulativeDays + NEW_LINE);
    s.append("Last PowerupV : " + this.lastInitVoltage + NEW_LINE);
    s.append("StartUps      : " + this.powerups + NEW_LINE);
    s.append("Corruption Day: " + this.corruptionDay + NEW_LINE);
    s.append("Periods       : " + this.periods + NEW_LINE);
    s.append("Rotations     : " + this.profileTotalRotations + NEW_LINE);
    s.append(NEW_LINE);
    s.append("TOTAL STATS (" + this.totalMessages + " messages)" + NEW_LINE);
    int totalSecondsPlayed = 0, countStarted = 0, countQuarter = 0,
        countHalf = 0, countThreequarters = 0, countCompleted = 0,
        countApplied = 0, countUseless = 0;
    for (int m = 0; m < this.totalMessages; m++) {
      for (int r = 0; r < (this.profileTotalRotations < 5
          ? this.profileTotalRotations : 5); r++) {
        totalSecondsPlayed += this.stats[m][r].totalSecondsPlayed;
        countStarted += this.stats[m][r].countStarted;
        countQuarter += this.stats[m][r].countQuarter;
        countHalf += this.stats[m][r].countHalf;
        countThreequarters += this.stats[m][r].countThreequarters;
        countCompleted += this.stats[m][r].countCompleted;
        countApplied += this.stats[m][r].countApplied;
        countUseless += this.stats[m][r].countUseless;
      }
    }
    s.append("       Time:" + totalSecondsPlayed / 60 + "min "
        + totalSecondsPlayed % 60 + "sec   Started:" + countStarted + "   P:"
        + countQuarter + "   H:" + countHalf + "   M:" + countThreequarters
        + "   F:" + countCompleted);
    s.append("   A:" + countApplied + "   U:" + countUseless + NEW_LINE);
    s.append(NEW_LINE);

    for (int r = 0; r < (this.profileTotalRotations < 5
        ? this.profileTotalRotations : 5); r++) {
      s.append("  Rotation:" + r + "     "
          + totalPlayedSecondsPerRotation(r) / 60 + "min "
          + totalPlayedSecondsPerRotation(r) % 60 + "sec    Starting Period:"
          + this.rotations[r].startingPeriod + "   Hours After Update:"
          + this.rotations[r].hoursAfterLastUpdate + "   Init Voltage:"
          + this.rotations[r].initVoltage + NEW_LINE);
    }
    s.append(NEW_LINE);
    s.append("Message Stats  (" + this.totalMessages + " messages)" + NEW_LINE);
    for (int m = 0; m < this.totalMessages; m++) {
      s.append("  MESSAGE ID:" + this.msgIdMap[m] + " ("
          + totalPlayedSecondsPerMsg(m) / 60 + "min "
          + totalPlayedSecondsPerMsg(m) % 60 + "sec)" + NEW_LINE);
      for (int r = 0; r < (this.profileTotalRotations < 5
          ? this.profileTotalRotations : 5); r++) {
        s.append("     ROTATION: " + r);
        s.append("       Time:" + this.stats[m][r].totalSecondsPlayed / 60
            + "min " + this.stats[m][r].totalSecondsPlayed % 60
            + "sec   Started:" + this.stats[m][r].countStarted + "   P:"
            + this.stats[m][r].countQuarter + "   H:"
            + this.stats[m][r].countHalf + "   M:"
            + this.stats[m][r].countThreequarters + "   F:"
            + this.stats[m][r].countCompleted);
        s.append("   A:" + this.stats[m][r].countApplied + "   U:"
            + this.stats[m][r].countUseless + NEW_LINE);
      }
    }
    return s.toString();
  }
}
