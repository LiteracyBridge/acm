package org.literacybridge.acm.rcp.views.toolbar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.ui.ISizeProvider;
import org.eclipse.ui.part.ViewPart;
import org.literacybridge.acm.rcp.core.Activator;

public class ToolbarView extends ViewPart implements ISizeProvider {


	
	@Override
	public void createPartControl(Composite parent) {

//		Color color1 = new Color(Display.getCurrent(), new RGB(100,200,144));		
//		parent.setBackground(color1);
		
		GridLayout gl = new GridLayout();
		gl.numColumns = 3;
		gl.marginTop = 1;
		gl.marginBottom = 1;
		gl.verticalSpacing = 1;
		gl.marginHeight = 0;
		
		parent.setLayout(gl);
		
	    addChildControls(parent);


	}

	private void addChildControls(Composite parent) {
		
//		Color color2= new Color(Display.getCurrent(), new RGB(50,50,50));
//		Color color3 = new Color(Display.getCurrent(), new RGB(22,200,100));
//		Color color4 = new Color(Display.getCurrent(), new RGB(2,223,244));

		GridData gd0 = new GridData();
		gd0.verticalAlignment = GridData.CENTER;
		gd0.horizontalAlignment = SWT.LEFT;
		gd0.grabExcessHorizontalSpace = true;
		gd0.grabExcessVerticalSpace = true;
		
		GridData gd1 = new GridData();
		gd1.verticalAlignment = SWT.CENTER;
		gd1.horizontalAlignment = SWT.CENTER;
		gd1.grabExcessHorizontalSpace = true;
		gd1.grabExcessVerticalSpace = true;

		GridData gd2 = new GridData();
		gd2.verticalAlignment = SWT.CENTER;
		gd2.horizontalAlignment = SWT.RIGHT;
		gd2.grabExcessHorizontalSpace = true;
		gd2.grabExcessVerticalSpace = true;
		
		Composite c1 = new Composite(parent, SWT.NULL);
//		c1.setBackground(color2);
		c1.setLayoutData(gd0);
		Composite c2 = new Composite(parent, SWT.NULL);
//		c2.setBackground(color3);
		c2.setLayoutData(gd1);
		Composite c3 = new Composite(parent, SWT.NULL);
//		c3.setBackground(color4);
		c3.setLayoutData(gd2);
		
		addButtonBox(c2);
		
	}

	private void addButtonBox(Composite parent) {
		GridLayout container = new GridLayout();
		container.numColumns = 1;
		container.marginHeight = 0;
		container.marginWidth = 0;
		container.verticalSpacing = 0;
		container.horizontalSpacing = 0;
		
		parent.setLayout(container);
		
		Composite buttonComp = new Composite(parent,	SWT.NULL);
		GridLayout gl = new GridLayout();
		gl.numColumns = 4;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		buttonComp.setLayout(gl);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
//		gd.grabExcessVerticalSpace = true;
//		gd.grabExcessHorizontalSpace = true;
//		gd.heightHint = 40;
//		gd.widthHint = 40;
		
		Button leftButton = new Button(buttonComp, SWT.PUSH);
		Image imageLeft = Activator.getDefault().getImageDescriptor("icons/arrow_left.png").createImage();
		leftButton.setImage(imageLeft);
		leftButton.setLayoutData(gd);

		Button playButton = new Button(buttonComp, SWT.PUSH);
		Image imagePlay = Activator.getDefault().getImageDescriptor("icons/arrow_play.png").createImage();
		playButton.setImage(imagePlay);
		playButton.setLayoutData(gd);	
		
		Button rightButton = new Button(buttonComp, SWT.PUSH);
		Image imageRight = Activator.getDefault().getImageDescriptor("icons/arrow_right.png").createImage();
		rightButton.setImage(imageRight);
		rightButton.setLayoutData(gd);		
		
		//Composite progressBarComp = new Composite(parent, SWT.NULL);
		Slider slider = new Slider(buttonComp, SWT.HORIZONTAL);
		slider.setMaximum(100);
		//slider.setBounds(0, 0, 50, 16);
	}
	
	
	@Override
	public void setFocus() {

	}
	
	@Override
	public int computePreferredSize(boolean width, int availableParallel,
			int availablePerpendicular, int preferredResult) {
		if (width == false) {
			return 50;
		}
		
		return computePreferredSize(width, availableParallel, availablePerpendicular, preferredResult);
	}

	@Override
	public int getSizeFlags(boolean width) {
		return SWT.MIN | SWT.MAX;
	}
}
