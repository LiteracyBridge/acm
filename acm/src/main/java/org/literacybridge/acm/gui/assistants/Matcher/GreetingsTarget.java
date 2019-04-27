package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.core.spec.RecipientList;

public class GreetingsTarget extends Target {
    // The Recipient, obviously. From the Program Specification.
    private RecipientList.RecipientAdapter recipientAdapter;

    // Is there already an audio greeting for the Recipient in the ACM?
    private boolean hasGreeting;

    public GreetingsTarget(RecipientList.RecipientAdapter recipientAdapter) {
        this.recipientAdapter = recipientAdapter;
    }

    public RecipientList.RecipientAdapter getRecipient() {
        return recipientAdapter;
    }
    public boolean hasGreeting() {
        return hasGreeting;
    }
    public void setHasGreeting(boolean hasGreeting) {
        this.hasGreeting = hasGreeting;
    }

    @Override
    public boolean targetExists() {
        return hasGreeting();
    }

    @Override
    public String toString() {
        return recipientAdapter.getName();
    }
}
