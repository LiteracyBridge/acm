package org.literacybridge.acm.gui.assistants.common;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.core.spec.ProgramSpec;

/**
 * This interface should be implemented by all Assistant contexts. I provides an accessor for the ProgramSpec.
 */
public interface AssistantContext {
    default ProgramSpec getProgramSpec() {
        if (ACMConfiguration.getInstance().isClearProgspec()) {
            ACMConfiguration.getInstance().getCurrentDB().clearProgramSpecCache();
        }
        return ACMConfiguration.getInstance().getCurrentDB().getProgramSpec();
    }
}
