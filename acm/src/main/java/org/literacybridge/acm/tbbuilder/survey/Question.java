package org.literacybridge.acm.tbbuilder.survey;

import org.amplio.csm.CsmData;
import org.amplio.csm.CsmEnums;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes a question in a survey.
 */
class Question {
    Question(Survey survey, String name, String prompt) {
        this.survey = survey;
        this.name = name.toLowerCase();
        this.prompt = prompt;
    }

    // Survey that this question is part of.
    final Survey survey;
    // Name of the question, like "q1".
    final String name;
    // Message title of the question prompt, like "How willing are you to visit again?"
    final String prompt;
    // Actions to be performed when the question becomes active.
    List<Action> actions = new ArrayList<>();
    // Event handlers for the question, like "Tree: Yes" to record "q1=Yes" if the Tree is pressed.
    List<Handler> handlers = new ArrayList<>();

    // The CSM state object for the question wait state (waiting for a key).
    private CsmData.CState itemWaitState;

    void addAction(Action action) {
        actions.add(action);
    }

    void addHandler(Handler handler) {
        handlers.add(handler);
    }

    private int thisIx() {
        return survey.questionIx(this);
    }

    private boolean isFirst() {
        return thisIx() == 0;
    }

    String entryStateName() {
        return name;
    }

    String promptStateName() {
        return name + "_prompt";
    }

    String waitStateName() {
        return name + "_wait";
    }

    String nextStateName() {
        return survey.nextStateNameFor(this);
    }

    /**
     * Emits the CSM for one question:
     * - A state for the entry to the question. Also plays the "beep-beep" acknowledgement for the previous question.
     * - A state to play the question prompt.
     * - A state to wait for the user's input.
     * - States to record the user's answer.
     */
    void generateCsm() {
        // q1: { Actions: [playMessage($messageid) ], CGroups: [ whenPlaying ], AudioDone: q1_wait, Circle: q1, Bowl__: q1_bowl },
        System.out.printf("Adding states for %s\n", name);
        // Create the states.

        // The CSM state object for the question itself (the prompt).
        CsmData.CState itemEntryState = survey.csmData.addCState(entryStateName());
        CsmData.CState itemPromptState = survey.csmData.addCState(promptStateName());
        itemWaitState = survey.csmData.addCState(waitStateName());

        if (isFirst()) {
            // The first question enters the "survey state".
            itemEntryState.addAction("beginSurvey", survey.surveyName);
            itemEntryState.addAction("enterState", promptStateName());
        } else {
            // Second and subsequent questions acknowledge the previous question with a "beep beep".
            // The CSM state to enter to ack the previous question and begin this one. The undecorated name of the question.
            // The beep-beep ack for the previous question.
            itemEntryState.addAction("playTune", "A/A/");
            itemEntryState.addEvent("AudioDone", promptStateName());
        }

        // The prompt itself.
        // Add the specified actions before any prompt, then the standard group and event handlers.
        actions.forEach(action -> itemPromptState.addAction(action.actionId, action.actionParams));
        String id = survey.promptToAudioItemId.get(prompt);
        itemPromptState.addAction("playMessage", id);
        itemPromptState.addGroup("whenPlaying");
        itemPromptState.addEvents(survey.csmData.new CEvent("AudioDone", name + "_wait"),
            survey.csmData.new CEvent("Circle", itemPromptState.name));

        // The wait state
        // q1_wait: { Actions: [ playTune(A/) ], CGroups: [ whenPlaying ], ...}
        System.out.printf("Adding state %s_wait\n", name);
        itemWaitState.addAction("playTune", "A/");
        itemWaitState.addGroup("whenPlaying");
        // { ...Tree: q1_tree, Table: q1_table, Lhand: q1_lhand, Rhand: q1_rhand, Bowl: q1_bowl, Circle: q1 },
        // q1_tree: { Actions: [writeMsg(s1q1=None), playTune(A/A/)], AudioDone: q2},
        handlers.forEach(Handler::generateCsm);
        // Circle to repeat the question.
        itemWaitState.addEvent("Circle", itemPromptState.name);
    }

    /**
     * Helper for toString
     *
     * @param buffer  holding the result
     * @param name    of an event, or "Action"
     * @param actions the actions of the event, or of the question's Action.
     */
    private void toStringHelper(StringBuilder buffer, String name, List<Action> actions) {
        buffer.append("  ").append(name).append(':');
        if (actions.size() == 1) {
            buffer.append("  { ").append(actions.get(0).toString()).append(" }");
        } else {
            for (Action action : actions) {
                buffer.append("\n    ").append(action.toString());
            }
        }
        buffer.append('\n');
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(name).append(":\n  Prompt: \"").append(prompt).append("\"\n");
        if (actions != null && !actions.isEmpty()) {
            toStringHelper(result, "Action", actions);
        }
        for (Handler handler : handlers) {
            toStringHelper(result, handler.eventId, handler.actionList);
        }
        return result.toString();
    }

    //region class Handler

    /**
     * This class captures handled events and their actions, like:
     * Tree:
     * write: "Not covered"
     * goto:  q10
     */
    class Handler {
        String eventId;
        List<Action> actionList = new ArrayList<>();
        boolean haveGivenNextState = false;

        Handler(String eventId, List<Action> actionList) {
            this.eventId = eventId;
            this.actionList.addAll(actionList);
        }

        /**
         * If a state does no action other than transfer to yet another state, we can skip the state.
         *
         * @return true if this state's only action is enterState.
         */
        boolean isGoOnly() {
            return actionList.size() == 1 && actionList.get(0).actionId.equals(CsmEnums.tknAction.enterState.name());
        }

        String goTarget() {
            String result = null;
            if (isGoOnly()) {
                result = actionList.get(0).actionParams;
            }
            return result;
        }

        /**
         * Emits the listeners onto the item's wait state. Emits a state to handle each event.
         */
        void generateCsm() {
            generateListener();
            generateState();
        }

        /**
         * Adds to the question's "wait state" a listener for the event to which this handler responds.
         */
        public void generateListener() {
            if (isGoOnly()) {
                // Like: Tree: go(q10) => Tree: q10
                System.out.printf("Adding event handler %s: %s\n", eventId, goTarget());
                itemWaitState.addEvent(eventId, goTarget());
            } else {
                // Like: Tree: Yes => Tree: q8_tree (with q8_tree: writeMsg(Yes), enterState(q9) generated later)
                System.out.printf("Adding event handler %s: %s_%s\n", eventId, name, eventId.toLowerCase());
                itemWaitState.addEvent(eventId, name + '_' + eventId.toLowerCase());
            }
        }

        /**
         * Adds a state containing actions to implement this handler's response. Multiple states may
         * be required for a complex response like "record user's voice input".
         */
        public void generateState() {
            if (isGoOnly()) return;
            String eventId = this.eventId.toLowerCase();
            System.out.printf("Adding state %s_%s\n", name, eventId);
            CsmData.CState hState = survey.csmData.addCState(name + '_' + eventId);
            // Add the actions to the event handling state.
            for (Action action : actionList) {
                haveGivenNextState = haveGivenNextState || action.isNoGoAction();
                CsmData.CAction cAction = survey.csmData.new CAction(action.actionId, action.actionParams);
                hState.addActions(cAction);
                if (action.isRecord()) {
                    // Add event listeners for the record script, and a handler state for the OK case.
                    String okName = name + '_' + eventId + "_OK";
                    hState.addEvents(survey.csmData.new CEvent(CsmEnums.tknEvent.OK.name(), okName),
                        survey.csmData.new CEvent(CsmEnums.tknEvent.CANCEL.name(), promptStateName()));
                    survey.csmData.addCState(okName)
                        .addActions(survey.csmData.new CAction(CsmEnums.tknAction.writeRecId.name(), name),
                            survey.csmData.new CAction(CsmEnums.tknAction.enterState.name(), nextStateName()));
                }
            }

            if (!haveGivenNextState && nextStateName() != null) {
                hState.addAction("enterState", nextStateName());
            }
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("  ").append(eventId).append(":\n");
            actionList.forEach(a -> result.append("    ").append(a).append('\n'));
            return result.toString();
        }
    }
    //endregion

    //region class Action

    // Map Actions from lower case to correct case. The survey script can specifiy actions in
    // any case and this is used to cannonicalize the action.
    static Map<String, String> actionIds = Arrays.stream(CsmEnums.tknAction.values())
        .map(Enum::name)
        .collect(Collectors.toMap(String::toLowerCase, Function.identity()));
    // Map short actions to the real names. Lower case, for look-up in prior map.
    static Map<String, String> actionIdMap = Stream.of(new String[][]{
        {"go", CsmEnums.tknAction.enterState.name().toLowerCase()},
        {"write", CsmEnums.tknAction.writeMsg.name().toLowerCase()},
        {"exit", CsmEnums.tknAction.exitScript.name().toLowerCase()},
        {"call", CsmEnums.tknAction.callScript.name().toLowerCase()}
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    // Actions that shouldn't be followed by an implicit enterState(). These are either asynchronous
    // (play*), setTimer, or already cause an explicit transfer of control.
    static Set<String> noGoActions = Stream.of(new String[]{
        CsmEnums.tknAction.enterState.name(),
        CsmEnums.tknAction.callScript.name(),
        CsmEnums.tknAction.exitScript.name(),
        CsmEnums.tknAction.playMessage.name(),
        CsmEnums.tknAction.playSubject.name(),
        CsmEnums.tknAction.playSys.name(),
        CsmEnums.tknAction.playTune.name(),
        CsmEnums.tknAction.setTimer.name()
    }).collect(Collectors.toSet());
    // Priority to "(xyz)" at the end of the string.
    static Pattern actionPattern = Pattern.compile("(?i)(\\w*)(?: *\\( *(\\w*) *\\) *)?");

    /**
     * This class describes a survey action.
     */

    class Action {

        String actionId;
        String actionParams;

        Action(Map.Entry<String, String> e) {
            this.actionId = e.getKey();
            this.actionParams = e.getValue();
        }

        /**
         * Parse a friendly action string to the proper CsmEnums.tknAction.* and param.
         *
         * @param actionString One of the following:
         *                     - A tknAction and optional param.
         *                     - A short-form action name and optional param (see actionIdMap).
         *                     - "record" to record user's voice.
         *                     - Any other string to be written as the question's response
         *                     (generates writeMsg(actionString) ).
         */
        Action(String actionString) {
            String id = "writeMsg";
            String arg = actionString;
            if (actionString.equalsIgnoreCase("record")) {
                id = CsmEnums.tknAction.callScript.name();
            } else {
                // matches "action(params)" or "action"
                Matcher m = actionPattern.matcher(actionString);
                if (m.matches()) {
                    String maybeAction = m.group(1).toLowerCase();
                    // go=>enterState, write=>writeMsg, etc.
                    if (actionIdMap.containsKey(maybeAction)) {
                        maybeAction = actionIdMap.get(maybeAction);
                    }
                    // fixup case
                    if (actionIds.containsKey(maybeAction)) {
                        id = actionIds.get(maybeAction);
                        arg = m.groupCount() == 2 ? m.group(2) : "";
                    }
                }
            }
            arg = StringUtils.defaultString(arg);
            if (id.equals("writeMsg") && !arg.startsWith(name)) {
                arg = name + '=' + arg;
            }
            this.actionId = id;
            this.actionParams = arg;
        }

        boolean isRecord() {
            return actionId.equalsIgnoreCase(CsmEnums.tknAction.callScript.name()) && actionParams.equalsIgnoreCase(
                "record");
        }

        boolean isNoGoAction() {
            return noGoActions.contains(actionId);
        }

        @Override
        public String toString() {
            return this.actionId + ": \"" + this.actionParams + '"';
        }
    }
    //endregion

}
