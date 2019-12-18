package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.assistants.Matcher.Target;
import org.literacybridge.core.spec.RecipientList;

public class GreetingTarget extends Target {
    // The Recipient, obviously. From the Program Specification.
    private RecipientList.RecipientAdapter recipientAdapter;

    // Is there already an audio greeting for the Recipient in the ACM?
    private boolean hasGreeting;

    GreetingTarget(RecipientList.RecipientAdapter recipientAdapter) {
        this.recipientAdapter = recipientAdapter;
    }

    public RecipientList.RecipientAdapter getRecipient() {
        return recipientAdapter;
    }
    private boolean hasGreeting() {
        return hasGreeting;
    }
    void setHasGreeting(boolean hasGreeting) {
        this.hasGreeting = hasGreeting;
    }

    @Override
    public boolean targetExists() {
        return hasGreeting();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(recipientAdapter.communityname);
        if (StringUtils.isNotBlank(recipientAdapter.groupname)) {
            result.append('-').append(recipientAdapter.groupname);
        }
        if (StringUtils.isNotBlank(recipientAdapter.agent)) {
            result.append('-').append(recipientAdapter.agent);
        }
        return result.toString();
    }
}
