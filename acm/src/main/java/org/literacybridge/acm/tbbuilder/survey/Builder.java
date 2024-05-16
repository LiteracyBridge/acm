package org.literacybridge.acm.tbbuilder.survey;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Builder {
    private final static List<String> nonItems = Arrays.asList("Name", "Defaults", "Survey");
    private final static List<String> nonEvents = Arrays.asList("Name", "Prompt", "Action");
    private final Map<String, Object> yamlObject;

    /**
     * Initialize from a YAML string.
     * @param yamlString string to be loaded.
     */
    public Builder(String yamlString) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(options);
        // We only load our own, trusted data.
        //noinspection VulnerableCodeUsages
        yamlObject = yaml.load(yamlString);
    }

    /**
     * Initialize from a File containing YAML.
     * @param yamlFile to be loaded.
     * @throws IOException if the file can't be read.
     */
    public Builder(File yamlFile) throws IOException {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(options);
        try (FileInputStream yamlStream = new FileInputStream(yamlFile)) {
            // We only load our own, trusted data.
            //noinspection VulnerableCodeUsages
            yamlObject = yaml.load(yamlStream);
        }
    }

    /**
     * Build the survey from the loaded YAML. This method assumes that the YAML is well-formed.
     * @return the survey.
     */
    public Survey build() {
        Survey result = buildSurvey();
        buildQuestions(result);
        return result;
    }

    /**
     * Parses the "Survey" object of the survey.yaml. Should contain:
     * Survey:
     *   Name: Survey Name  (Default "Survey1"
     *   Prolog: Welcome to the Ghana Health Services satisfaction survey
     *   Epilog: Thank you for you participation.
     * @return a Survey object initialized with these values.
     */
    private Survey buildSurvey() {
        //noinspection unchecked
        Map<String,String> surveyObject = (Map<String,String>)yamlObject.get("Survey");
        String name = surveyObject.getOrDefault("Name", "Survey1");
        String prolog = surveyObject.get("Prolog");
        String epilog = surveyObject.get("Epilog");
        String confirmExit = surveyObject.get("ConfirmExit");
        return new Survey(name, prolog, epilog, confirmExit);
    }

    private void buildQuestions(Survey survey) {
        // q1:
        //    ...
        // q2:
        //   ...
        for (Map.Entry<String, Object> entry : yamlObject.entrySet()) {
            // Skip "Name", "Defaults", "Survey"
            String name = entry.getKey();
            if (nonItems.contains(name)) continue;
            // q1:
            //   Prompt: This is my question title
            //   Action:
            //      write: Halfway done
            //   LHand:
            //      write: Response One
            //   RHand:
            //      write: Response Two
            //      goto: q3

            // Get the question's definition.
            //noinspection unchecked
            Map<String, Object> itemDefinition = (Map<String, Object>) entry.getValue();
            String prompt = itemDefinition.getOrDefault("Prompt", name).toString();
            Question question = new Question(survey, name, prompt);

            // Pull out the "Action" entry, if any, and add it to the question.
            List<Question.Action> actionList = parseActions(question, itemDefinition.get("Action"));
            actionList.forEach(question::addAction);

            // Get the handled events and their actions.
            Map<String, List<Question.Action>> handlers = new LinkedHashMap<>();
            itemDefinition.entrySet().stream()
                // "Prompt" & "Action" are not events, and have already been handled separately.
                .filter((e) -> !nonEvents.contains(e.getKey())).forEach((e) -> {
                    String eventId = e.getKey();
                    // The actions may be given in several forms:
                    // Tree: "No nurse"   ==> Map.Entry("Tree", "No nurse") ==> Tree: List(Action("write", "No nurse"))
                    // Tree: go(q2)       ==> Map.Entry("Tree", "go(q2)")   ==> Tree: List(Action("enterState", "q2"))
                    // Tree:
                    //   - "No nurse"
                    //   - go(q2)
                    //      ==> Map.Entry("Tree", List("No Nurse", "go(q2)")) ==> Tree: List(Action("write", "No nurse"), Action("enterState", "q2"))

                    Object eventActions = e.getValue();
                    List<Question.Action> actions = parseActions(question, eventActions);
                    handlers.put(eventId, actions);
                });
            handlers.forEach((eventId, actions) -> question.addHandler(question.new Handler(eventId, actions)));
            survey.addQuestion(question);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Question.Action> parseActions(Question question, Object actions) {
        List<Question.Action> result = new ArrayList<>();
        if (actions instanceof String) {
            result = Collections.singletonList(question.new Action((String)actions));
        } else if (actions instanceof List) {
            // TODO: Should this recurse on each element? In case we get List<Map<String,String>>, etc.?
            result = ((List<String>)actions).stream().map((String actionString) -> question.new Action(actionString)).collect(Collectors.toList());
        } else if (actions instanceof Map) {
            result = ((Map<String, String>)actions).entrySet().stream()
                .map((Map.Entry<String, String> e) -> question.new Action(e)).collect(Collectors.toList());
        }
        return result;
    }

}
