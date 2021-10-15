package org.literacybridge.acm.tbloader;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

class TbLoaderArgs {
    @Option(name = "--oldtbs", aliases = "-o", usage = "Target OLD Talking Books.")
    boolean oldTbs = false;

    @Option(name = "--choose", aliases = "-c", usage = "Choose Deployment and/or Package.")
    boolean choices = false;

    @Option(name="--nimbus", usage="Use 'Nimbus' look-and-feel.")
    boolean nimbus = false;

    @Option(name = "--go", aliases = "-g", usage = "Proceed without waiting where possible.")
    boolean autoGo = false;

    @Argument(usage = "Project or ACM name to export.", index = 0, metaVar = "ACM")
    String project;

    @Argument(usage = "Serial number prefix, default 'B-'.", index = 1, metaVar = "SRN_PREFIX")
    String srnPrefix = null;
}
