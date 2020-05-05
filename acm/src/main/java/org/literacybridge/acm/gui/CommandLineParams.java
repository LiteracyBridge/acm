package org.literacybridge.acm.gui;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class CommandLineParams {
  @Option(name = "--sandbox", aliases = {"-s", "--demo", "-d"}, usage = "To enter demo mode. Overrides --update")
  public boolean sandbox;

  @Option(name = "--update", aliases = {"-u"}, usage = "Don't ask, just update.")
  public boolean update;

  @Option(name = "--title", aliases = {"-title"}, usage = "to set name of ACM to be displayed in title bar")
  public String titleACM;

  @Option(name = "--headless", aliases = {"-no_ui", "--no_ui"}, usage = "start the system without showing the UI")
  public boolean disableUI;

  @Option(name = "--allcategories", usage = "show all categories, regardless of whitelisting")
  public boolean allCategories;

  @Option(name = "--clean", usage = "Clean up un-referenced audio files")
  public boolean cleanUnreferenced;

  @Option(name="--config", usage="Add toolbar icon to launch configuration dialog.")
  public boolean config;

  @Option(name="--nosplash", aliases={"--ns", "-n"}, usage="Don't use the internal splash screen." )
  public boolean noSplash;

  @Option(name="--testdata", usage="Synthesize test data, for more convenient test iteration.")
  public boolean testData;

  @Option(name="--nimbus", usage="Use 'Nimbus' look-and-feel.")
  public boolean nimbus;

  @Argument(required=false)
  public String sharedACM;
}
