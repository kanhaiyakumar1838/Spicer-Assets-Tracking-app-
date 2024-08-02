package com.xlzn.hcpda.uhf.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
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


public class KillFragment extends MyFragment implements View.OnClickListener{
    private MainActivity  mainActivity;
    private EditText etPWDKill;
    private CheckBox cbSelectKill;
    private Button btnKill;
    private SelectEntity selectEntity=null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kill, container, false);
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity=(MainActivity)getActivity();
        cbSelectKill=mainActivity.findViewById(R.id.cbSelectKill);
        etPWDKill=mainActivity.findViewById(R.id.etPWDKill);
        btnKill=mainActivity.findViewById(R.id.btnKill);
        btnKill.setOnClickListener(this);
        cbSelectKill.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnKill:
                String password = etPWDKill.getHint().toString();
                if (!TextUtils.isEmpty(etPWDKill.getText())) {
                    password = etPWDKill.getText().toString();
                    if (password.length() != 8) {
                        Toast.makeText(mainActivity, R.string.tiplen, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                UHFReaderResult<Boolean> readerResult = null;
                if (cbSelectKill.isChecked()) {
                    //查找指定标签
                    readerResult = UHFReader.getInstance().kill(password, selectEntity);
                } else {
                    readerResult = UHFReader.getInstance().kill(password, null);
                }

                if (readerResult.getResultCode() != UHFReaderResult.ResultCode.CODE_SUCCESS) {
                    Toast.makeText(mainActivity,
                            R.string.killerr,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(mainActivity, R.string.successKill, Toast.LENGTH_SHORT).show();
                break;
            case R.id.cbSelectKill:
                if (cbSelectKill.isChecked()) {
                    showDialog(mainActivity);
                }

                break;
        }
    }

    private void showDialog(final Context mainActivity){
        final View view = LayoutInflater.from(mainActivity).inflate(R.layout.select, null);
        final EditText etaddr = view.findViewById(R.id.etSelectAddress);
        final EditText etlen = view.findViewById(R.id.etSelectLen);
        final EditText etdata = view.findViewById(R.id.etSelectData);
        final Spinner spSelectMembank=view.findViewById(R.id.spSelectMembank);
        etdata.requestFocus();
        if(selectEntity!=null){
            etaddr.setText(selectEntity.getAddress()+"");
            etlen.setText(selectEntity.getLength()+"");
            etdata.setText(selectEntity.getData()+"");
        }

        final AlertDialog dialog = new AlertDialog.Builder(mainActivity).setView(view).setTitle(R.string.selectTag)
                .setPositiveButton(R.string.sures, null)
                .setNegativeButton(R.string.cancles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        cbSelectKill.setChecked(false);
                    }
                }).create();
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int address=Integer.parseInt(etaddr.getHint().toString());
                if(!TextUtils.isEmpty(etaddr.getText())) {
                    address = Integer.parseInt(etaddr.getText().toString());
                }
                int len=Integer.parseInt(etlen.getHint().toString());
                if(!TextUtils.isEmpty(etlen.getText())) {
                    len = Integer.parseInt(etlen.getText().toString());
                }
                if(len==0){
                    dialog.dismiss();
                    selectEntity.setLength(0);
                    return;
                }

                String data=etdata.getText().toString();
                int bytesLen=len/8;
                if(len%8>0){
                    bytesLen++;
                }

                if(data==null){
                    Toast.makeText(mainActivity,
                            R.string.datanonull,
                            Toast.LENGTH_SHORT).show();
                    cbSelectKill.setChecked(false);
                    return;
                }
                if(data.length()/2 < bytesLen){
                    Toast.makeText(mainActivity, R.string.nolong, Toast.LENGTH_SHORT).show();
                    cbSelectKill.setChecked(false);
                    return;
                }
                int option=4;
                int epc=4;
                int tid=2;
                int user=3;
                if(spSelectMembank.getSelectedItemPosition()==0){
                    option=epc;
                }else if(spSelectMembank.getSelectedItemPosition()==1){
                    option=tid;
                }else if(spSelectMembank.getSelectedItemPosition()==2){
                    option=user;
                }
                selectEntity=new SelectEntity();
                selectEntity.setOption(option);
                selectEntity.setLength(len);
                selectEntity.setAddress(address);
                selectEntity.setData(data);
                dialog.dismiss();
            }
        });
    }

}

