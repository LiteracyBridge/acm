/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.literacybridge.archived_androidtbloader.util;

import com.amazonaws.regions.Regions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class Constants {

    public static final CognitoConfig cognitoConfig = /* Amplio */ new CognitoConfig(Regions.US_WEST_2,
            "us-west-2_3evpQGyi5",
            "5oviumtu4cmhspn9qt2bvn130s",
            "us-west-2:c57e8e17-b2ab-404d-a4a2-29d3865e28f7",
            "https://1rhce42l9a.execute-api.us-west-2.amazonaws.com/prod");

    public static final CognitoConfig cognitoFallbackConfig = /* greetings */ new CognitoConfig(Regions.US_WEST_2,
            "us-west-2_6EKGzq75p",
            "5h9tg11mb73p4j2ca1oii7bhkn",
            "us-west-2:a544b58b-8be0-46db-aece-e6fe14d29124",
            "https://lj82ei7mce.execute-api.us-west-2.amazonaws.com/Prod");

    // Deployments come from here.
    public static final String DEPLOYMENTS_BUCKET_NAME = "acm-content-updates";
    // Smart Sync programs are hosted here, and their deployments come from here as well.
    public static final String CONTENT_BUCKET_NAME = "amplio-program-content";
    // Statistics, logs get uploaded to here.
    public static final String COLLECTED_DATA_BUCKET_NAME = "acm-stats";

    public static final String LOCATION_FILE_EXTENSION = ".csv";

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final DateFormat ISO8601 = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset

    // "extra" names for intents
    public static final String TESTING_DEPLOYMENT = "testing_deployment";
    public static final String STATSONLY = "statsonly";
    public static final String USERNAME = "username";
    public static final String USEREMAIL = "useremail";
    public static final String PROJECT = "project";
    public static final String UPLOAD_STATUS_NAME = "status_name";
    public static final String UPLOAD_STATUS_COUNT = "status_count";
    public static final String UPLOAD_STATUS_SIZE = "status_size";
    public static final String LOCATION = "location";
    public static final String COMMUNITIES = "communities";
    public static final String PRESELECTED_RECIPIENTS = "preselected_recipients";
    public static final String SELECTED_RECIPIENT = "selected_recipient";
    public static final String SELECTED = "selected";
    public static final String SIGNOUT = "signout";
    public static final String EXIT_APPLICATION = "exitApplication";

    static {
        ISO8601.setTimeZone(UTC);
    }

    // Time to sit twiddling thumbs on Android, because we can't flush files on USB storage.
    public static final int androidPostUpdateSleepTime = 5000; // millis

    public static class CognitoConfig {
        public final Regions COGNITO_REGION;
        public final String COGNITO_USER_POOL_ID;
        public final String COGNITO_APP_CLIENT_ID;
        public final String COGNITO_APP_SECRET;
        public final String COGNITO_IDENTITY_POOL_ID;
        public final String COGNITO_USER_POOL_LOGIN_STRING;
        public final String SRN_HELPER_URL;

        public CognitoConfig(Regions cognito_region, String cognito_user_pool_id, String cognito_app_client_id, String cognito_identity_pool_id, String srn_helper_url) {
            COGNITO_REGION = cognito_region;
            COGNITO_USER_POOL_ID = cognito_user_pool_id;
            COGNITO_APP_CLIENT_ID = cognito_app_client_id;
            COGNITO_APP_SECRET = null;
            COGNITO_IDENTITY_POOL_ID = cognito_identity_pool_id;
            COGNITO_USER_POOL_LOGIN_STRING = "cognito-idp." + cognito_region.getName() + ".amazonaws.com/" + cognito_user_pool_id;
            SRN_HELPER_URL = srn_helper_url;
        }
    }
}
