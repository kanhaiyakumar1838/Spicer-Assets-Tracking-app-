package com.xlzn.hcpda.uhf;

import static com.xlzn.hcpda.uhf.entity.UHFReaderResult.ResultCode.CODE_FAILURE;
import static com.xlzn.hcpda.uhf.entity.UHFReaderResult.ResultCode.CODE_READER_NOT_CONNECTED;
import static com.xlzn.hcpda.uhf.entity.UHFReaderResult.ResultCode.CODE_SUCCESS;

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
import com.xlzn.hcpda.uhf.interfaces.IUHFReader;
import com.xlzn.hcpda.uhf.interfaces.OnInventoryDataListener;
import com.xlzn.hcpda.uhf.module.UHFReaderSLR;

public class UHFReader {

    private static UHFReader uhfReader=new UHFReader();
    private IUHFReader reader=null;
    private OnInventoryDataListener onInventoryDataListener;
    private UHFReader(){

    }

    public static UHFReader getInstance(){
            return uhfReader;
    }

    /**
     *
     * Connect the UHF reader
     * @param context
     * @return UHFReaderResult
     */
    public synchronized UHFReaderResult<Boolean> connect(Context context) {
        if(getConnectState() == ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_SUCCESS,"模块已经连接成功,请勿重复连接!",true);
        }

        UHFReaderResult result= UHFReaderSLR.getInstance().connect(context);
        if(result.getResultCode()== CODE_SUCCESS){
            reader= UHFReaderSLR.getInstance();
            return result;
        }

        return new UHFReaderResult(CODE_FAILURE);
    }

    /**
     *
     * Disconnect the UHF reader
     * @return UHFReaderResult
     */
    public synchronized UHFReaderResult<Boolean> disConnect() {

        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return  reader.disConnect();
    }

    /***
     *
     *  Inventory of specified labels
     * @param selectEntity
     * @return
     */
    public synchronized UHFReaderResult<Boolean> startInventory(SelectEntity selectEntity) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }

        if(selectEntity!=null){
            if(selectEntity.getLength()==0){
                return new UHFReaderResult<Boolean>(CODE_FAILURE,"选择的标签长度不能为0!",false);
            }
            if(selectEntity.getData()==null){
                return new UHFReaderResult<Boolean>(CODE_FAILURE,"选择的标签数据不能为null!",false);
            }
        }
        reader.setOnInventoryDataListener(onInventoryDataListener);
        return  reader.startInventory(selectEntity);
    }
    /***
     *
     * Start taking stock
     * @return UHFReaderResult
     */
    public synchronized UHFReaderResult<Boolean> startInventory() {
        return startInventory(null);
    }

    /**
     *
     * stop inventory
     * @return UHFReaderResult
     */
    public synchronized UHFReaderResult<Boolean> stopInventory() {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return  reader.stopInventory();
    }

    /**
     *
     * Start a single inventory
     * @param selectEntity Specifies the label. null indicates that no label is specified
     * @return UHFReaderResult
     */
    public UHFReaderResult<UHFTagEntity> singleTagInventory(SelectEntity selectEntity) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        if(selectEntity!=null){
            if(selectEntity.getLength()==0){
                return new UHFReaderResult<UHFTagEntity>(CODE_FAILURE,"选择的标签长度不能为0!",null);
            }
            if(selectEntity.getData()==null){
                return new UHFReaderResult<UHFTagEntity>(CODE_FAILURE,"选择的标签数据不能为null!",null);
            }
        }
        return  reader.singleTagInventory(selectEntity);
    }

    /**
     *
     * Start a single inventory
     * @return UHFReaderResult
     */
    public synchronized UHFReaderResult<UHFTagEntity> singleTagInventory() {
        return singleTagInventory(null);
    }


//TODO
    /**
     *
     * @param selectEntity
     * @return
     */
    public UHFReaderResult<Boolean> setInventorySelectEntity(SelectEntity selectEntity) {
        return reader.setInventorySelectEntity(selectEntity);
    }

    /**
     *
     * Setting the count TID
     * @param flag true:inventory tid    false:inventory epc
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> setInventoryTid(boolean flag){
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setInventoryTid(flag);
    }


    /**
     *
     * Gets UHF reader connection status
     * @return UHFReaderResult
     */
    public synchronized ConnectState getConnectState() {
        if(reader==null){
            return ConnectState.DISCONNECT;
        }
        return reader.getConnectState();
    }

    /**
     *
     * Gets the UHF reader version
     * @return UHFReaderResult
     */
    public synchronized UHFReaderResult<UHFVersionInfo> getVersions() {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.getVersions();
    }

    /**
     * set Session
     * @param vlaue
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> setSession(UHFSession vlaue) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setSession(vlaue);
    }

    /**
     *  get Session
     * @return UHFReaderResult
     */
    public UHFReaderResult<UHFSession> getSession() {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.getSession();
    }

    /**
     * set Target
     * @param vlaue (0,1)  00:A->B   01:B->A
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> setDynamicTarget(int vlaue) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setDynamicTarget(vlaue);
    }

    /**
     * set static Target
     * @param vlaue (0,1)  00:A   01:B
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> setStaticTarget(int vlaue) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setStaticTarget(vlaue);
    }

    /**
     * get move Target
     * @return
     */
    public UHFReaderResult<int[]> getTarget() {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.getTarget();
    }

    /**
     *
     * @param InventoryMode
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> setInventoryModeForPower(InventoryModeForPower InventoryMode) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setInventoryModeForPower(InventoryMode);
    }

    /**
     *
     * Set the inventory data callback
     * @param onInventoryDataListener
     */
    public void setOnInventoryDataListener(OnInventoryDataListener onInventoryDataListener) {
        this.onInventoryDataListener=onInventoryDataListener;
    }

    /**
     *  set region

     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> setFrequencyRegion(int region) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setFrequencyRegion(region);
    }

    /**
     * get region
     */
    public UHFReaderResult<Integer> getFrequencyRegion() {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.getFrequencyRegion();
    }

    /**
     * get module temperature
     * @return UHFReaderResult
     */
    public UHFReaderResult<Integer> getTemperature() {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,0);
        }
        return reader.getTemperature();
    }
    /*

      membank :
        0x00 = Reserved
        0x01 = EPC
        0x02= TID
        0x03 = User Memory

     */

    /**
     * read tag
     * @param password 密码 password
     * @param membank    00：保留区  1：epc  2:tid   3：UER
     * @param address  起始地址(单位：字)
     * @param wordCount 数据长度(单位：字)
     * @param selectEntity  Specifies the label. null indicates that no label is specified
     * @return UHFReaderResult
     */
    public UHFReaderResult<String> read(String password,int membank,int address,int wordCount,SelectEntity selectEntity) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult<String>(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,null);
        }
        if(selectEntity!=null){
            if(selectEntity.getLength()==0){
                return new UHFReaderResult<String>(CODE_FAILURE,"选择的标签长度不能为0!",null);
            }
            if(selectEntity.getData()==null){
                return new UHFReaderResult<String>(CODE_FAILURE,"选择的标签数据不能为null!",null);
            }
        }
        return reader.read(  password,  membank,  address,  wordCount,  selectEntity);
    }

    /**
     *
     * write tag
     * @param password password
     * @param membank   00：保留区  1：epc  2:tid   3：UER
     * @param address start address
     * @param wordCount length
     * @param data data
     * @param selectEntity Specifies the label. null indicates that no label is specified
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> write(String password, int membank, int address, int wordCount, String data, SelectEntity selectEntity) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult<Boolean>(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        if(selectEntity!=null){
            if(selectEntity.getLength()==0){
                return new UHFReaderResult(CODE_FAILURE,"选择的标签长度不能为0!",null);
            }
            if(selectEntity.getData()==null){
                return new UHFReaderResult(CODE_FAILURE,"选择的标签数据不能为null!",null);
            }
        }
        return reader.write(  password,  membank,  address,  wordCount,  data,selectEntity);
    }

    /**
     *
     * kill tag
     * @param password
     * @param selectEntity  Specifies the label. null indicates that no label is specified
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> kill(String password, SelectEntity selectEntity) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult<Boolean>(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        if(selectEntity!=null){
            if(selectEntity.getLength()==0){
                return new UHFReaderResult(CODE_FAILURE,"选择的标签长度不能为0!",null);
            }
            if(selectEntity.getData()==null){
                return new UHFReaderResult(CODE_FAILURE,"选择的标签数据不能为null!",null);
            }
        }
        return reader.kill(  password, selectEntity);
    }

    /**
     *
     * lock tag
     * @param password
     * @param membankEnum
     * @param actionEnum
     * @param selectEntity  指定标签，此参数为null表示不指定标签  Specifies the label. null indicates that no label is specified
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> lock(String password, LockMembankEnum membankEnum, LockActionEnum actionEnum, SelectEntity selectEntity) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult<Boolean>(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        if(selectEntity!=null){
            if(selectEntity.getLength()==0){
                return new UHFReaderResult(CODE_FAILURE,"选择的标签长度不能为0!",null);
            }
            if(selectEntity.getData()==null){
                return new UHFReaderResult(CODE_FAILURE,"选择的标签数据不能为null!",null);
            }
        }
        return reader.lock(  password, membankEnum,actionEnum, selectEntity);
    }

    /**
     *
     * set power
     * @param power (5-33)
     * @return UHFReaderResult
     */
    public UHFReaderResult<Boolean> setPower(int power) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setPower(power);
    }



    /**
     *
     * get power
     * @return (5-33)
     */
    public UHFReaderResult<Integer> getPower() {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.getPower();
    }


    public UHFReaderResult<String> getModuleType() {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }

        return new UHFReaderResult(0);
    }


    public UHFReaderResult<Boolean> setModuleType(String moduleType) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setModuleType(moduleType);
    }

    public UHFReaderResult<Boolean> setFrequencyPoint(int point) {
        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult<Boolean>(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setFrequencyPoint(point);
    }
    public UHFReaderResult<Boolean> setRFLink(int mode) {

        if(getConnectState() != ConnectState.CONNECTED){
            return new UHFReaderResult<Boolean>(CODE_READER_NOT_CONNECTED, UHFReaderResult.ResultMessage.READER_NOT_CONNECTED,false);
        }
        return reader.setRFLink(mode);
    }
}
