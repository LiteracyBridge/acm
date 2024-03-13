package org.literacybridge.acm.config;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PermissionsControl {

    private Boolean manage_deployment;
    private Boolean deploy_content;
    private Boolean manage_prompt;
    private Boolean manage_playlist;

    public PermissionsControl() {
        manage_deployment = false;
        deploy_content = false;
        manage_prompt = false;
        manage_playlist = false;
    }

    public Boolean hasManageDeployment() {
        return manage_deployment;
    }

    public Boolean hasDeployContent() {
        return deploy_content;
    }

    public Boolean hasManagePrompt() {
        return manage_prompt;
    }

    public Boolean hasManagePlaylist() {
        return manage_playlist;
    }

    @SuppressWarnings("unchecked")
    public void getAccessControls() {
        // make an api request and get current user's roles and permissions
        // Now enable/disable certain features based on his access permissions

        String requestURL = "https://nhr12r5plj.execute-api.us-west-2.amazonaws.com/dev/users/me";
        HashMap<String, String> headers = getHeaders();

        HttpUtility httpUtility = new HttpUtility();
        JSONObject jsonResponse = null;
        try {
            httpUtility.sendGetRequest(requestURL, headers);
            jsonResponse = httpUtility.readJSONObject();
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            httpUtility.disconnect();
        }

        if (jsonResponse == null) {
            // There was an error somewhere.
            // How do we handle this?
            return;
        }

        if (jsonResponse.containsKey("status_code")) {
            // now what else can we do with this data ?
            Object status_code = jsonResponse.get("status_code");
            if (Objects.equals(status_code.toString(), "200")) {
                // Okay connection succeeded
                // let's get data object
                System.out.println("Connection succeeded");
                List<Object> data = (List<Object>) jsonResponse.get("data");
                Map<String, JSONObject> permissionsMap = (Map<String, JSONObject>) data.get(0);
                // Let's get permission fields
                JSONObject permissionsObject = permissionsMap.get("permissions");
                manage_deployment = (Boolean) permissionsObject.get("manage_deployment");
                deploy_content = (Boolean) permissionsObject.get("deploy_content");
                manage_prompt = (Boolean) permissionsObject.get("manage_prompt");
                manage_playlist = (Boolean) permissionsObject.get("manage_playlist");
            }
        }

    }

    private static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer eyJraWQiOiIzWHNtM2o5dGJQTWxBWDIzaUR2b1FJSWZXWjhcL2ZqbUpjME1LWWV3TVlDTT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI0YjM5YzM3ZS1kMDJhLTRiNmItYWU3MS1iMDNhZTk2NjE0NDYiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLXdlc3QtMi5hbWF6b25hd3MuY29tXC91cy13ZXN0LTJfM2V2cFFHeWk1IiwiY29nbml0bzp1c2VybmFtZSI6IjRiMzljMzdlLWQwMmEtNGI2Yi1hZTcxLWIwM2FlOTY2MTQ0NiIsImF1ZCI6IjExNWZrbjdjNzA4OWd2ZWIzZjJuMHJvcDRhIiwiZXZlbnRfaWQiOiJmMmIzZjM2YS04MGNiLTRiMjMtODNhNC0wYjJmNWQ5YzhmZGUiLCJ0b2tlbl91c2UiOiJpZCIsImF1dGhfdGltZSI6MTcwOTYyNTg3MywibmFtZSI6IkVtbWFudWVsIFNhc3UiLCJleHAiOjE3MDk3MTIyNzMsImlhdCI6MTcwOTYyNTg3MywiZW1haWwiOiJlbW1hbnVlbEBhbXBsaW8ub3JnIn0.Bi8mJEN2DePI8A34qmhZ43HVI7-Ub0HgbUyvOdhpSE_KKH__v206aVl5CLE-ZbhcaSweUcyWyD4GN5AGrLE1NoPsWZO1JDQNxZAaf9PTLLIJP_Z11Vs9dOX9zxKiIWf_Y41YmC5CChTU3CW2h_MeS2DiJzMdce9WjEgnH2SjU7xrWiBLmSpE9lXSCh2xn54UPnVJxxgD5M3FCVX1wQ28UY9GOECbgNP3_Mju8EJcUraDnqnIgN-VvZx3ZsnjFNJMIgQmT1i0-tWqvZJRUrMitmKqJ7wjQsq2d9Ptx3CWLJhdt0OAnEwI0at7O_VrcE1tnLtok5h8DYZhm0qznSZ4ZA");
        headers.put("accept", "application/json");
        return headers;
    }
}
