package com.xlzn.hcpda.uhf.ui.main;

import static com.xlzn.hcpda.uhf.R.string.tips;

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

public class ReadFragment extends MyFragment implements View.OnClickListener {

    private MainActivity mainActivity;
    private EditText etAddressRead;
    private EditText etPWDRead;
    private EditText etDataRead;
    private EditText etLenRead;
    private Spinner spMembankRead;
    private Button btnRead;
    private CheckBox cbSelectRead;
    private SelectEntity selectEntity = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_read, container, false);
    }

    @Override
    public void onKeyDownTo(int keycode) {
        super.onKeyDownTo(keycode);
        if (keycode == 287 || keycode == 286) {
            read();
            Log.e("TAG", "onKeyDownTo: read"  );
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        etAddressRead = mainActivity.findViewById(R.id.etAddressRead);
        etPWDRead = mainActivity.findViewById(R.id.etPWDRead);
        etDataRead = mainActivity.findViewById(R.id.etDataRead);
        etLenRead = mainActivity.findViewById(R.id.etLenRead);
        spMembankRead = mainActivity.findViewById(R.id.spMembankRead);
        btnRead = mainActivity.findViewById(R.id.btnRead);
        cbSelectRead = mainActivity.findViewById(R.id.cbSelectRead);
        btnRead.setOnClickListener(this);
        cbSelectRead.setOnClickListener(this);
        spMembankRead.setSelection(1);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnRead:
              read();
                break;
            case R.id.cbSelectRead:
                if (cbSelectRead.isChecked()) {
                    showDialog(mainActivity);
                }
                break;
        }
    }

    public void read() {
        String password = etPWDRead.getHint().toString();
        if (!TextUtils.isEmpty(etPWDRead.getText())) {
            password = etPWDRead.getText().toString();
            if (password.length() != 8) {
                Toast.makeText(mainActivity, tips, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        int address = Integer.parseInt(etAddressRead.getHint().toString());
        if (!TextUtils.isEmpty(etAddressRead.getText())) {
            address = Integer.parseInt(etAddressRead.getText().toString());
        }
        int wordCount = Integer.parseInt(etLenRead.getHint().toString());
        if (!TextUtils.isEmpty(etLenRead.getText())) {
            wordCount = Integer.parseInt(etLenRead.getText().toString());
        }

        int membank = spMembankRead.getSelectedItemPosition();
        UHFReaderResult<String> readerResult = null;
        if (cbSelectRead.isChecked()) {
            //查找指定标签
            readerResult = UHFReader.getInstance().read(password, membank, address, wordCount, selectEntity);
        } else {
            readerResult = UHFReader.getInstance().read(password, membank, address, wordCount, null);
        }
        if (readerResult.getResultCode() != UHFReaderResult.ResultCode.CODE_SUCCESS) {
            etDataRead.setText("");
            Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
        etDataRead.setText(readerResult.getData());
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
                        cbSelectRead.setChecked(false);
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
                    cbSelectRead.setChecked(false);
                    return;
                }
                if (data.length() / 2 < bytesLen) {
                    Toast.makeText(mainActivity, R.string.nono, Toast.LENGTH_SHORT).show();
                    cbSelectRead.setChecked(false);
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