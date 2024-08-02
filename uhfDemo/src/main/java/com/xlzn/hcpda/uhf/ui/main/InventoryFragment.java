package com.xlzn.hcpda.uhf.ui.main;
import com.xlzn.hcpda.uhf.AddTagActivity;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;
import com.xlzn.hcpda.jxl.FileImport;
import com.xlzn.hcpda.uhf.MainActivity;
import com.xlzn.hcpda.uhf.R;
import com.xlzn.hcpda.uhf.UHFReader;
import com.xlzn.hcpda.uhf.Utils;
import com.xlzn.hcpda.uhf.entity.SelectEntity;
import com.xlzn.hcpda.uhf.entity.UHFReaderResult;
import com.xlzn.hcpda.uhf.entity.UHFTagEntity;
import com.xlzn.hcpda.uhf.interfaces.OnInventoryDataListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
// Import necessary packages for reading Excel files
import org.apache.poi.ss.usermodel.*;
//libraries for reading csv files

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;



public class InventoryFragment extends MyFragment implements View.OnClickListener {
    private MainActivity mainActivity = null;
    private int fetch=0;

    private RecyclerView recyclerview = null;
    private Button btExport;
    private Button btClear, btStartStop, btnSingle;
    private MyAdapter myAdapter;
    private int count = 0;
    private long startTime;
    private TextView tvNumber, tvTime, tvCount,tvScannedTime;
    private TextView tv_80, tv_90, tv_100;
    long stopTime;
    EditText et_stopTime;
    CheckBox cb_stopTime;
    private SelectEntity selectEntity = null;
    boolean needStop = false;
    public boolean isAscii = false;
    CheckBox cb_ascii;
    private List<UHFTagEntity> tagEntityList = new ArrayList<>();
    EditText et_singleRead;
    private String category;



    public int findRowNumberByEpcNumber(String epcNumber) throws IOException {
        try (InputStream inputStream = getResources().openRawResource(R.raw.rfid)) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            int rowsCount = sheet.getPhysicalNumberOfRows();
            for (int rowIndex = 1; rowIndex < rowsCount; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell cell = row.getCell(0);
                    if (cell != null) {
                        String newEpc = cell.getStringCellValue();
                        if (Objects.equals(newEpc.trim(), epcNumber.trim())) {
                            return rowIndex;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private String extractValueFromExcel(int columnIndex, int rowIndex) throws IOException {
        try (InputStream inputStream = getResources().openRawResource(R.raw.rfid)) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    return cell.getStringCellValue();
                }
            }
        }
        return null;
    }

    private String extractMachineNameFromExcel(int rowIndex) throws IOException {
        return extractValueFromExcel(1, rowIndex); // Assuming machine name is in the second column (index 1)
    }

    private String extractOperationFromExcel(int rowIndex) throws IOException {
        return extractValueFromExcel(3, rowIndex); // Assuming operation is in the fourth column (index 3)
    }
    private String extractCellNameFromExcel(Integer rowIndex) throws IOException {
        return extractValueFromExcel(2,rowIndex); // Assuming cell name is in the third column (index 2)
    }
    private String extractAssetCodeFromExcel(int rowIndex) throws IOException {
        return extractValueFromExcel(4,rowIndex); // Assuming asset code is in the fifth column (index 4)
    }


    Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 1) {
                UHFTagEntity uhfTagEntity = (UHFTagEntity) msg.obj;
                count++;
                boolean isFlag = false;
                for (UHFTagEntity entity : tagEntityList) {
                    if (entity.getEcpHex().equals(uhfTagEntity.getEcpHex())) {
                        entity.setCount(entity.getCount() + uhfTagEntity.getCount());
                        isFlag = true;
                        break;
                    }
                }
                if (!isFlag) {
                    tagEntityList.add(uhfTagEntity);
                }
                tvCount.setText(count + "");
                tvNumber.setText(tagEntityList.size() + "");
                if (tagEntityList.size() == 80) {
                    long time = SystemClock.elapsedRealtime() - startTime;
                    tv_80.setText(time + "ms");
                }
                if (tagEntityList.size() == 90) {
                    long time = SystemClock.elapsedRealtime() - startTime;
                    tv_90.setText(time + "ms");
                }
                if (tagEntityList.size() == 100) {
                    long time = SystemClock.elapsedRealtime() - startTime;
                    tv_100.setText(time + "ms");
                }

                // Format scanned time to IST and set it to the tag entity
                String scannedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String scannedTimeInIST = convertToIST(scannedTime);
                uhfTagEntity.setScannedTime(scannedTime);
                if (myAdapter != null) {
                    myAdapter.notifyDataSetChanged();
                }


            } else {
                handler.sendEmptyMessageDelayed(2, 100);
                long time = SystemClock.elapsedRealtime() - startTime;
                Log.e("TAG", "停止时间: " + stopTime);
                Log.e("TAG", "现在时间: " + time / 1000);
                if (needStop) {
                    if (stopTime == time / 1000) {
//                    UHFReader.getInstance().stopInventory();
                        singleRead = et_singleRead.getText().toString().trim();
                        if (singleRead != null) {
                            startStop(singleRead);
                        } else {
                            startStop();
                        }
                    }
                }
                tvTime.setText(time / 1000 + "");
            }

        }

        private String convertToIST(String dateTime) {
            try {
                SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = utcFormat.parse(dateTime);

                SimpleDateFormat istFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                istFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                return istFormat.format(date);
            } catch (Exception e) {
                e.printStackTrace();
                return dateTime;
            }
        }





    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_inventory, container, false);
        // Retrieve category from arguments
        if (getArguments() != null) {
            category = getArguments().getString("category");
        }

        // Use the category value as needed
        Log.d("InventoryFragment", "Category received: " + category);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        Bundle args = getArguments();
        if (args != null) {
            fetch = args.getInt("fetch", 0);
            Log.d("InventoryFragment", "Fetch value: " + fetch);
        } else {
            Log.d("InventoryFragment", "Arguments are null");
            Log.d("InventoryFragment", "Fetch value: " + fetch);
        }

        // Check if the activity is an instance of MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            // Now you can call methods from MainActivity
        }
        // Check if the activity is an instance of AddTagActivity
        else if (getActivity() instanceof AddTagActivity) {
            AddTagActivity addTagActivity = (AddTagActivity) getActivity();
            // Now you can call methods from AddTagActivity
        } else {
            throw new IllegalStateException("Unexpected activity type");
        }
        btExport = mainActivity.findViewById(R.id.btExport);
        recyclerview = mainActivity.findViewById(R.id.recyclerview);
        btClear = mainActivity.findViewById(R.id.btClear);
        btStartStop = mainActivity.findViewById(R.id.btStartStop);
        btnSingle = mainActivity.findViewById(R.id.btnSingle);
        tvNumber = mainActivity.findViewById(R.id.tvNumber);
        tvTime = mainActivity.findViewById(R.id.tvTime);
        cb_stopTime = mainActivity.findViewById(R.id.cb_stopTime);
        et_stopTime = mainActivity.findViewById(R.id.et_stopTime);
        cb_ascii = mainActivity.findViewById(R.id.cb_ascii);
        tvCount = mainActivity.findViewById(R.id.tvCount);
        tv_80 = mainActivity.findViewById(R.id.tv_80);
        tv_90 = mainActivity.findViewById(R.id.tv_90);
        tv_100 = mainActivity.findViewById(R.id.tv_100);
        et_singleRead = mainActivity.findViewById(R.id.et_singleRead);

        //设置LayoutManager，以LinearLayoutManager为例子进行线性布局
        recyclerview.setLayoutManager(new LinearLayoutManager(mainActivity));
        //设置分割线
        recyclerview.addItemDecoration(new DividerItemDecoration(mainActivity, LinearLayoutManager.VERTICAL));
//        //创建适配器
//        myAdapter = new MyAdapter(tagEntityList,isAscii);
//        //设置适配器
//        recyclerview.setAdapter(myAdapter);

        btnSingle.setOnClickListener(this);
        btStartStop.setOnClickListener(this);
        btClear.setOnClickListener(this);
        btExport.setOnClickListener(this);


        myAdapter = new MyAdapter(tagEntityList, isAscii);
        //设置适配器
        recyclerview.setAdapter(myAdapter);
        Log.e("TAG", "onActivityCreated: 盘");

        cb_stopTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.e("TAG", "onCheckedChanged: " + isChecked);
                needStop = isChecked;
            }
        });

        cb_ascii.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isAscii = isChecked;
            }
        });
    }

    @Override
    public void onKeyDownTo(int keycode) {
        super.onKeyDownTo(keycode);
        singleRead = et_singleRead.getText().toString().trim();
        if (singleRead != null) {
            startStop(singleRead);
        } else {
            startStop();
        }
    }
    String singleRead = "";
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btClear:
                clear();
                break;
            case R.id.btStartStop:
                // UHFReader.getInstance().setInventoryModeForPower(InventoryModeForPower.POWER_SAVING_MODE);
                singleRead = et_singleRead.getText().toString().trim();
                if (singleRead != null) {
                    startStop(singleRead);
                } else {
                    startStop();
                }
                break;
            case R.id.btExport:

                new XPopup.Builder(mainActivity).asInputConfirm("名称相同将覆盖之前的文件", "导出文件根目录Download下", "epc", new OnInputConfirmListener() {
                    @Override
                    public void onConfirm(String text) {
                        String fileName = text.trim();
                        if (TextUtils.isEmpty(fileName)) {
                            fileName = "epc.xls";
                        } else {
                            fileName = fileName + ".xls";
                        }
                        new ExPortTask().execute(fileName);

                    }
                }).show();

                break;
            case R.id.btnSingle:
                UHFReaderResult<UHFTagEntity> uhfTagEntityUHFReaderResult = null;
                uhfTagEntityUHFReaderResult = UHFReader.getInstance().singleTagInventory();
                if (uhfTagEntityUHFReaderResult.getResultCode() != UHFReaderResult.ResultCode.CODE_SUCCESS) {
                    Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_SHORT).show();
                    return;
                }
                Message message = new Message();
                message.what = 1;
                message.obj = uhfTagEntityUHFReaderResult.getData();
                handler.sendMessage(message);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        btStartStop.setText(R.string.starts);
        UHFReader.getInstance().stopInventory();
        handler.removeMessages(2);
    }

    private void startStop(final String singleRead) {
        Log.e("TAG", "startStop: " + btStartStop.getText());
        Log.e("TAG", "startStop: " + getResources().getString(R.string.start));

        // Start Inventory
        if (btStartStop.getText().equals(getResources().getString(R.string.start))) {
            if (fetch == 1) {
                // Fetch mode: Handle fetching the EPC
                UHFReader.getInstance().setOnInventoryDataListener(new OnInventoryDataListener() {
                    @Override
                    public void onInventoryData(List<UHFTagEntity> tagEntityList) {
                        Log.e("UTU", "onInventoryData: 一次回调--------  " + tagEntityList.size());
                        if (tagEntityList != null && !tagEntityList.isEmpty()) {
                            UHFTagEntity tag = tagEntityList.get(0);
                            if (!TextUtils.isEmpty(tag.getEcpHex())) {
                                String epcHex = tag.getEcpHex();
                                Log.d("UTU", "EPC fetched: " + epcHex);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "EPC: " + epcHex, Toast.LENGTH_SHORT).show());
                                }
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("epcNumber", epcHex);
                                getActivity().setResult(Activity.RESULT_OK, resultIntent);
                                getActivity().finish(); // Finish MainActivity to return to AddTagActivity
                                return;
                            }
                        }
                    }
                });
            } else if (fetch == 0) {
                // Normal mode: Handle normal inventory process
                UHFReader.getInstance().setOnInventoryDataListener(new OnInventoryDataListener() {
                    @Override
                    public void onInventoryData(List<UHFTagEntity> tagEntityList) {
                        Log.e("UTU", "onInventoryData: 一次回调--------  " + tagEntityList.size());
                        if (tagEntityList != null && !tagEntityList.isEmpty()) {
                            for (UHFTagEntity tag : tagEntityList) {
                                if (!TextUtils.isEmpty(tag.getEcpHex()) && tag.getEcpHex().startsWith(singleRead)) {
                                    int rowIndex;
                                    try {
                                        Log.d("UTU", "EPC being sent to findRowNumberByEpcNumber: " + tag.getEcpHex());
                                        rowIndex = findRowNumberByEpcNumber(tag.getEcpHex());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Log.d("UTU", "rowIndex returned by findRowNumberByEpcNumber: " + rowIndex);
                                    if (rowIndex != -1) {
                                        try {
                                            tag.setMachineName(extractMachineNameFromExcel(rowIndex));
                                            tag.setOperation(extractOperationFromExcel(rowIndex));
                                            tag.setCellName(extractCellNameFromExcel(rowIndex));
                                            tag.setAssetCode(extractAssetCodeFromExcel(rowIndex));
                                            Log.d("InventoryFragment2", "MachineName: " + tag.getMachineName() + ", Operation: " + tag.getOperation());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    Message message = new Message();
                                    message.what = 1;
                                    message.obj = tag;
                                    handler.sendMessage(message);
                                    Utils.play();
                                }
                            }
                        }
                    }
                });
                myAdapter = new MyAdapter(tagEntityList, isAscii);
                recyclerview.setAdapter(myAdapter);
            }

            // Start Inventory
            UHFReaderResult<Boolean> readerResult = UHFReader.getInstance().startInventory();
            if (readerResult.getData()) {
                String trim = et_stopTime.getText().toString().trim();
                if (trim.isEmpty()) {
                    trim = "800000";
                }
                stopTime = Long.parseLong(trim);
                handler.sendEmptyMessageDelayed(2, 100);
                startTime = SystemClock.elapsedRealtime();
                getActivity().runOnUiThread(() -> {
                    btStartStop.setText(R.string.stop);
                    btnSingle.setEnabled(false);
                });
            } else {
                getActivity().runOnUiThread(() -> Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show());
            }
        } else {
            // Stop Inventory
            getActivity().runOnUiThread(() -> {
                btStartStop.setText(R.string.start);
                btnSingle.setEnabled(true);
            });
            UHFReaderResult<Boolean> booleanUHFReaderResult = UHFReader.getInstance().stopInventory();
            handler.removeMessages(2);
        }
    }

    private void startStop() {
        long time = SystemClock.elapsedRealtime() - startTime;
        if (btStartStop.getText().equals(getResources().getString(R.string.start))) {
            UHFReader.getInstance().setOnInventoryDataListener(new OnInventoryDataListener() {
                @Override
                public void onInventoryData(List<UHFTagEntity> tagEntityList) {
                    Log.e("UTU", "onInventoryData: 一次回调--------  " + tagEntityList.size());
                    if (tagEntityList != null && !tagEntityList.isEmpty()) {
                        if (fetch == 1) {
                            // Fetch mode: Handle fetching the EPC
                            UHFTagEntity tag = tagEntityList.get(0); // Assuming you want the first tag
                            if (!TextUtils.isEmpty(tag.getEcpHex())) {
                                String epcHex = tag.getEcpHex();
                                Log.d("UTU", "EPC fetched: " + epcHex);
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("epcNumber", epcHex);
                                getActivity().setResult(Activity.RESULT_OK, resultIntent);
                                getActivity().finish(); // Finish MainActivity to return to AddTagActivity
                                return;
                            }
                        } else {
                            // Normal mode: Handle normal inventory process
                            for (UHFTagEntity tagEntity : tagEntityList) {
                                if (!TextUtils.isEmpty(tagEntity.getEcpHex())) {
                                    Message message = new Message();
                                    message.what = 1;
                                    message.obj = tagEntity;
                                    handler.sendMessage(message);
                                    Utils.play();
                                }
                            }
                            // Update the adapter
                            myAdapter = new MyAdapter(tagEntityList, isAscii);
                            getActivity().runOnUiThread(() -> recyclerview.setAdapter(myAdapter));
                        }
                    }
                }
            });

            UHFReaderResult<Boolean> readerResult = UHFReader.getInstance().startInventory();
            if (readerResult.getData()) {
                String trim = et_stopTime.getText().toString().trim();
                if (trim.isEmpty()) {
                    trim = "800000";
                }
                stopTime = Long.parseLong(trim);
                handler.sendEmptyMessageDelayed(2, 100);
                startTime = SystemClock.elapsedRealtime();
                getActivity().runOnUiThread(() -> {
                    btStartStop.setText(R.string.stop);
                    btnSingle.setEnabled(false);
                });
            } else {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.fail, Toast.LENGTH_SHORT).show());
            }
        } else {
            getActivity().runOnUiThread(() -> {
                btStartStop.setText(R.string.start);
                btnSingle.setEnabled(true);
            });
            UHFReaderResult<Boolean> booleanUHFReaderResult = UHFReader.getInstance().stopInventory();
            handler.removeMessages(2);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void clear() {
        tagEntityList.clear();
        tvNumber.setText("0");
        tvTime.setText("0");
        tvCount.setText("0");
        startTime = SystemClock.elapsedRealtime();
        count = 0;
        if (myAdapter != null) {

            myAdapter.notifyDataSetChanged();
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<UHFTagEntity> dataList;
        boolean isAsciiMy;

        MyAdapter(List<UHFTagEntity> dataList, boolean isAscii) {
            this.dataList = dataList;
            this.isAsciiMy = isAscii;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UHFTagEntity entity = dataList.get(position);
            holder.tvID.setText((position + 1) + "");
            if (entity.getTidHex() != null) {
                holder.tvEPC.setText("EPC:" + entity.getEcpHex() + "\nTID:" + entity.getTidHex());
            } else {
                if (isAsciiMy) {
                    holder.tvEPC.setText(decode(entity.getEcpHex()));
                } else {
                    String ecpHex = entity.getEcpHex();
                    holder.tvEPC.setText((ecpHex));
                }
            }
            holder.tvRssi.setText("RSSI: " + entity.getRssi());
            holder.tvCount.setText("Count: " + entity.getCount());
            holder.tvMachineName.setText("Machine Name: " + entity.getMachineName());
            holder.tvOperation.setText("Operation: " + entity.getOperation());
            holder.tvCellName.setText("Cell Name: " + entity.getCellName()); // Bind CellName
            holder.tvAssetCode.setText("Asset Code: " + entity.getAssetCode()); // Bind AssetCode
            holder.tvScannedTime.setText("Scanned Time: " + entity.getScannedTime());
        }


        @Override
        public int getItemCount() {
            return dataList == null ? 0 : dataList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvID;
            private TextView tvEPC;
            private TextView tvRssi;
            private TextView tvCount;
            private TextView tvMachineName; // New TextView
            private TextView tvOperation;   // New TextView
            private TextView tvCellName; // New TextView
            private TextView tvAssetCode; // New TextView
            private TextView tvScannedTime;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvID = itemView.findViewById(R.id.tvID);
                tvEPC = itemView.findViewById(R.id.tvEPC);
                tvRssi = itemView.findViewById(R.id.tvRssi);
                tvCount = itemView.findViewById(R.id.tvCount);
                tvMachineName = itemView.findViewById(R.id.tvMachineName); // Initialize new TextView
                tvOperation = itemView.findViewById(R.id.tvOperation);     // Initialize new TextView
                tvCellName = itemView.findViewById(R.id.tvCellName); // Initialize new TextView
                tvAssetCode = itemView.findViewById(R.id.tvAssetCode);
                tvScannedTime = itemView.findViewById(R.id.tvScannedTime);

            }
        }
    }

    public class ExPortTask extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog progressDialog;

        @Override
        protected Boolean doInBackground(String... params) {
            return FileImport.daochu(params[0], tagEntityList);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                Toast.makeText(mainActivity, "导出成功", Toast.LENGTH_SHORT).show();
                clear();
            } else {
                Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
            }
            progressDialog.cancel();
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            progressDialog = new ProgressDialog(mainActivity);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("导出中...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }
    }


    /*
     * 将字符串编码成16进制数字,适用于所有字符（包括中文）
     */
    public static String encode(String str) {
        // 根据默认编码获取字节数组
        byte[] bytes = str.getBytes();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        // 将字节数组中每个字节拆解成2位16进制整数
        for (int i = 0; i < bytes.length; i++) {
            sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
            sb.append(hexString.charAt((bytes[i] & 0x0f) >> 0));
        }
        return sb.toString();
    }

    private static String hexString = "0123456789ABCDEFabcdef";

    /*
     * 将16进制数字解码成字符串,适用于所有字符（包括中文）
     */
    public static String decode(String bytes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                bytes.length() / 2);
        // 将每2位16进制整数组装成一个字节 5731363939303030323334
        for (int i = 0; i < bytes.length(); i += 2)
            baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString
                    .indexOf(bytes.charAt(i + 1))));
        return new String(baos.toByteArray());
    }

}
