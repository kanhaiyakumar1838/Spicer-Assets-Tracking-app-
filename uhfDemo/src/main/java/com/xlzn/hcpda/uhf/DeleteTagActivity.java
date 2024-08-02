package com.xlzn.hcpda.uhf;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class DeleteTagActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_FETCH_EPC = 1; // Request code for starting MainActivity
    private EditText etEpcNumber;
    private EditText etMachineName;
    private EditText etCellName;
    private EditText etOperation;
    private EditText etAssetCode;
    private Button btnDelete;
    private Button btnFetchEpc;

    private String category;
    private static final String TAG = "DeleteTagActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_tag);

        etEpcNumber = findViewById(R.id.etEpcNumber);
        etMachineName = findViewById(R.id.etMachineName);
        etCellName = findViewById(R.id.etCellName);
        etOperation = findViewById(R.id.etOperation);
        etAssetCode = findViewById(R.id.etAssetCode);
        btnDelete = findViewById(R.id.btnDelete);
        btnFetchEpc = findViewById(R.id.btnFetchEpc);

        // Retrieve the category name from the intent
        category = getIntent().getStringExtra("category");

        // Log the category value
        Log.d(TAG, "Category received: " + category);

        btnDelete.setOnClickListener(v -> deleteTag());
        btnFetchEpc.setOnClickListener(v -> fetchEpc());
    }

    private void fetchEpc() {
        // Start MainActivity with fetch value
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fetch", 1); // Send fetch value to MainActivity
        startActivityForResult(intent, REQUEST_CODE_FETCH_EPC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FETCH_EPC && resultCode == Activity.RESULT_OK && data != null) {
            // Get the EPC number from MainActivity
            String epcNumber = data.getStringExtra("epcNumber");
            if (epcNumber != null) {
                etEpcNumber.setText(epcNumber);
            } else {
                Toast.makeText(this, "Failed to fetch EPC", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void deleteTag() {
        String epcNumber = etEpcNumber.getText().toString().trim();

        if (epcNumber.isEmpty()) {
            Toast.makeText(this, "Please enter EPC number", Toast.LENGTH_SHORT).show();
        } else {
            if (removeFromExcel(epcNumber)) {
                Toast.makeText(this, "Tag deleted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "EPC number not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean removeFromExcel(String epcNumber) {
        File file = new File(getExternalFilesDir(null), "tags.xlsx");
        boolean found = false;

        if (!file.exists()) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Use category as sheet name
            Sheet sheet = workbook.getSheet(category);
            if (sheet != null) {
                for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        Cell cell = row.getCell(0);
                        if (cell != null && cell.getStringCellValue().equals(epcNumber)) {
                            sheet.removeRow(row);
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    // Save the updated workbook
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        workbook.write(fos);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error modifying Excel file", e);
        }

        return found;
    }
}
