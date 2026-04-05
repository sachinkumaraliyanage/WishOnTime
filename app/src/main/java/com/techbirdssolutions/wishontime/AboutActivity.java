package com.techbirdssolutions.wishontime;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAbout);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnFacebook).setOnClickListener(v -> openUrl("https://www.facebook.com/sachinkumaraliyanage"));
        findViewById(R.id.btnWebsite).setOnClickListener(v -> openUrl("https://techbirdssolutions.com/"));
        findViewById(R.id.btnGithub).setOnClickListener(v -> openUrl("https://github.com/sachinkumaraliyanage"));
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}