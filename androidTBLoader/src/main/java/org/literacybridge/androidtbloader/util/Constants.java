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

package org.literacybridge.androidtbloader.util;

import com.amazonaws.regions.Regions;
import org.literacybridge.core.fs.OperationLog;

import java.util.HashSet;
import java.util.Set;

public class Constants {

    public static final Regions COGNITO_REGION = Regions.US_WEST_2;

    public static final String COGNITO_IDENTITY_POOL_ID = "us-west-2:a544b58b-8be0-46db-aece-e6fe14d29124";
    public static final String COGNITO_USER_POOL_ID = "us-west-2_6EKGzq75p";
    public static final String COGNITO_USER_POOL_LOGIN_STRING = "cognito-idp.us-west-2.amazonaws.com/" + COGNITO_USER_POOL_ID;

    public static final String COGNITO_APP_CLIENT_ID = "5h9tg11mb73p4j2ca1oii7bhkn";
    public static final String COGNITO_APP_SECRET = null;

    // Content updates come from here.
    public static final String CONTENT_UPDATES_BUCKET_NAME = "acm-content-updates";
    // Statistics, logs get uploaded to here.
    public static final String COLLECTED_DATA_BUCKET_NAME = "acm-stats";

    public static final String LOCATION_FILE_EXTENSION = ".csv";
}
