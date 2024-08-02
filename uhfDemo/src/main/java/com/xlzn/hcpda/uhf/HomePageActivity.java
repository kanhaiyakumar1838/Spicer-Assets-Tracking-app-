package com.xlzn.hcpda.uhf;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        Button addTagButton = findViewById(R.id.button_add_tag);
        Button verifyTagButton = findViewById(R.id.button_verify_tag);
        Button deleteTagButton = findViewById(R.id.button_delete_tag);

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
    }
}
