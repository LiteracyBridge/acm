package org.literacybridge.acm.device;

import java.io.File;

public abstract class DeviceRecognizer {
	public abstract DeviceInfo identifyDevice(File pathToDevice);
}
