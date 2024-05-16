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

  @Option(name = "--allcategories", usage = "show all categories, regardless of includelisting")
  public boolean allCategories;

  @Option(name = "--clean", usage = "Clean up un-referenced audio files")
  public boolean cleanUnreferenced;

  @Option(name="--config", usage="Add toolbar icon to launch configuration dialog.")
  public boolean config;

  @Option(name="--nosplash", aliases={"--ns", "-n"}, usage="Internal splash screen is never 'on top'." )
  public boolean noSplash;

  @Option(name="--testdata", usage="Synthesize test data, for more convenient test iteration.")
  public boolean testData;

  @Option(name="--nimbus", usage="Use 'Nimbus' look-and-feel.")
  public boolean nimbus;

  @Option(name="--devo", aliases={"--dev"}, usage="Enable 'developer' features.")
  public boolean devo;

  @Option(name="--go", usage="Do not wait for user to override default values.")
  public boolean go;

  @Option(name="--autofix", usage="Automatically fix errors.")
  public boolean autofix;

  @Option(name="--no-found-dbs", usage="Do not include found programs not in server's list of programs")
  public boolean noFoundDbs;

  @Option(name="--no-s3-dbs", usage="Do not look in S3 for programs to open")
  public boolean noS3Dbs;

  @Argument()
  public String sharedACM;
}
