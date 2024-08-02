package com.xlzn.hcpda.uhf.module;

import android.content.Context;
import android.os.SystemClock;

import com.hc.so.HcPowerCtrl;
import com.xlzn.hcpda.DeviceConfigManage;
import com.xlzn.hcpda.uhf.analysis.BuilderAnalysisSLR;
import com.xlzn.hcpda.uhf.analysis.BuilderAnalysisSLR_E710;
import com.xlzn.hcpda.uhf.analysis.UHFProtocolAnalysisBase;
import com.xlzn.hcpda.uhf.analysis.UHFProtocolAnalysisSLR;
import com.xlzn.hcpda.uhf.entity.SelectEntity;
import com.xlzn.hcpda.uhf.entity.UHFReaderResult;
import com.xlzn.hcpda.uhf.entity.UHFTagEntity;
import com.xlzn.hcpda.uhf.entity.UHFVersionInfo;
import com.xlzn.hcpda.uhf.enums.ConnectState;
import com.xlzn.hcpda.uhf.enums.InventoryModeForPower;
import com.xlzn.hcpda.uhf.enums.LockActionEnum;
import com.xlzn.hcpda.uhf.enums.LockMembankEnum;
import com.xlzn.hcpda.uhf.enums.UHFSession;
import com.xlzn.hcpda.uhf.interfaces.IBuilderAnalysis;
import com.xlzn.hcpda.uhf.interfaces.IUHFProtocolAnalysis;
import com.xlzn.hcpda.uhf.interfaces.IUHFReader;
import com.xlzn.hcpda.uhf.interfaces.OnInventoryDataListener;
import com.xlzn.hcpda.uhf.serialport.UHFSerialPort;
import com.xlzn.hcpda.utils.DataConverter;
import com.xlzn.hcpda.utils.LoggerUtils;

public class UHFReaderSLR implements IUHFReader {
    private String TAG = "UHFReaderSLR";
    public static UHFReaderSLR uhfReaderSLR = new UHFReaderSLR();
    //模块供电
    private HcPowerCtrl hcPowerCtrl = new HcPowerCtrl();
    //具体UHF模块操作对象
    private IUHFReader iuhfReader = null;
    //协议解析
    private IUHFProtocolAnalysis uhfProtocolAnalysisSLR = new UHFProtocolAnalysisSLR();
    //构建发送的数据
    private IBuilderAnalysis builderAnalysisSLR = new BuilderAnalysisSLR();
    public boolean is5300 = false;
    public static UHFReaderSLR getInstance() {
        return uhfReaderSLR;
    }
    @Override
    public UHFReaderResult<Boolean> setInventoryTid(boolean flag) {
        return iuhfReader.setInventoryTid(flag);
    }

    @Override
    public UHFReaderResult<Boolean> connect(Context context) {
        LoggerUtils.d(TAG, "connect!");
        if (getConnectState() == ConnectState.CONNECTED) {
            //模块已经上电
            LoggerUtils.d(TAG, "模块已经连接成功!");
            return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_SUCCESS, "模块已经连接成功,不可重复连接!", true);
        }

        DeviceConfigManage.UHFConfig uhfConfig = DeviceConfigManage.getInstance().getUhfConfig();
        //*****模块上电**********

        hcPowerCtrl.uhfPower(1);
        hcPowerCtrl.uhfCtrl(1);
        hcPowerCtrl.identityPower(1);
        hcPowerCtrl.identityCtrl(1);
        LoggerUtils.d(TAG,"供电-------7");
        //****************
        UHFReaderResult<UHFVersionInfo> verInfo = null;
        int baudrate = 115200;
        for (int k = 0; k < 2; k++) {
            boolean result = UHFSerialPort.getInstance().open(uhfConfig.getUhfUart(), uhfProtocolAnalysisSLR, baudrate);
            LoggerUtils.d(TAG, "打开串口=" + result + "  Uart=" + uhfConfig.getUhfUart() + "  baudrate=" + baudrate);
            if (!result) {
                //打开串口失败
                LoggerUtils.d(TAG, "打开串口失败!");
                return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_OPEN_SERIAL_PORT_FAILURE, UHFReaderResult.ResultMessage.OPEN_SERIAL_PORT_FAILURE, false);
            }
            //*******获取版本号***************
            //发送激活模块的命令
            SystemClock.sleep(80);
            sendData(DataConverter.hexToBytes("FF00041D0B"));
            SystemClock.sleep(400);
            //获取版本号
            LoggerUtils.d(TAG, "获取版本号!");
            UHFProtocolAnalysisBase.DataFrameInfo dataFrameInfo = sendAndReceiveData(builderAnalysisSLR.makeGetVersionSendData());
            verInfo = builderAnalysisSLR.analysisVersionData(dataFrameInfo);
            LoggerUtils.d(TAG, "唤醒模块----"+ verInfo.getResultCode());
            SystemClock.sleep(200);
            if (verInfo.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
                if (verInfo.getData().getHardwareVersion().startsWith("31") || verInfo.getData().getHardwareVersion().startsWith("33")) {
                    builderAnalysisSLR = new BuilderAnalysisSLR_E710();
//                    builderAnalysisSLR = new BuilderAnalysisSLR();
                    LoggerUtils.d(TAG, "是E710啊----");
                    DeviceConfigManage.module_type = "E710";
                }
                hcPowerCtrl.identityPower(0);
                hcPowerCtrl.identityCtrl(0);
                break;
            } else {
                LoggerUtils.d(TAG, "获取版本号失败，打开另外一个串口");
//                uhfConfig.setUhfUart("/dev/ttysWK1");
                boolean result2 = UHFSerialPort.getInstance().open(uhfConfig.getUhfUart(), uhfProtocolAnalysisSLR, baudrate);
                LoggerUtils.d(TAG, "打开串口=" + result2 + "  Uart=" + uhfConfig.getUhfUart() + "  baudrate=" + baudrate);
                if (!result2) {
                    //打开串口失败
                    LoggerUtils.d(TAG, "打开串口失败!");
                    return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_OPEN_SERIAL_PORT_FAILURE, UHFReaderResult.ResultMessage.OPEN_SERIAL_PORT_FAILURE, false);
                }
                //*******获取版本号***************
                //发送激活模块的命令
                SystemClock.sleep(80);
                sendData(DataConverter.hexToBytes("FF00041D0B"));
                SystemClock.sleep(300);
                //获取版本号
                LoggerUtils.d(TAG, "获取版本号!");
                UHFProtocolAnalysisBase.DataFrameInfo dataFrameInfo2 = sendAndReceiveData(builderAnalysisSLR.makeGetVersionSendData());
                verInfo = builderAnalysisSLR.analysisVersionData(dataFrameInfo2);
                SystemClock.sleep(200);
                if (verInfo.getResultCode() == UHFReaderResult.ResultCode.CODE_SUCCESS) {
                    if (verInfo.getData().getHardwareVersion().startsWith("31") || verInfo.getData().getHardwareVersion().startsWith("33")) {
                        builderAnalysisSLR = new BuilderAnalysisSLR_E710();
//                        builderAnalysisSLR = new BuilderAnalysisSLR();
                        LoggerUtils.d(TAG, "是E710啊----");
                        DeviceConfigManage.module_type = "E710";
                    }
                    break;
                } else {
                    hcPowerCtrl.uhfPower(0);
                    hcPowerCtrl.uhfCtrl(0);
                }
            }
            UHFSerialPort.getInstance().close();
        }

        if (verInfo.getResultCode() != UHFReaderResult.ResultCode.CODE_SUCCESS) {
            disConnect();
            return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_FAILURE, "获取模块信息失败!");
        } else {
            UHFVersionInfo uhfVersionInfo = verInfo.getData();
            String hver = uhfVersionInfo.getHardwareVersion();
            String firmwareVersion = uhfVersionInfo.getFirmwareVersion();
                        LoggerUtils.d(TAG, "固件版本:" + firmwareVersion +"  硬件版本="+hver);
            if ( hver.startsWith("A1")) {
                //SLR1200 固件版本:20200703  硬件版本=A1000201
               LoggerUtils.d(TAG,"R2000 协议构建");
                DeviceConfigManage.module_type = "R2000";
                is5300 = false;
                iuhfReader = new UHFReaderSLR1200(uhfProtocolAnalysisSLR, builderAnalysisSLR);
            } else if (hver.startsWith("31")||hver.startsWith("33")) {
                LoggerUtils.d(TAG,"E710 协议构建");
                //其他模块
                //e710 硬件版本:31000000\固件版本:20220531
                // iuhfReader=new ...
                if (hver.startsWith("33")) {
                    DeviceConfigManage.module_type = "E310";
                } else {
                    DeviceConfigManage.module_type = "E710";
                }
                is5300 = false;
                iuhfReader = new UHFReaderSLR1200(uhfProtocolAnalysisSLR, builderAnalysisSLR);
            } else if (hver.startsWith("A6")||hver.startsWith("A3")) {
                LoggerUtils.d(TAG,"5300 协议构建");
                if (hver.startsWith("A6")) {
                    DeviceConfigManage.module_type = "5100";
                } else {
                    DeviceConfigManage.module_type = "5300";
                }
                // 5300  固件版本:20160401  硬件版本=A6000001
                is5300 = true;
                iuhfReader = new UHFReaderSLR1200(uhfProtocolAnalysisSLR, builderAnalysisSLR);

            }
        }
        if (iuhfReader==null) {
            return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_FAILURE);
        }
        ((UHFReaderBase) iuhfReader).setConnectState(ConnectState.CONNECTED);
        //成功
        return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_SUCCESS);
    }

    @Override
    public UHFReaderResult disConnect() {
        //停止盘点
        stopInventory();
        //模块下电
        hcPowerCtrl.uhfPower(0);
        hcPowerCtrl.uhfCtrl(0);
//        hcPowerCtrl.identityPower(0);
        LoggerUtils.d("CHLOG","----------------------模块下电");
        UHFSerialPort.getInstance().close();
        if (iuhfReader != null) {
            ((UHFReaderBase) iuhfReader).setConnectState(ConnectState.DISCONNECT);
        }
        return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_SUCCESS);
    }

    @Override
    public UHFReaderResult<Boolean> startInventory(SelectEntity selectEntity) {
        return iuhfReader.startInventory(selectEntity);
    }

    @Override
    public UHFReaderResult<Boolean> stopInventory() {
        if (iuhfReader == null) {
            return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_FAILURE, "");
        }
        return iuhfReader.stopInventory();
    }

    @Override
    public UHFReaderResult<UHFTagEntity> singleTagInventory(SelectEntity selectEntity) {
        return iuhfReader.singleTagInventory(selectEntity);
    }

    @Override
    public UHFReaderResult<Boolean> setInventorySelectEntity(SelectEntity selectEntity) {
        return iuhfReader.setInventorySelectEntity(selectEntity);
    }

    @Override
    public ConnectState getConnectState() {
        if (iuhfReader == null) {
            return ConnectState.DISCONNECT;
        }
        return iuhfReader.getConnectState();
    }

    @Override
    public UHFReaderResult<UHFVersionInfo> getVersions() {
        return iuhfReader.getVersions();
    }

    @Override
    public UHFReaderResult<Boolean> setSession(UHFSession vlaue) {
        return iuhfReader.setSession(vlaue);
    }

    @Override
    public UHFReaderResult<UHFSession> getSession() {
        return iuhfReader.getSession();
    }

    @Override
    public UHFReaderResult<Boolean> setDynamicTarget(int vlaue) {
        return iuhfReader.setDynamicTarget(vlaue);
    }

    @Override
    public UHFReaderResult<Boolean> setStaticTarget(int vlaue) {
        return iuhfReader.setStaticTarget(vlaue);
    }

    @Override
    public UHFReaderResult<int[]> getTarget() {
        return iuhfReader.getTarget();
    }

    @Override
    public UHFReaderResult<Boolean> setInventoryModeForPower(InventoryModeForPower modeForPower) {
        return iuhfReader.setInventoryModeForPower(modeForPower);
    }

    @Override
    public void setOnInventoryDataListener(OnInventoryDataListener onInventoryDataListener) {
        iuhfReader.setOnInventoryDataListener(onInventoryDataListener);
    }

    @Override
    public UHFReaderResult<Boolean> setPower(int power) {
        return iuhfReader.setPower(power);
    }

    @Override
    public UHFReaderResult<Integer> getPower() {
        return iuhfReader.getPower();
    }

    @Override
    public UHFReaderResult<Boolean> setModuleType(String moduleType) {
        return null;
    }

    @Override
    public UHFReaderResult<String> getModuleType() {
        return null;
    }


    @Override
    public UHFReaderResult<Boolean> setFrequencyRegion(int region) {
        return iuhfReader.setFrequencyRegion(region);
    }

    @Override
    public UHFReaderResult<Integer> getFrequencyRegion() {
        return iuhfReader.getFrequencyRegion();
    }

    @Override
    public UHFReaderResult<Integer> getTemperature() {
        return iuhfReader.getTemperature();
    }

    @Override
    public UHFReaderResult<String> read(String password, int membank, int address, int wordCount, SelectEntity selectEntity) {
        return iuhfReader.read(password, membank, address, wordCount, selectEntity);
    }

    @Override
    public UHFReaderResult<Boolean> write(String password, int membank, int address, int wordCount, String data, SelectEntity selectEntity) {
        return iuhfReader.write(password, membank, address, wordCount, data, selectEntity);
    }

    @Override
    public UHFReaderResult<Boolean> kill(String password, SelectEntity selectEntity) {
        return iuhfReader.kill(password, selectEntity);
    }

    @Override
    public UHFReaderResult<Boolean> lock(String password, LockMembankEnum hexMask, LockActionEnum hexAction, SelectEntity selectEntity) {
        return iuhfReader.lock(password, hexMask, hexAction, selectEntity);
    }

    @Override
    public UHFReaderResult<Boolean> setBaudRate(int baudRate) {
        return iuhfReader.setBaudRate(baudRate);
    }

    @Override
    public UHFReaderResult<Boolean> setFrequencyPoint(int baudRate) {
        return iuhfReader.setFrequencyPoint(baudRate);
    }

    @Override
    public UHFReaderResult<Boolean> setRFLink(int mode) {
        return iuhfReader.setRFLink(mode);
    }

    //发送数据到模块
    private boolean sendData(byte[] data) {
        return UHFSerialPort.getInstance().send(data);
    }

    //发送接收数据
    private UHFProtocolAnalysisBase.DataFrameInfo sendAndReceiveData(byte[] sData) {
        if (!sendData(sData)) {
            return null;
        }
        int timeOut = 1000;
        int cmd = sData[2] & 0xFF;
        return uhfProtocolAnalysisSLR.getOtherInfo(cmd, timeOut);
    }
}


//package com.xlzn.hcpda.uhf.module;
//
//import android.content.Context;
//import android.os.SystemClock;
//
//import com.hc.so.HcPowerCtrl;
//import com.xlzn.hcpda.DeviceConfigManage;
//import com.xlzn.hcpda.uhf.analysis.BuilderAnalysisSLR;
//import com.xlzn.hcpda.uhf.analysis.UHFProtocolAnalysisBase;
//import com.xlzn.hcpda.uhf.analysis.UHFProtocolAnalysisSLR;
//import com.xlzn.hcpda.uhf.entity.SelectEntity;
//import com.xlzn.hcpda.uhf.entity.UHFReaderResult;
//import com.xlzn.hcpda.uhf.entity.UHFTagEntity;
//import com.xlzn.hcpda.uhf.entity.UHFVersionInfo;
//import com.xlzn.hcpda.uhf.enums.ConnectState;
//import com.xlzn.hcpda.uhf.enums.InventoryModeForPower;
//import com.xlzn.hcpda.uhf.enums.LockActionEnum;
//import com.xlzn.hcpda.uhf.enums.LockMembankEnum;
//import com.xlzn.hcpda.uhf.enums.UHFSession;
//import com.xlzn.hcpda.uhf.interfaces.IBuilderAnalysis;
//import com.xlzn.hcpda.uhf.interfaces.IUHFProtocolAnalysis;
//import com.xlzn.hcpda.uhf.interfaces.IUHFReader;
//import com.xlzn.hcpda.uhf.interfaces.OnInventoryDataListener;
//import com.xlzn.hcpda.uhf.serialport.UHFSerialPort;
//import com.xlzn.hcpda.utils.DataConverter;
//import com.xlzn.hcpda.utils.LoggerUtils;
//
//public class UHFReaderSLR implements IUHFReader {
//    private String TAG="UHFReaderSLR";
//    public static UHFReaderSLR uhfReaderSLR=new UHFReaderSLR();
//    //模块供电
//    private HcPowerCtrl hcPowerCtrl=new HcPowerCtrl();
//
//    //具体UHF模块操作对象
//    private IUHFReader iuhfReader=null;
//    //协议解析
//    private IUHFProtocolAnalysis uhfProtocolAnalysisSLR=new UHFProtocolAnalysisSLR();
//    //构建发送的数据
//    private IBuilderAnalysis builderAnalysisSLR=new BuilderAnalysisSLR();
//    public boolean is5300 = false;
//
//    public static UHFReaderSLR getInstance() {
//        return uhfReaderSLR;
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setInventoryTid(boolean flag) {
//        return iuhfReader.setInventoryTid(flag);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> connect(Context context) {
//        LoggerUtils.d(TAG,"connect!");
//        if(getConnectState() ==ConnectState.CONNECTED){
//            //模块已经上电
//            LoggerUtils.d(TAG,"模块已经连接成功!");
//            return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_SUCCESS,"模块已经连接成功,不可重复连接!",true);
//        }
//        DeviceConfigManage.UHFConfig uhfConfig= DeviceConfigManage.getInstance().getUhfConfig();
//        //*****模块上电**********
//        hcPowerCtrl = new HcPowerCtrl();
//        hcPowerCtrl.uhfPower(1);
//        hcPowerCtrl.uhfCtrl(1);
//        hcPowerCtrl.identityPower(1);
////        hcPowerCtrl.identityCtrl(1);
//        LoggerUtils.d(TAG,"测试--------------------------------11 ");
//
//        UHFReaderResult<UHFVersionInfo> verInfo=null;
//        //****************
//        int baudrate=115200;
////        for(int k=0;k<1;k++) {
////            if(k==1){
////                  baudrate=921600;
////            }
//            boolean result = UHFSerialPort.getInstance().open(uhfConfig.getUhfUart(), uhfProtocolAnalysisSLR,baudrate);
//            LoggerUtils.d(TAG, "打开串口="+ result + "  Uart="+uhfConfig.getUhfUart()+"  baudrate="+baudrate);
//            if (!result) {
//                //打开串口失败
//                LoggerUtils.d(TAG, "打开串口失败!");
//                return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_OPEN_SERIAL_PORT_FAILURE, UHFReaderResult.ResultMessage.OPEN_SERIAL_PORT_FAILURE, false);
//            }
//            //*******获取版本号***************
//            //发送激活模块的命令
//            SystemClock.sleep(2000);
//            sendData(DataConverter.hexToBytes("FF00041D0B"));
//            SystemClock.sleep(500);
//            //获取版本号
//            LoggerUtils.d(TAG, "获取版本号!");
//        UHFProtocolAnalysisBase.DataFrameInfo dataFrameInfo = sendAndReceiveData(builderAnalysisSLR.makeGetVersionSendData());
////        SystemClock.sleep(600);
//            verInfo = builderAnalysisSLR.analysisVersionData(dataFrameInfo);
//            if(verInfo.getResultCode()==UHFReaderResult.ResultCode.CODE_SUCCESS) {
//                LoggerUtils.d(TAG, "获取版本号成功");
//            }else {
//                LoggerUtils.d(TAG, "获取版本号失败");
//            }
//
////            UHFSerialPort.getInstance().close();
////        }
//        if(verInfo.getResultCode()!=UHFReaderResult.ResultCode.CODE_SUCCESS){
//            LoggerUtils.d(TAG, "connect: 获取模块信息失败1"  );
////            disConnect();
//            return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_FAILURE,"获取模块信息失败!");
//        }else {
//            UHFVersionInfo uhfVersionInfo = verInfo.getData();
//            String hver = uhfVersionInfo.getHardwareVersion();
//            String firmwareVersion = uhfVersionInfo.getFirmwareVersion();
//            LoggerUtils.d(TAG, "固件版本:" + firmwareVersion +"  硬件版本="+hver);
//            if ( hver.startsWith("A1")) {
//                //SLR1200 固件版本:20200703  硬件版本=A1000201
//               LoggerUtils.d(TAG,"R2000 协议构建");
//                is5300 = false;
//                iuhfReader = new UHFReaderSLR1200(uhfProtocolAnalysisSLR, builderAnalysisSLR);
//            } else if (hver.contains("31")) {
//                LoggerUtils.d(TAG,"E710 协议构建");
//                //其他模块
//                //e710 硬件版本:31000000\固件版本:20220531
//                // iuhfReader=new ...
//                is5300 = false;
//                iuhfReader = new UHFReaderSLR1200(uhfProtocolAnalysisSLR, builderAnalysisSLR);
//            } else if ( hver.contains("A6")) {
//                LoggerUtils.d(TAG,"5300 协议构建");
//                // 5300  固件版本:20160401  硬件版本=A6000001
//                is5300 = true;
//                iuhfReader = new UHFReaderSLR1200(uhfProtocolAnalysisSLR, builderAnalysisSLR);
//
//            }
//        }
//        LoggerUtils.d(TAG,"模块连接成功!");
//        ((UHFReaderBase)iuhfReader).setConnectState(ConnectState.CONNECTED);
//        //成功
//        return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_SUCCESS);
//    }
//
//    @Override
//    public UHFReaderResult disConnect() {
//        //停止盘点
//        stopInventory();
//        //模块下电
//        hcPowerCtrl.uhfPower(0);
//        hcPowerCtrl.uhfCtrl(0);
//        hcPowerCtrl.identityPower(0);
//        LoggerUtils.d(TAG,"执行disConnect");
//        UHFSerialPort.getInstance().close();
//        if(iuhfReader!=null) {
//            ((UHFReaderBase) iuhfReader).setConnectState(ConnectState.DISCONNECT);
//        }
//        return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_SUCCESS);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> startInventory(SelectEntity selectEntity) {
//        return iuhfReader.startInventory(selectEntity);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> stopInventory() {
//        if(iuhfReader==null){
//            return  new UHFReaderResult(UHFReaderResult.ResultCode.CODE_FAILURE,"");
//        }
//        return iuhfReader.stopInventory();
//    }
//
//    @Override
//    public UHFReaderResult<UHFTagEntity> singleTagInventory(SelectEntity selectEntity) {
//        return iuhfReader.singleTagInventory(selectEntity);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setInventorySelectEntity(SelectEntity selectEntity) {
//        return iuhfReader.setInventorySelectEntity(selectEntity);
//    }
//
//    @Override
//    public ConnectState getConnectState() {
//        if(iuhfReader==null){
//            return ConnectState.DISCONNECT;
//        }
//        return iuhfReader.getConnectState();
//    }
//
//    @Override
//    public UHFReaderResult<UHFVersionInfo> getVersions() {
//        return iuhfReader.getVersions();
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setSession(UHFSession vlaue) {
//        return iuhfReader.setSession(vlaue);
//    }
//
//    @Override
//    public UHFReaderResult<UHFSession> getSession() {
//        return iuhfReader.getSession();
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setDynamicTarget(int vlaue) {
//        return iuhfReader.setDynamicTarget(vlaue);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setStaticTarget(int vlaue) {
//        return iuhfReader.setStaticTarget(vlaue);
//    }
//
//    @Override
//    public UHFReaderResult<int[]> getTarget() {
//        return iuhfReader.getTarget();
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setInventoryModeForPower(InventoryModeForPower modeForPower) {
//        return iuhfReader.setInventoryModeForPower(modeForPower);
//    }
//
//    @Override
//    public void setOnInventoryDataListener(OnInventoryDataListener onInventoryDataListener) {
//        iuhfReader.setOnInventoryDataListener(onInventoryDataListener);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setPower(int power) {
//        return iuhfReader.setPower(power);
//    }
//
//    @Override
//    public UHFReaderResult<Integer> getPower() {
//        return iuhfReader.getPower();
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setModuleType(String moduleType) {
//        return iuhfReader.setModuleType(moduleType);
//
//    }
//
//    @Override
//    public UHFReaderResult<String> getModuleType() {
//        return iuhfReader.getModuleType();
//    }
//
//
//    @Override
//    public UHFReaderResult<Boolean> setFrequencyRegion(int region) {
//        return iuhfReader.setFrequencyRegion(region);
//    }
//
//    @Override
//    public UHFReaderResult<Integer> getFrequencyRegion() {
//        return iuhfReader.getFrequencyRegion();
//    }
//
//    @Override
//    public UHFReaderResult<Integer> getTemperature() {
//        return iuhfReader.getTemperature();
//    }
//
//    @Override
//    public UHFReaderResult<String> read(String password,int membank,int address,int wordCount,SelectEntity selectEntity) {
//        return iuhfReader.read(  password,  membank,  address,  wordCount,  selectEntity);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> write(String password, int membank, int address, int wordCount, String data, SelectEntity selectEntity) {
//        return iuhfReader.write(  password,  membank,  address,  wordCount,  data,selectEntity);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> kill(String password, SelectEntity selectEntity) {
//        return iuhfReader.kill(  password ,selectEntity);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> lock(String password, LockMembankEnum hexMask, LockActionEnum hexAction, SelectEntity selectEntity) {
//        return iuhfReader.lock(password,hexMask,hexAction,selectEntity);
//    }
//
//    @Override
//    public UHFReaderResult<Boolean> setBaudRate(int baudRate) {
//        return iuhfReader.setBaudRate(baudRate);
//    }
//
//    //发送数据到模块
//    private boolean sendData(byte[] data){
//        return UHFSerialPort.getInstance().send(data);
//    }
//    //发送接收数据
//    private UHFProtocolAnalysisBase.DataFrameInfo sendAndReceiveData(byte[] sData){
//        if(!sendData(sData)){
//            return null;
//        }
//        int timeOut=1000;
//        int cmd= sData[2]&0xFF;
//        return uhfProtocolAnalysisSLR.getOtherInfo(cmd,timeOut);
//    }
//}
