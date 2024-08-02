package com.xlzn.hcpda.uhf.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.xlzn.hcpda.uhf.HcPreferences;
import com.xlzn.hcpda.uhf.MainActivity;
import com.xlzn.hcpda.uhf.R;
import com.xlzn.hcpda.uhf.UHFReader;
import com.xlzn.hcpda.uhf.entity.UHFReaderResult;
import com.xlzn.hcpda.uhf.enums.UHFSession;


public class ConfigFragment extends MyFragment implements View.OnClickListener {

    private MainActivity mainActivity;
    private Button btnGetPower, btnSetPower, btnGetFrequencyBand, btnSetFrequencyBand, btnSetSession, btnGetSession;
    Button btnGetFrequencyPoint, btnSetFrequencyPoint, bt_setRFLink, bt_getRFLink;
    private Spinner spFrequencyBand, spPower, spSession, spPoint, spRFLink;
    private CheckBox cbTID, cbEPC;
    int point = 902750;
    int link = 2;
    String sp_path = "pda";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        assert mainActivity != null;
        btnGetFrequencyPoint = mainActivity.findViewById(R.id.btnGetFrequencyPoint);
        btnSetFrequencyPoint = mainActivity.findViewById(R.id.btnSetFrequencyPoint);
        spPoint = mainActivity.findViewById(R.id.spFrequencyPoint);
        spRFLink = mainActivity.findViewById(R.id.spRFLink);

        bt_getRFLink = mainActivity.findViewById(R.id.btnGetRFLink);
        bt_setRFLink = mainActivity.findViewById(R.id.btnSetRFLink);

        btnGetPower = mainActivity.findViewById(R.id.btnGetPower);
        btnSetPower = mainActivity.findViewById(R.id.btnSetPower);
        btnGetFrequencyBand = mainActivity.findViewById(R.id.btnGetFrequencyBand);
        btnSetFrequencyBand = mainActivity.findViewById(R.id.btnSetFrequencyBand);
        btnSetSession = mainActivity.findViewById(R.id.btnSetSession);
        btnGetSession = mainActivity.findViewById(R.id.btnGetSession);
        spFrequencyBand = mainActivity.findViewById(R.id.spFrequencyBand);
        spPower = mainActivity.findViewById(R.id.spPower);
        spSession = mainActivity.findViewById(R.id.spSession);
        cbTID = mainActivity.findViewById(R.id.cbTID);
        cbEPC = mainActivity.findViewById(R.id.cbEPC);

        bt_getRFLink.setOnClickListener(this);
        bt_setRFLink.setOnClickListener(this);

        btnGetPower.setOnClickListener(this);
        btnSetPower.setOnClickListener(this);
        btnGetFrequencyBand.setOnClickListener(this);
        btnSetFrequencyBand.setOnClickListener(this);
        btnSetSession.setOnClickListener(this);
        btnGetSession.setOnClickListener(this);
        btnGetFrequencyPoint.setOnClickListener(this);
        btnSetFrequencyPoint.setOnClickListener(this);
        cbTID.setOnClickListener(this);
        cbEPC.setOnClickListener(this);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(mainActivity, R.array.arrPower, android.R.layout.simple_spinner_item);
        spPower.setAdapter(adapter);
        spPower.setSelection(HcPreferences.getInstance().getInt(mainActivity, sp_path, "power")-5);
        spRFLink.setSelection(link);


    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            getFrequencyBand();
            getPower();
            getSession();
        }
    }


    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.btnSetRFLink:
                int mode = spRFLink.getSelectedItemPosition();
                link = mode;
                Log.e("TAG", "onClick: " + mode);
                UHFReaderResult<Boolean> booleanUHFReaderResult1 = UHFReader.getInstance().setRFLink(mode);
                Log.e("TAG", mode + " =====onClick: " + booleanUHFReaderResult1.getResultCode());
                if (booleanUHFReaderResult1.getData()) {
                    Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnGetRFLink:
                spRFLink.setSelection(link);
                break;
            case R.id.btnGetFrequencyPoint:
                spPoint.setSelection(point);
                break;
            case R.id.btnSetFrequencyPoint:
                String selectedItem = (String) spPoint.getSelectedItem();
                point = (spPoint.getSelectedItemPosition());
                UHFReaderResult<Boolean> booleanUHFReaderResult = UHFReader.getInstance().setFrequencyPoint(Integer.parseInt(selectedItem));
                Log.e("TAG", booleanUHFReaderResult.getData() + " = onClick: " + selectedItem);

                if (booleanUHFReaderResult.getData()) {
                    Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.cbEPC:
                if (cbEPC.isChecked()) {
                    cbTID.setChecked(false);
                    UHFReaderResult<Boolean> result = UHFReader.getInstance().setInventoryTid(false);
                    if (result.getData()) {
                        Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.cbTID:
                if (cbTID.isChecked()) {
                    cbEPC.setChecked(false);
                    UHFReaderResult<Boolean> result = UHFReader.getInstance().setInventoryTid(true);
                    if (result.getData()) {
                        Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btnGetFrequencyBand:
                boolean reuslt = getFrequencyBand();
                if (reuslt) {
                    Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnSetFrequencyBand:
                setFrequencyBand();
                break;
            case R.id.btnGetPower:
                getPower();
                break;
            case R.id.btnSetPower:
                setPower();
                break;
            case R.id.btnGetSession:
                reuslt = getSession();
                if (reuslt) {
                    Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnSetSession:
                setSession();
                break;
        }

    }

    public boolean getFrequencyBand() {
        //    北美（902-928）	0x01
        //    中国1（920-925）	0x06
        //    欧频（865-867）	0x08
        //    中国2（840-845）	0x0a
        //    全频段（840-960）	0xff
        UHFReaderResult<Integer> result = UHFReader.getInstance().getFrequencyRegion();
        if (result.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
            switch (result.getData()) {
                case 0x01:
                    spFrequencyBand.setSelection(0);
                    break;
                case 0x06:
                    spFrequencyBand.setSelection(1);
                    break;
                case 0x08:
                    spFrequencyBand.setSelection(2);
                    break;
                case 0x0a:
                    spFrequencyBand.setSelection(3);
                    break;
                case 0xff:
                    spFrequencyBand.setSelection(4);
                    break;
            }
            return true;
        }
        return false;
    }

    private void setFrequencyBand() {
        int value = 1;
        switch (spFrequencyBand.getSelectedItemPosition()) {
            case 0:
                value = 0x01;
                break;
            case 1:
                value = 0x06;
                break;
            case 2:
                value = 0x08;
                break;
            case 3:
                value = 0x0a;
                break;
            case 4:
                value = 0xff;
                break;
        }
        UHFReaderResult<Boolean> result = UHFReader.getInstance().setFrequencyRegion(value);
        if (result.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
            Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void setPower() {
        UHFReaderResult<Boolean> result = UHFReader.getInstance().setPower(spPower.getSelectedItemPosition() + 5);
        if (result.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
            HcPreferences.getInstance().set(mainActivity, sp_path, "power", spPower.getSelectedItemPosition() + 5);
            Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean getPower() {
        UHFReaderResult<Integer> result = UHFReader.getInstance().getPower();
        if (result.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
            spPower.setSelection(result.getData() - 5);
            return true;
        }
        return false;
    }

    private boolean getSession() {
        UHFReaderResult<UHFSession> result = UHFReader.getInstance().getSession();
        if (result.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {

            spSession.setSelection(result.getData().getValue());
            return true;
        }
        return false;
    }

    private void setSession() {
        UHFReaderResult<Boolean> result = UHFReader.getInstance().setSession(UHFSession.getValue(spSession.getSelectedItemPosition()));
        if (result.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
            HcPreferences.getInstance().set(mainActivity, sp_path, "session", spSession.getSelectedItemPosition());
            Toast.makeText(mainActivity, R.string.success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mainActivity, R.string.fail, Toast.LENGTH_SHORT).show();
        }
    }
}
