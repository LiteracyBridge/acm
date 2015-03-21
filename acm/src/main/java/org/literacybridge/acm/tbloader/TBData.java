package org.literacybridge.acm.tbloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

public class TBData {
	public static void aggregate () {
		String line;
		try {
			BufferedWriter bwMain = new BufferedWriter(new FileWriter("tbdata-v01-main.csv",false));
			BufferedWriter bwRotations = new BufferedWriter(new FileWriter("tbdata-v01-rotations.csv",false));
			File dir = new File("c:/tbdata-all");
			File[] files = dir.listFiles(new FilenameFilter() {
						@Override public boolean accept(File dir, String name) {
							return name.startsWith("tbData-v01-");
				}
			});				
			for (File f : files) {	
				BufferedReader in = new BufferedReader (new FileReader(f));
				if ((line = in.readLine()) != null && f == files[0]) {
//					bwMain.write("STAMP,DEVICE,");
//					bwMain.write(line + "\n");
				}
				String deviceId = f.getName().substring(f.getName().lastIndexOf("-")+1);
				deviceId = deviceId.substring(0, 8);
				//System.out.println(f.getName());
				//System.out.println(deviceId);
				long lDeviceId = Long.parseLong(deviceId, 16);
				

				while ((line = in.readLine()) != null) {
					try {
						System.out.println(line);
						line = line.replaceAll("\"", "_");
						line = line.replaceAll("\'", "_");
						System.out.println(line+"\n");
						int yr = Integer.parseInt(line.substring(0,4));
						int mo = Integer.parseInt(line.substring(5,7));
						int day = Integer.parseInt(line.substring(8,10));
						int hr = Integer.parseInt(line.substring(11,13));
						int min = Integer.parseInt(line.substring(14,16));
						int sec = Integer.parseInt(line.substring(17,19));
						long sum = (yr-2013)*366*24*3600+mo*31*24*3600+day*24*3600+hr*3600+min*60+sec;
						long stamp = lDeviceId + sum;
						bwMain.write(String.valueOf(stamp)+",");
						bwMain.write(deviceId + ",");
						bwMain.write(yr+","+mo+","+day+","+hr+":"+min+":"+sec+",");
						bwMain.write("UWR,");
						//System.out.println(line);
						final int MAIN_COLUMN_COUNT = 40;
						int index = findNthChar(line,',',MAIN_COLUMN_COUNT); // first 40 columns only
						if (index < 0) {
							bwMain.write(line);
							for (;index>-(MAIN_COLUMN_COUNT-1);index--) {
								bwMain.write(",");
							}
							bwMain.write("\n");
						} else {
							bwMain.write(line.substring(0, index) + "\n");							
							if (index < line.length()) {
								line = line.substring(index+1);
								System.out.println(line);
								for (int r=0;r<5;r++) {
									bwRotations.write(String.valueOf(stamp+","));
									int j = findNthChar(line,',',5);
									if (j < 0) {
										bwRotations.write(line + "\n");
										break;
									} else {
										bwRotations.write(line.substring(0,j)+"\n");
										if (j < line.length()) {
											line = line.substring(j+1);
										} else {
											break;
										}
									}
								}
							}							
						}
					} catch (java.lang.NumberFormatException e) {
						System.out.println("ERROR:"+line);
					}						
				}
				in.close();
			}
			bwMain.flush();
			bwMain.close();
			bwRotations.flush();
			bwRotations.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int findNthChar(String s, char c, int n) {
		int index = -1;
		int counter=0;
		int i;
		for (i=0;i<s.length();i++) {
			if (s.charAt(i) == c) {
				counter++;
				if (counter == n) {
					index = i;
					break;
				}
			}
		}
		if (counter == n) {
			index = i;
		} else {
			index = -counter;
		}
		return index;
	}
	
	public static void main(String[] args){
		TBData.aggregate();
	}
}

