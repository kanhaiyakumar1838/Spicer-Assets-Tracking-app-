package com.xlzn.hcpda.uhf.interfaces;

import android.content.Context;

import com.xlzn.hcpda.uhf.entity.SelectEntity;
import com.xlzn.hcpda.uhf.entity.UHFReaderResult;
import com.xlzn.hcpda.uhf.entity.UHFTagEntity;
import com.xlzn.hcpda.uhf.entity.UHFVersionInfo;
import com.xlzn.hcpda.uhf.enums.ConnectState;
import com.xlzn.hcpda.uhf.enums.InventoryModeForPower;
import com.xlzn.hcpda.uhf.enums.LockActionEnum;
import com.xlzn.hcpda.uhf.enums.LockMembankEnum;
import com.xlzn.hcpda.uhf.enums.UHFSession;

public interface IUHFReader {
    //开始连接
    public UHFReaderResult<Boolean> setInventoryTid(boolean flag);
    //开始连接
    public UHFReaderResult<Boolean> connect(Context context);
    //断开连接
    public UHFReaderResult<Boolean> disConnect();
    //开始盘点
    public UHFReaderResult<Boolean> startInventory(SelectEntity selectEntity);
    //停止盘点
    public UHFReaderResult<Boolean> stopInventory();
    //单次盘点
    public UHFReaderResult<UHFTagEntity> singleTagInventory(SelectEntity selectEntity);
    //设置连续盘点指定标签
    public UHFReaderResult<Boolean> setInventorySelectEntity(SelectEntity selectEntity);
    //获取连接状态
    public ConnectState getConnectState();
    //获取模块版本
    public UHFReaderResult<UHFVersionInfo> getVersions();
    //设置session
    public UHFReaderResult<Boolean> setSession(UHFSession vlaue);
    //获取session
    public UHFReaderResult<UHFSession> getSession();
    //设置动态Target 00:A->B   01:B->A
    public UHFReaderResult<Boolean> setDynamicTarget(int value);
    //设置静态Target  00:A   01:B
    public UHFReaderResult<Boolean> setStaticTarget(int value);
    //获取Target   [0]:0:表示动态 ,1:表示静态    [1]:设置的value
    public UHFReaderResult<int[]> getTarget();
    //设置盘点模式，针对于功耗，设置省电模式 和 快速模式
    public UHFReaderResult<Boolean> setInventoryModeForPower(InventoryModeForPower modeForPower);
    //设置盘点标签回调
    public void setOnInventoryDataListener(OnInventoryDataListener onInventoryDataListener);
    //设置功率
    public UHFReaderResult<Boolean> setPower(int power);
    //获取功率
    public UHFReaderResult<Integer> getPower();

    //设置功率
    public UHFReaderResult<Boolean> setModuleType(String moduleType);
    //获取功率
    public UHFReaderResult<String> getModuleType();



    //设置频率区域
    /*
    北美（902-928）	0x01
    中国1（920-925）	0x06
    欧频（865-867）	0x08
    中国2（840-845）	0x0a
    全频段（840-960）	0xff
*/
    public UHFReaderResult<Boolean> setFrequencyRegion(int region);
    //获取频率区域
    public UHFReaderResult<Integer> getFrequencyRegion();
    //获取模块温度
    public UHFReaderResult<Integer> getTemperature();
    //获取标签数据 membank 00：保留期  1：epc  2:tid   3：UER
    public UHFReaderResult<String> read(String password,int membank,int address,int wordCount,SelectEntity selectEntity);
    //获取写标签数据
    public UHFReaderResult<Boolean> write(String password,int membank,int address,int wordCount,String data,SelectEntity selectEntity);
    //销毁
    public UHFReaderResult<Boolean> kill(String password,SelectEntity selectEntity);
    //lock
    public UHFReaderResult<Boolean> lock(String password, LockMembankEnum hexMask, LockActionEnum hexAction, SelectEntity selectEntity);

    public UHFReaderResult<Boolean> setBaudRate(int baudRate);

    public UHFReaderResult<Boolean> setFrequencyPoint(int baudRate);
    public UHFReaderResult<Boolean> setRFLink(int mode);
}
