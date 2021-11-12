/*
 *  Copyright 2013-2016 Amazon.com,
 *  Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Amazon Software License (the "License").
 *  You may not use this file except in compliance with the
 *  License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 *  or in the "license" file accompanying this file. This file is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, express or implied. See the License
 *  for the specific language governing permissions and
 *  limitations under the License.
 */

package org.literacybridge.androidtbloader.signin;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.literacybridge.androidtbloader.BuildConfig;
import org.literacybridge.androidtbloader.R;

import java.util.Date;

public class AboutApp extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_AboutApp);
        toolbar.setTitle("");
        TextView title = (TextView) findViewById(R.id.about_toolbar_title);
        title.setText("About");
        TextView version =(TextView) findViewById(R.id.about_version_name);
        version.setText(String.format("Version %s", BuildConfig.VERSION_NAME));

        Date buildDate = new Date(BuildConfig.TIMESTAMP);
        TextView buildDateText = (TextView) findViewById(R.id.about_version_timestamp);
        buildDateText.setText(String.format("%sBuild time %s", BuildConfig.DEBUG?"D":"", buildDate.toString()));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        Button backButton = (Button) findViewById(R.id.aboutBack);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

}
