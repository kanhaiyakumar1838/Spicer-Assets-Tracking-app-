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
import com.xlzn.hcpda.uhf.enums.LockActionEnum;
import com.xlzn.hcpda.uhf.enums.LockMembankEnum;


public class LockFragment extends MyFragment implements View.OnClickListener{

    private MainActivity  mainActivity;
    private EditText etPWDLock;
    private CheckBox cbSelectLock;
    private Button btnLock;
    Spinner spLockBank,spLockAction;
    private SelectEntity selectEntity=null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lock, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity=(MainActivity)getActivity();
        cbSelectLock=mainActivity.findViewById(R.id.cbSelectLock);
        etPWDLock=mainActivity.findViewById(R.id.etPWDLock);
        spLockAction=mainActivity.findViewById(R.id.spLockAction);
        spLockBank=mainActivity.findViewById(R.id.spLockBank);
        btnLock=mainActivity.findViewById(R.id.btnLock);
        btnLock.setOnClickListener(this);
        cbSelectLock.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnLock:
                String password = etPWDLock.getHint().toString();
                if (!TextUtils.isEmpty(etPWDLock.getText())) {
                    password = etPWDLock.getText().toString();
                    if (password.length() != 8) {
                        Toast.makeText(mainActivity, R.string.tips, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                LockMembankEnum membankEnum =LockMembankEnum.getValue(spLockBank.getSelectedItemPosition());
                LockActionEnum actionEnum=LockActionEnum.getValue(spLockAction.getSelectedItemPosition());
                UHFReaderResult<Boolean>readerResult=null;
                if(cbSelectLock.isChecked()) {
                    readerResult= UHFReader.getInstance().lock(password, membankEnum, actionEnum,selectEntity);
                }else{
                    readerResult= UHFReader.getInstance().lock(password, membankEnum, actionEnum, null);
                }

                if (readerResult.getResultCode() != UHFReaderResult.ResultCode.CODE_SUCCESS) {
                    Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
                break;
            case R.id.cbSelectLock:
                if(cbSelectLock.isChecked()){
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

        final AlertDialog dialog = new AlertDialog.Builder(mainActivity).setView(view).setTitle(R.string.select)
                .setPositiveButton(R.string.sure, null)
                .setNegativeButton(R.string.cancle, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        cbSelectLock.setChecked(false);
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
                    Toast.makeText(mainActivity,R.string.no, Toast.LENGTH_SHORT).show();
                    cbSelectLock.setChecked(false);
                    return;
                }
                if(data.length()/2 < bytesLen){
                    Toast.makeText(mainActivity,R.string.nono, Toast.LENGTH_SHORT).show();
                    cbSelectLock.setChecked(false);
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