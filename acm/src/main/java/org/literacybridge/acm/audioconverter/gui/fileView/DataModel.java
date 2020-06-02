package org.literacybridge.acm.audioconverter.gui.fileView;

import java.io.File;
import java.util.Set;
import java.util.Vector;

import org.literacybridge.acm.audioconverter.gui.fileFilters.Utils;

public class DataModel {

  // inner class to store some info of the files.
  public class FileInfo {

    private String fileName;
    private String fileExtension;
    private String fileSize;
    File fileRef;
    boolean doConvert = false;
    boolean valid = true;

    public FileInfo(String fileName, String fileExtension, String fileSize,
        File fileRef, boolean doConvert) {
      this.fileName = fileName;
      this.fileExtension = fileExtension;
      this.fileSize = fileSize;
      this.fileRef = fileRef;
      this.doConvert = doConvert;
    }

    public void setConvert(boolean doConvert) {
      this.doConvert = doConvert;
    }

    public String getFileName() {
      return fileName;
    }

    public String getFileExtension() {
      return fileExtension;
    }

    public String getFileSize() {
      return fileSize;
    }

    public File getFileRef() {
      return fileRef;
    }

    public boolean doConvert() {
      return doConvert;
    }

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      FileInfo clone = new FileInfo(fileName, fileExtension, fileSize, fileRef,
          false);
      clone.setValid(false);
      return clone;
    }
  }

  // stores the original file list
  private Vector orgFileInfoList = new Vector();

  // stores filtered list
  private Vector fileInfoList = new Vector();

  private File directory;

  // constructor
  public DataModel(File directory) {
    this.directory = directory;
    getFileData(directory);
  }

  public void updateFiles() {
    getFileData(directory);
  }

  public Vector getFileInfoList() {
    return fileInfoList;
  }

  private void getFileData(File directory) {
    orgFileInfoList.clear();
    fileInfoList.clear();

    if (directory.isDirectory()) {
      File[] files = directory.listFiles();
      for (int i = 0; i < files.length; ++i) {
        File currFile = files[i];

        if (!currFile.isDirectory()) {
          String fileName = currFile.getName();
          String fileExtension = Utils.getExtension(currFile).toLowerCase();
          String fileSize = new Long(currFile.length()).toString();

          if (fileName != null && fileExtension != null && fileSize != null) {
            fileInfoList.add(new FileInfo(fileName, fileExtension, fileSize,
                currFile, false));
          }
        }
      }
      // clone file list to save original files
      orgFileInfoList = (Vector) fileInfoList.clone();
    }
  }

  public void checkAll(boolean check) {
    for (int i = 0; i < fileInfoList.size(); ++i) {
      ((FileInfo) fileInfoList.get(i)).setConvert(check);
    }
  }

  public void showOnlyFilesWithExtension(Set<String> fileExtensions) {
    // only files with the passed extension shall be shown
    fileInfoList.clear();
    for (int i = 0; i < orgFileInfoList.size(); ++i) {
      FileInfo info = (FileInfo) orgFileInfoList.get(i);
      if (fileExtensions == null // show all
          || fileExtensions.contains(info.fileExtension)) {
        try {
          fileInfoList.add(info.clone());
        } catch (CloneNotSupportedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public int getNumFilesToConvert() {
    int num = 0;
    for (int i = 0; i < fileInfoList.size(); ++i) {
      FileInfo info = (FileInfo) fileInfoList.get(i);
      if (info.doConvert()) {
        num++;
      }
    }
    return num;
  }
}
