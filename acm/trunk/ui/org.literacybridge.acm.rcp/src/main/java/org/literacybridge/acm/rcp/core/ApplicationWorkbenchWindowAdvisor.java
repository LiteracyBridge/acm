package org.literacybridge.acm.rcp.core;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private static FontRegistry fontRegistry = null;
	
    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(1000, 500));
        configurer.setShowStatusLine(false);
        configurer.setShowCoolBar(false);
        configurer.setTitle("LiteracyBridge - Audio Content Manager");
        
        
        
        // init font registry for this workbench
        initFontRegistry();
    }
    
    private void initFontRegistry() {
    	fontRegistry = new FontRegistry(Display.getCurrent());
    }
    
    public static FontRegistry getFontRegistry() {
    	return fontRegistry;
    }
}
