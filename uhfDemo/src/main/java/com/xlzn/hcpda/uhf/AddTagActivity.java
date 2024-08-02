package com.xlzn.hcpda.uhf;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.xlzn.hcpda.uhf.entity.UHFTagEntity;
import com.xlzn.hcpda.uhf.interfaces.OnInventoryDataListener;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import androidx.appcompat.app.AlertDialog;

public class AddTagActivity extends AppCompatActivity {

    private EditText etEpcNumber;
    private EditText etMachineName;
    private EditText etCellName;
    private EditText etOperation;
    private EditText etAssetCode;
    private Button btnSave;
    private Button btnFetchEpc;
    private String stringSingleRead = "start"; // Default value
    private long startTime;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private static final String TAG = "AddTagActivity";
    private String category;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tag);

        etEpcNumber = findViewById(R.id.etEpcNumber);
        etMachineName = findViewById(R.id.etMachineName);
        etCellName = findViewById(R.id.etCellName);
        etOperation = findViewById(R.id.etOperation);
        etAssetCode = findViewById(R.id.etAssetCode);
        btnSave = findViewById(R.id.btnSave);
        btnFetchEpc = findViewById(R.id.btnFetchEpc);

        // Retrieve the category name from the intent
        category = getIntent().getStringExtra("category");

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    String epcNumber = data.getStringExtra("epcNumber");
                    if (epcNumber != null) {
                        etEpcNumber.setText(epcNumber);
                    }
                }
            }
        });

        btnSave.setOnClickListener(v -> saveTag());
        btnFetchEpc.setOnClickListener(v -> fetchEpc());
    }

    private void fetchEpc() {
        Intent intent = new Intent(AddTagActivity.this, MainActivity.class);
        intent.putExtra("fetch", 1);
        activityResultLauncher.launch(intent);
    }

    private void startStop(final String singleRead) {
        Log.d(TAG, "startStop called with: " + singleRead);

        if ("start".equals(singleRead)) {
            Log.d(TAG, "Entering start inventory condition");
            long time = SystemClock.elapsedRealtime() - startTime;
            // Set up the inventory data listener
            UHFReader.getInstance().setOnInventoryDataListener(new OnInventoryDataListener() {
                @Override
                public void onInventoryData(List<UHFTagEntity> tagEntityList) {
                    Log.d(TAG, "onInventoryData called");
                    Log.d(TAG, "Received tag list size: " + tagEntityList.size());

                    if (tagEntityList != null && tagEntityList.size() > 0) {
                        for (UHFTagEntity tagEntity : tagEntityList) {
                            String epcHex = tagEntity.getEcpHex();
                            Log.d(TAG, "Tag EPC: " + epcHex);

                            if (!TextUtils.isEmpty(epcHex) && epcHex.startsWith(singleRead)) {
                                runOnUiThread(() -> etEpcNumber.setText(epcHex));
                                Log.d(TAG, "Tag found: " + epcHex);

                                UHFReader.getInstance().stopInventory();
                                Log.d(TAG, "Inventory stopped");
                                return;
                            }
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(AddTagActivity.this, "No tag found", Toast.LENGTH_SHORT).show());
                        Log.d(TAG, "No tag found");
                    }
                }
            });
        } else if ("stop".equals(singleRead)) {
            Log.d(TAG, "Stopping inventory");

            // Stop the inventory
            UHFReader.getInstance().stopInventory();
            Log.d(TAG, "stopInventory called");
        }

        // Update the state and show a toast message
        String newState = "start".equals(singleRead) ? "stop" : "start";
        stringSingleRead = newState;
        String message = "start".equals(newState) ? "Inventory started successfully" : "Inventory stopped";
        Toast.makeText(AddTagActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveTag() {
        String epcNumber = etEpcNumber.getText().toString().trim();
        String machineName = etMachineName.getText().toString().trim();
        String cellName = etCellName.getText().toString().trim();
        String operation = etOperation.getText().toString().trim();
        String assetCode = etAssetCode.getText().toString().trim();

        if (epcNumber.isEmpty() || machineName.isEmpty() || cellName.isEmpty() || operation.isEmpty() || assetCode.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        } else {
            if (isEpcNumberExists(epcNumber)) {
                showReplaceDialog(epcNumber, machineName, cellName, operation, assetCode);
            } else {
                saveToExcel(epcNumber, machineName, cellName, operation, assetCode);
                Toast.makeText(this, "Tag saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean isEpcNumberExists(String epcNumber) {
        File file = new File(getExternalFilesDir(null), "tags.xlsx");
        if (!file.exists()) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(category); // Use category as sheet name
            if (sheet != null) {
                for (Row row : sheet) {
                    Cell cell = row.getCell(0);
                    if (cell != null && cell.getStringCellValue().equals(epcNumber)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading Excel file", e);
        }

        return false;
    }

    private void showReplaceDialog(String epcNumber, String machineName, String cellName, String operation, String assetCode) {
        new AlertDialog.Builder(this)
                .setTitle("EPC Number Exists")
                .setMessage("This EPC number already exists. Do you want to replace it?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    updateExcel(epcNumber, machineName, cellName, operation, assetCode);
                    Toast.makeText(this, "Tag updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void updateExcel(String epcNumber, String machineName, String cellName, String operation, String assetCode) {
        File file = new File(getExternalFilesDir(null), "tags.xlsx");

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(category); // Use category as sheet name
            if (sheet != null) {
                for (Row row : sheet) {
                    Cell cell = row.getCell(0);
                    if (cell != null && cell.getStringCellValue().equals(epcNumber)) {
                        row.getCell(1).setCellValue(machineName);
                        row.getCell(2).setCellValue(cellName);
                        row.getCell(3).setCellValue(operation);
                        row.getCell(4).setCellValue(assetCode);
                        break;
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error updating Excel file", e);
        }
    }

    private void saveToExcel(String epcNumber, String machineName, String cellName, String operation, String assetCode) {
        File file = new File(getExternalFilesDir(null), "tags.xlsx");
        Workbook workbook;
        Sheet sheet;

        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheet(category); // Use category as sheet name
            } catch (IOException e) {
                Log.e(TAG, "Error reading existing Excel file", e);
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet(category); // Create new sheet with category name
            }
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet(category); // Create new sheet with category name
        }

        if (sheet == null) {
            sheet = workbook.createSheet(category); // Create sheet if not exists
        }

        int lastRowNum = sheet.getLastRowNum();
        Row row = sheet.createRow(lastRowNum + 1);
        row.createCell(0).setCellValue(epcNumber);
        row.createCell(1).setCellValue(machineName);
        row.createCell(2).setCellValue(cellName);
        row.createCell(3).setCellValue(operation);
        row.createCell(4).setCellValue(assetCode);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        } catch (IOException e) {
            Log.e(TAG, "Error saving Excel file", e);
        }
    }
}
