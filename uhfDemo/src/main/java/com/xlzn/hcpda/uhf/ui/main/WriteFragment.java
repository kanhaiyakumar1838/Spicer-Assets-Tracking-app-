package com.xlzn.hcpda.uhf.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.xlzn.hcpda.uhf.MainActivity;
import com.xlzn.hcpda.uhf.R;
import com.xlzn.hcpda.uhf.UHFReader;
import com.xlzn.hcpda.uhf.entity.SelectEntity;
import com.xlzn.hcpda.uhf.entity.UHFReaderResult;

public class WriteFragment extends MyFragment implements View.OnClickListener {
    private MainActivity mainActivity;
    private EditText etAddressWrite;
    private EditText etPWDWrite;
    private EditText etDataWrite;
    private EditText etLenWrite;
    private Spinner spMembankWrite;
    private Button btnWrite;
    private CheckBox cbSelectWrite;
    private SelectEntity selectEntity = null;
    private CheckBox cb_writeZh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_write, container, false);
    }

    @Override
    public void onKeyDownTo(int keycode) {
        super.onKeyDownTo(keycode);
        if (keycode == 287 || keycode == 286) {
            write();
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        etAddressWrite = mainActivity.findViewById(R.id.etAddressWrite);
        etPWDWrite = mainActivity.findViewById(R.id.etPWDWrite);
        etDataWrite = mainActivity.findViewById(R.id.etDataWrite);
        etLenWrite = mainActivity.findViewById(R.id.etLenWrite);
        spMembankWrite = mainActivity.findViewById(R.id.spMembankWrite);
        btnWrite = mainActivity.findViewById(R.id.btnWrite);
        cbSelectWrite = mainActivity.findViewById(R.id.cbSelectWrite);
        cb_writeZh = mainActivity.findViewById(R.id.cb_write_zh);
        btnWrite.setOnClickListener(this);
        cbSelectWrite.setOnClickListener(this);
        cb_writeZh.setOnClickListener(this);
        spMembankWrite.setSelection(1);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cb_write_zh:
                write();
                break;
            case R.id.btnWrite:
                write();
                break;
            case R.id.cbSelectWrite:
                if (cbSelectWrite.isChecked()) {
                    showDialog(mainActivity);
                }
                break;
        }
    }

    int dd = 121;

    public void write() {
        String hexData = etDataWrite.getText().toString();
        if (cb_writeZh.isChecked()) {
            String hexData1 = getHexData(hexData);
            etAddressWrite.setText("1");
            hexData1 = getPC(hexData1)+hexData1;
            Log.e("TAG", "最终写入: " + hexData1);
            hexData = hexData1;
        }

        String password = etPWDWrite.getHint().toString();
        if (!TextUtils.isEmpty(etPWDWrite.getText())) {
            password = etPWDWrite.getText().toString();
            if (password.length() != 8) {
                Toast.makeText(mainActivity, R.string.tips, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        int address = Integer.parseInt(etAddressWrite.getHint().toString());
        if (!TextUtils.isEmpty(etAddressWrite.getText())) {
            address = Integer.parseInt(etAddressWrite.getText().toString());
        }
        int wordCount = Integer.parseInt(etLenWrite.getHint().toString());
        if (!TextUtils.isEmpty(etLenWrite.getText())) {
            wordCount = Integer.parseInt(etLenWrite.getText().toString());
        }

        wordCount = hexData.length()/4;
        Log.e("TAG", "写入长度: " + wordCount  );
        if (hexData == null || hexData.length() == 0) {
            Toast.makeText(mainActivity, R.string.no, Toast.LENGTH_SHORT).show();
            return;
        }
        if (wordCount > hexData.length() / 4) {
            Toast.makeText(mainActivity, R.string.nono, Toast.LENGTH_SHORT).show();
            return;
        }
        int membank = spMembankWrite.getSelectedItemPosition();
        UHFReaderResult<Boolean> readerResult = null;
        if (cbSelectWrite.isChecked()) {
            //查找指定标签
            readerResult = UHFReader.getInstance().write(password, membank, address, wordCount, hexData, selectEntity);
        } else {
            readerResult = UHFReader.getInstance().write(password, membank, address, wordCount, hexData, null);
        }

        if (readerResult.getResultCode() != UHFReaderResult.ResultCode.CODE_SUCCESS) {
            Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
            return;
        }
        dd = dd + 1;
        Log.e("TAG", "write: " + dd);
        etDataWrite.setText("0800F"+dd);
        Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
    }

    private String getHexData(String hexData) {
        String hex = encode(hexData);
        Log.e("TAG", "getHexData: " + hex);
        int i = 13;
        Log.e("TAG", "getHexData: " + i % 4);
        if (hex.length() % 4 == 0) {
            return hex;
        } else {
            return  getS(hex);
        }

    }

    private String getS(String hex) {
        int i = hex.length() % 4;
        Log.e("TAG", "长度是: " + hex.length());
        Log.e("TAG", "要补几个0: " + i);
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < i; j++) {
            builder.append("0");
        }
        Log.e("TAG", "最终补出来: " + hex + builder);
        return hex + builder;
    }

//    private String getData(String data) {
//
//    }

    private String getPC(String data) {
        if (data.length() == 4) {
            return "0800";
        }
        if (data.length() == 8) {
            return "1000";
        }
        if (data.length() == 12) {
            return "1800";
        }
        if (data.length() == 16) {
            return "2000";
        }
        if (data.length() == 20) {
            return "2800";
        }
        if (data.length() == 24) {
            return "3000";
        }
        if (data.length() == 28) {
            return "3800";
        }
        if (data.length() == 32) {
            return "4000";
        }
        return "3000";
    }

    private static String hexString = "0123456789ABCDEFabcdef";

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

    private void showDialog(final Context mainActivity) {
        final View view = LayoutInflater.from(mainActivity).inflate(R.layout.select, null);
        final EditText etaddr = view.findViewById(R.id.etSelectAddress);
        final EditText etlen = view.findViewById(R.id.etSelectLen);
        final EditText etdata = view.findViewById(R.id.etSelectData);
        final Spinner spSelectMembank = view.findViewById(R.id.spSelectMembank);
        etdata.requestFocus();
        if (selectEntity != null) {
            etaddr.setText(selectEntity.getAddress() + "");
            etlen.setText(selectEntity.getLength() + "");
            etdata.setText(selectEntity.getData() + "");
        }

        final AlertDialog dialog = new AlertDialog.Builder(mainActivity).setView(view).setTitle(R.string.select)
                .setPositiveButton(R.string.sure, null)
                .setNegativeButton(R.string.cancle, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        cbSelectWrite.setChecked(false);
                    }
                }).create();
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int address = Integer.parseInt(etaddr.getHint().toString());
                if (!TextUtils.isEmpty(etaddr.getText())) {
                    address = Integer.parseInt(etaddr.getText().toString());
                }
                int len = Integer.parseInt(etlen.getHint().toString());
                if (!TextUtils.isEmpty(etlen.getText())) {
                    len = Integer.parseInt(etlen.getText().toString());
                }
                if (len == 0) {
                    dialog.dismiss();
                    selectEntity.setLength(0);
                    return;
                }

                String data = etdata.getText().toString();
                int bytesLen = len / 8;
                if (len % 8 > 0) {
                    bytesLen++;
                }

                if (data == null) {
                    Toast.makeText(mainActivity, R.string.no, Toast.LENGTH_SHORT).show();
                    cbSelectWrite.setChecked(false);
                    return;
                }
                if (data.length() / 2 < bytesLen) {
                    Toast.makeText(mainActivity, R.string.nono, Toast.LENGTH_SHORT).show();
                    cbSelectWrite.setChecked(false);
                    return;
                }
                int option = 4;
                int epc = 4;
                int tid = 2;
                int user = 3;
                if (spSelectMembank.getSelectedItemPosition() == 0) {
                    option = epc;
                } else if (spSelectMembank.getSelectedItemPosition() == 1) {
                    option = tid;
                } else if (spSelectMembank.getSelectedItemPosition() == 2) {
                    option = user;
                }
                selectEntity = new SelectEntity();
                selectEntity.setOption(option);
                selectEntity.setLength(len);
                selectEntity.setAddress(address);
                selectEntity.setData(data);
                dialog.dismiss();
            }
        });
    }

}
