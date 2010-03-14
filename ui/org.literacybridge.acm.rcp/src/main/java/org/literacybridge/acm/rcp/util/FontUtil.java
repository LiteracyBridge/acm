package org.literacybridge.acm.rcp.util;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.literacybridge.acm.rcp.core.ApplicationWorkbenchWindowAdvisor;

public class FontUtil {
	
	// Font for the text shown in the toolbar
	private static Font toolbarFont = null;
	
	public static Font getToolbarDefaultFont() {
		if (toolbarFont == null) {
			Font defaultFont = ApplicationWorkbenchWindowAdvisor.getFontRegistry().defaultFont();
			
		    FontData[] fontData = defaultFont.getFontData();
		    // copy font data
		    FontData[] newFontData = new FontData[fontData.length];
		    for (int i = 0; i < fontData.length; i++) {
		    	FontData oldFontData = fontData[i]; 
		    	newFontData[i] = new FontData("toolbarfont"+i, oldFontData.getHeight(), oldFontData.getStyle());
		    	newFontData[i].setHeight(14);
		    }			

		    toolbarFont = new Font(Display.getCurrent(), newFontData);
		}    
	    
	    return toolbarFont;
	}
}
