package org.literacybridge.acm.gui;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.Argument;

public class CommandLineParams {
	@Option(name="-r",usage="to force read-only")
	public boolean readonly;

	@Option(name="-s",usage="to enter sandbox mode")
	public boolean sandbox;

	/*	NOT CURRENTLY USING THIS PARAMETER --  NEED TO RETHINK IT WHEN WE NEED IT
	@Option(name="-db",usage="to set path to database")
	public String pathDB;

	@Option(name="-repo",usage="to set path to repository of a18 files")
	public String pathRepository;
*/
	@Option(name="-title",usage="to set name of ACM to be displayed in title bar")
	public String titleACM;

	@Option(name="-no_ui",usage="start the system without showing the UI")
	public boolean disableUI;
	
	@Argument
	public String sharedACM;	
}
