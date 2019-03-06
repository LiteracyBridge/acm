package org.literacybridge.acm.gui.Assistant;

/**
 * This packages implements a simple multi-step dialog, with step-to-step navigation, often
 * called a "Wizard".
 *
 * To implement an Assistant, create a context class to pass information between pages.
 *
 * Create page classes derived from AssistantPage<ContextClass>, one for every page in the
 * Assistant. Each page must implement onPageEntered(boolean progressing), and must call
 * setComplete() to indicate that the Assistant can move on. There are a few helpers
 * in AssistantPage; see that class for details.
 *
 * Finally, create an instance of the Assistant.Factory, set the desired options, at
 * least the pages of the Assistant (passed as an array of references to the individual
 * page constructors.
 *
 */
