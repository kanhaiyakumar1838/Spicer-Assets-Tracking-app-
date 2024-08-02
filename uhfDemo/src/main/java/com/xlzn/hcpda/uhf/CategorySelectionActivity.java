package com.xlzn.hcpda.uhf;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class CategorySelectionActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "CategoryPrefs";
    private static final String CATEGORIES_KEY = "Categories";

    private GridLayout gridLayout;
    private SharedPreferences sharedPreferences;
    private Set<String> categories;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_selection);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        categories = sharedPreferences.getStringSet(CATEGORIES_KEY, new HashSet<>());

        gridLayout = findViewById(R.id.grid_layout);

        Button plant1Button = findViewById(R.id.button_plant_1);
        Button plant2Button = findViewById(R.id.button_plant_2);
        Button scogButton = findViewById(R.id.button_scog);
        Button adminButton = findViewById(R.id.button_admin);
        Button addMoreButton = findViewById(R.id.button_add_more_category);
        Button deleteCategoryButton = findViewById(R.id.button_delete_category);

        // Get the mode from the intent
        mode = getIntent().getStringExtra("mode");

        plant1Button.setOnClickListener(createButtonClickListener("Plant 1"));
        plant2Button.setOnClickListener(createButtonClickListener("Plant 2"));
        scogButton.setOnClickListener(createButtonClickListener("SCOG"));
        adminButton.setOnClickListener(createButtonClickListener("Admin"));

        addMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog();
            }
        });

        deleteCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteCategoryDialog();
            }
        });

        loadCategories();
    }

    private View.OnClickListener createButtonClickListener(final String category) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                switch (mode) {
                    case "add":
                        // Logic for Add Tag mode
                        intent = new Intent(CategorySelectionActivity.this, AddTagActivity.class);
                        intent.putExtra("category", category);
                        startActivity(intent);
                        break;
                    case "verify":
                        intent = new Intent(CategorySelectionActivity.this, MainActivity.class);
                        intent.putExtra("category", category); // Pass category
                        startActivity(intent);
                        break;
                    case "delete":
                        // Logic for Delete Tag mode
                        intent = new Intent(CategorySelectionActivity.this, DeleteTagActivity.class);
                        intent.putExtra("category", category); // Pass category
                        startActivity(intent);
                        break;
                }
            }
        };
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Category");

        final EditText input = new EditText(this);
        input.setHint("Enter category name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            if (!categoryName.isEmpty() && !categories.contains(categoryName)) {
                addNewCategoryButton(categoryName);
                categories.add(categoryName);
                saveCategories();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Category");

        final EditText input = new EditText(this);
        input.setHint("Enter category name to delete");
        builder.setView(input);

        builder.setPositiveButton("Delete", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            deleteCategoryButton(categoryName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addNewCategoryButton(String categoryName) {
        Button newButton = new Button(this);
        newButton.setText(categoryName);
        newButton.setBackgroundTintList(getResources().getColorStateList(R.color.new_category_color, null));
        newButton.setTag(categoryName);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins(4, 4, 4, 4); // Adjust margins to match existing buttons
        newButton.setLayoutParams(params);

        newButton.setOnClickListener(createButtonClickListener(categoryName));

        gridLayout.addView(newButton);
    }

    private void deleteCategoryButton(String categoryName) {
        String categoryNameLower = categoryName.trim().toLowerCase(); // Convert input to lowercase and trim
        boolean found = false;

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View view = gridLayout.getChildAt(i);
            if (view instanceof Button) {
                Button button = (Button) view;
                String buttonText = button.getText().toString().trim().toLowerCase(); // Convert button text to lowercase and trim
                if (categoryNameLower.equals(buttonText)) { // Compare lowercase values
                    gridLayout.removeView(button);
                    found = true;
                    break;
                }
            }
        }

        // Show a Toast message if the category was not found
        if (!found) {
            Toast.makeText(this, "Category not found", Toast.LENGTH_SHORT).show();
        } else {
            categories.remove(categoryName); // Remove the category from the set
            saveCategories(); // Save the updated categories
        }
    }

    private void saveCategories() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(CATEGORIES_KEY, categories);
        editor.apply();
    }

    private void loadCategories() {
        for (String category : categories) {
            addNewCategoryButton(category);
        }
    }
}
