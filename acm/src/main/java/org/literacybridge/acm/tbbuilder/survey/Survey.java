package org.literacybridge.acm.tbbuilder.survey;

import org.amplio.csm.CsmData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Survey {
    @SuppressWarnings("unused")
    public static Survey loadDefinition(String yamlString) {
        return new Builder(yamlString).build();
    }
    public static Survey loadDefinition(File yamlFile) throws IOException {
        return new Builder(yamlFile).build();
    }

    final String surveyName;
    private final String prologPrompt;
    private final String epilogPrompt;
    private final String confirmExitPrompt;

    private final List<Question> questions = new ArrayList<>();

    Map<String,String> promptToAudioItemId = new HashMap<>();

    final CsmData csmData;

    public Survey(String surveyName, String prologPrompt, String epilogPrompt, String confirmExitPrompt) {
        this.surveyName = surveyName;
        this.prologPrompt = prologPrompt;
        this.epilogPrompt = epilogPrompt;
        this.confirmExitPrompt = confirmExitPrompt;
        this.csmData = new CsmData();
    }

    /**
     * Retrieve a list of the prompts used in this survey, including the prolog and epilog.
     * @return a collection of prompts.
     */
    public Collection<String> getPromptList() {
        Set<String> result = new HashSet<>();
        result.add(prologPrompt);
        result.add(epilogPrompt);
        result.add(confirmExitPrompt);
        result.addAll(questions.stream().map(q->q.prompt).collect(Collectors.toList()));
        return result;
    }

    /**
     * Provide a map between prompts and audio ids. These ids will be used in "playMessage(id)" actions.
     * @param promptMap A map of {prompt:id}.
     */
    public void setPromptMap(Map<String,String> promptMap) {
        this.promptToAudioItemId.putAll(promptMap);
    }

    /**
     * Generate the Control State Machine definition to implement the survey.
     *
     * @return The CSM.
     */
    public CsmData generateCsm() {
        // The first state is the entry state (in a v2 script), so the prolog must be first.
        generateProlog();
        generateEpilog();
        generateQuestions();
        generateBoilerPlate();
        ArrayList<String> errors = new ArrayList<>();
        csmData.fillErrors(errors);

        return csmData;
    }

    /**
     * Adds a Question to the list of questions.
     * @param question to be added.
     */
    void addQuestion(Question question) {
        questions.add(question);
    }

    /**
     * Given a Question, find its index in questions.
     * @param question to be found
     * @return the index, or -1 if the Question is not in questions.
     */
    int questionIx(Question question) {
        return questions.indexOf(question);
    }

    /**
     * Given a Question, find the (name of the) next state to be entered in normal flow.
     * @param question for which next state name is needed
     * @return the name of the next state, or "epilog" if there is no next state.
     */
    String nextStateNameFor(Question question) {
        int ix = questionIx(question);
        if (ix < questions.size()-1) {
            return questions.get(ix+1).entryStateName();
        }
        return "epilog";
    }

    /**
     * Generate the prolog state.
     * The state will play the prolog greeting, and respond to House (exit), Circle (repeat), and Tree (start survey).
     */
    private void generateProlog() {
        String id = promptToAudioItemId.get(prologPrompt);
        // CState: prolog: { Actions: [ playMessage(prolog) ], CGroups: [ whenPlaying ], House(epilog_3), Circle(prolog), Tree(q1)
        CsmData.CState prologState = csmData.addCState("prolog");
        prologState.addAction("playMessage", id);
        prologState.addGroup("whenPlaying");
        prologState.addEvent("House", "epilog_3");
        prologState.addEvent("Circle", "prolog");
        prologState.addEvent("Tree", questions.get(0).entryStateName());
    }

    /**
     * Generate the epilog, called when the survey has been completed.
     * Records that the survey is complete, ends the survey state, and responds to House and Tree (exit survey,
     * with "HOUSE" as the result)
     */
    private void generateEpilog() {
        String id = promptToAudioItemId.get(epilogPrompt);
        CsmData.CState epilogState = csmData.addCState("epilog");
        epilogState.addAction("writeMsg", "complete=Yes");
        epilogState.addAction("endSurvey");
        epilogState.addAction("playTune", "A/A/");
        epilogState.addEvent("AudioDone", "epilog_2");
        CsmData.CState epilogState2 = csmData.addCState("epilog_2");
        epilogState2.addAction("playMessage", id);
        epilogState2.addGroup("whenPlaying");
        epilogState2.addEvent("House", "epilog_3");
        epilogState2.addEvent("Tree", "epilog_3");
        CsmData.CState epilogState3 = csmData.addCState("epilog_3");
        epilogState3.addAction("exitScript", "House");
    }

    /**
     * Iterates the questions and requests each to generate its required states.
     */
    private void generateQuestions() {
        for (Question question : questions) {
            question.generateCsm();
        }
    }

    /**
     * Generates the "whenPlaying" CGroup, and the handlers for Plus, Minus, House, and Bowl.
     *
     * Note that Bowl can be used as an answer key; there is nothing playing when awaiting an answer.
     */
    private void generateBoilerPlate() {
        // CGroup: whenPlaying: { Plus:stPlayLouder, Minus:stPlaySofter, House:stHousePressed, Bowl:stPlayPause }
        CsmData.CGroup whenPlaying = csmData.addCGroup("whenPlaying");
        whenPlaying.addEvent("Plus", "stPlayLouder");
        whenPlaying.addEvent("Minus", "stPlaySofter");
        whenPlaying.addEvent("House", "stHousePressed");
        whenPlaying.addEvent("Bowl", "stPlayPause");

        // CState: stPlayLouder: { Actions: [ volAdj(+1), resumePrevState ] }
        CsmData.CState stPlayLouder = csmData.addCState("stPlayLouder");
        stPlayLouder.addAction("volAdj", "+1");
        stPlayLouder.addAction("resumePrevState");
        // CState: stPlaySofter: { Actions: [ volAdj(-1), resumePrevState ] }
        CsmData.CState stPlaySofter = csmData.addCState("stPlaySofter");
        stPlaySofter.addAction("volAdj", "-1");
        stPlaySofter.addAction("resumePrevState");

        // CState: stHousePressed: { Actions: [ saveState(1), playSys(CONFIRM_EXIT) ], Tree: stHouseExit, Table: stHouseContinue
        // TODO: Should this be a system-defined "do you want to quit the survey", or a survey-specific one?
        String id = promptToAudioItemId.get(confirmExitPrompt);
        CsmData.CState stHousePressed = csmData.addCState("stHousePressed");
        stHousePressed.addAction("saveState", "1");
        stHousePressed.addAction("playMessage", id);
        stHousePressed.addEvent("Tree", "stHouseExit");
        stHousePressed.addEvent("Table", "stHouseContinue");
        // CState: stHouseExit: { Actions: [ writeMsg(complete=No), exitScript(House) ] }
        CsmData.CState stHouseExit = csmData.addCState("stHouseExit");
        stHouseExit.addAction("writeMsg", "complete=No");
        stHouseExit.addAction("exitScript", "House");
        // CState: stHouseContinue: { Actions: [ resumeSavedState(1) ] }
        CsmData.CState stHouseContinue = csmData.addCState("stHouseContinue");
        stHouseContinue.addAction("resumeSavedState", "1");

        // CState: stPlayPause:  { Actions: [ saveState( 2),  setTimer(180000), pausePlay ], CGroups:[ whenPlaying ], Bowl: stPlayResume, Timer: houseExit }
        CsmData.CState stPlayPause = csmData.addCState("stPlayPause");
        stPlayPause.addAction("saveState", "2");
        stPlayPause .addAction("setTimer", "180000");
        stPlayPause.addAction("pausePlay");
        stPlayPause.addGroups("whenPlaying");
        stPlayPause.addEvent("Bowl", "stPlayResume");
        stPlayPause.addEvent("Timer", "stHouseExit");
        // CState: stPlayResume: { Actions: [ resumePlay,  resumeSavedState(2) ] }
        CsmData.CState stPlayResume = csmData.addCState("stPlayResume");
        stPlayResume.addAction("resumePlay");
        stPlayResume.addAction("resumeSavedState", "2");
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Name: ").append(surveyName).append('\n');
        questions.forEach(q -> result.append(q).append('\n'));
        return result.toString();
    }


}
