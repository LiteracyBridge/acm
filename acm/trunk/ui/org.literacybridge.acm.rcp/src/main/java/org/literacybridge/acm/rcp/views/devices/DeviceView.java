package org.literacybridge.acm.rcp.views.devices;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.core.MessageBus.Message;
import org.literacybridge.acm.device.DeviceConnectEvent;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;

public class DeviceView extends ViewPart {

	private TableViewer deviceListViewer = null;
	private FormToolkit toolkit;
	private Form form;

	public DeviceView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createForm(parent);
		form.setText("Available devices:");

		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		form.getBody().setLayout(layout);

		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 1;

		Table table = toolkit.createTable(form.getBody(), SWT.H_SCROLL
				| SWT.V_SCROLL);

		table.setLayoutData(td);

		deviceListViewer = new TableViewer(table);
		createColumn(deviceListViewer);

		deviceListViewer.setContentProvider(new DeviceContentProvider());
		deviceListViewer.setLabelProvider(new DeviceLabelProvider());
			
		// start after table creation so that listener can be initialized
		registerAudioDeviceListener();
	}

	private void createColumn(TableViewer viewer) {
		String[] titles = { "Location:" };
		int[] bounds = { 60 };

		for (int i = 0; i < titles.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(titles[i]);
			column.getColumn().setWidth(bounds[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
		}
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}

	private void registerAudioDeviceListener() {
		FileSystemMonitor monitor = new FileSystemMonitor();
		monitor.addDeviceRecognizer(new LiteracyBridgeTalkingBookRecognizer());	
		monitor.start();
		
		MessageBus bus = MessageBus.getInstance();
		bus.addListener(DeviceConnectEvent.class, new MessageBus.MessageListener() {
			
			@Override
			public void receiveMessage(final Message message) {
				UIJob newJob = new UIJob("Device Message Bus") {
					
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						if (deviceListViewer != null) {
							deviceListViewer.setInput(message);
						}
						
						return null;
					}
				};
				 
				newJob.schedule();
			}
		});
	}
	
	@Override
	public void setFocus() {
	}

}
