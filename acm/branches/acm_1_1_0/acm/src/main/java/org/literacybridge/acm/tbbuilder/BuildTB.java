package org.literacybridge.acm.tbbuilder;

public class BuildTB {
	static tbbuilder TB;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		// below test with output of running DeviceImages
		
		TB = new tbbuilder("test_lb_image");
		System.out.printf("BuildTB main issue dir = %s\n", TB.getIssueDir());
		System.out.printf("ISSUE NAME = %s\n", TB.deviceImagename());
		System.out.printf("profiles ");
		System.out.println(TB.getProfiles());
		
//		List<ThriftDeviceProfile> proflist = TB.getProfiles();
		
		TB.Build();
		

	}

}