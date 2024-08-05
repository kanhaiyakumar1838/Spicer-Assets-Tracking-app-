package com.xlzn.hcpda.uhf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        Button addTagButton = findViewById(R.id.button_add_tag);
        Button verifyTagButton = findViewById(R.id.button_verify_tag);
        Button deleteTagButton = findViewById(R.id.button_delete_tag);
        Button historyButton = findViewById(R.id.button_history); // Add this line

        addTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, ServiceActivity.class);
                intent.putExtra("mode", "add");
                startActivity(intent);
            }
        });

        verifyTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, ServiceActivity.class);
                intent.putExtra("mode", "verify");
                startActivity(intent);
            }
        });

        deleteTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomePageActivity.this, ServiceActivity.class);
                intent.putExtra("mode", "delete");
                startActivity(intent);
            }
        });

        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistoryFile();
            }
        });
    }

    private void openHistoryFile() {
        File file = new File(getExternalFilesDir(null), "history.xlsx");
        Uri fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

}
