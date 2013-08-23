package org.literacybridge.acm.gui;

import java.net.URL;

public abstract class UIConstants {
	public static final String ICON_EDIT_16_PX = "edit16px.png";
	public static final String ICON_DELETE_16_PX = "delete16px.png";
	public static final String ICON_EXPORT_16_PX = "export16px.png";
	public static final String ICON_SETTINGS_16_PX = "settings16px.png";
	
	public static final String ICON_BACKWARD_24_PX = "back-24px.png";
	public static final String ICON_PAUSE_24_PX = "pause-24px.png";
	public static final String ICON_PLAY_24_PX = "play-24px.png";
	public static final String ICON_FORWARD_24_PX = "forward-24px.png";
	
	public static URL getResource(String resourceName) {
		return UIConstants.class.getResource("/" + resourceName);
	}

}
