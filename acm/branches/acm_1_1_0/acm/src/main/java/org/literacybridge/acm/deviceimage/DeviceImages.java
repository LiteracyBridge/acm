package org.literacybridge.acm.deviceimage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TIOStreamTransport;
import org.literacybridge.acm.repository.Repository;
import org.literacybridge.acm.thrift.ThriftDeviceImage;
import org.literacybridge.acm.thrift.ThriftDeviceProfile;
import org.literacybridge.acm.thrift.ThriftPlaylistMapping;

import com.google.common.collect.Lists;

public class DeviceImages {
	private final static TProtocolFactory PROTOCOL_FACTORY = new TJSONProtocol.Factory();
	
	public static ThriftDeviceImage loadDeviceImage(File file) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), 2048);
		try {
			TProtocol protocol = PROTOCOL_FACTORY.getProtocol(new TIOStreamTransport(in));
			ThriftDeviceImage image = new ThriftDeviceImage();
			image.read(protocol);
			return image;
		} catch (TException e) {
			throw new IOException(e);
		} finally {
			in.close();
		}
	}
	
	public static void saveDeviceImage(ThriftDeviceImage image, File file) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file), 2048);
		try {
			TProtocol protocol = PROTOCOL_FACTORY.getProtocol(new TIOStreamTransport(out));
			image.write(protocol);
			out.flush();
		} catch (TException e) {
			throw new IOException(e);
		} finally {
			out.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		ThriftDeviceImage image = new ThriftDeviceImage();
		image.setName("Issue 1");
		
		ThriftDeviceProfile profile1 = new ThriftDeviceProfile();
		ThriftDeviceProfile profile2 = new ThriftDeviceProfile();
		image.addToProfiles(profile1);
		image.addToProfiles(profile2);
		
		profile1.setName("Cliff's profile");
		profile1.setLanguage("en");
		profile1.setEraseOtherList(true);
		
		profile1.addToPlaylists(new ThriftPlaylistMapping("Agric", Lists.newArrayList("id-1", "id-2")).setLocked(true));
		profile1.addToPlaylists(new ThriftPlaylistMapping("Health", Lists.newArrayList("id-3", "id-4")));
		
		
		profile2.setName("Roger's profile");
		profile2.setLanguage("dag");
		profile2.addToPlaylists(new ThriftPlaylistMapping("Agric", Lists.newArrayList("id-5", "id-6")));
		profile2.addToPlaylists(new ThriftPlaylistMapping("Health", Lists.newArrayList("id-7", "id-8")));

		
		File file = new File("test_lb_image.json");
		saveDeviceImage(image, file);
		
		
		ThriftPlaylistMapping mapping = new ThriftPlaylistMapping();
		for (String uid : mapping.getAudioItemUIDs()) {
			File audioFile = Repository.getRepository().getA18File(uid);
//			File toFile = null;
//			Repository.copy(audioFile, toFile);
		}
		
		System.out.println(loadDeviceImage(file));
	}
}
