package org.literacybridge.acm.gui;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class CommandLineParams {
  @Option(name = "-s", usage = "to enter sandbox mode")
  public boolean sandbox;

  @Option(name = "-title", usage = "to set name of ACM to be displayed in title bar")
  public String titleACM;

  @Option(name = "-no_ui", usage = "start the system without showing the UI")
  public boolean disableUI;

  @Argument
  public String sharedACM;
}
