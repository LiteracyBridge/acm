package org.literacybridge.androidtbloader;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // The actionbar has a "title" property that is set from the activity's "label=" property
        // from the AndroidManifest file. Here, we make the toolbar work like an action bar.

        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = (TextView)findViewById(R.id.main_toolbar_title);
        //main_title.setText("");

        // We want a "back" button (sometimes called "up"), but we don't want back navigation, but
        // to simply end this activity without setting project or community.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }
}
