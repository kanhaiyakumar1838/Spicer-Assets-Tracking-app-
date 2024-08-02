package com.xlzn.hcpda.uhf.analysis;

import android.os.SystemClock;
import android.util.Log;

import com.xlzn.hcpda.ModuleAPI;
import com.xlzn.hcpda.uhf.interfaces.IUHFCheckCodeErrorCallback;
import com.xlzn.hcpda.uhf.interfaces.IUHFProtocolAnalysis;
import com.xlzn.hcpda.utils.DataConverter;
import com.xlzn.hcpda.utils.LoggerUtils;

import java.util.Arrays;
import java.util.Iterator;

/****
 * 芯联模块
 *
 * 1.主机到模块的通信格式：
 * Header   +   Data Length  +  Command  +  Data  +  CRC-16
 * Header: 一字节 固定0XFF
 * DataLength: 一字节，Data数据块的字节数
 * Command：一字节，命令码
 * Data：数据块，高字节在前面。
 * CRC-16: 二字节循环冗余码，高字节在前，从DataLength到Data结束的所有数据参与计算所得。
 * 备注：整个通信数据串的字节数不得大于255个字节。
 *
 * 2.模块到主机的通信格式：
 *    Header   +   DataLength  +  Command +  Status   +  Data  +  CRC-16
 *    Header: 一字节 固定0XFF
 * DataLength: 一字节，Data数据块的字节数
 * Command：一字节，命令码，同上一条 主机发来的命令码
 * Status: 二字节，状态位，为0时表示操作成功，非0值为操作失败具体请看后面返回状态码解释，如非0且非后面解释的错误状态码则仅表示操作失败。
 * Data：数据块，高字节在前面。
 * CRC-16: 二字节循环冗余码，高字节在前，从DataLength到Data结束的所有数据参与计算所得。
 * 备注：整个通信数据串的字节数不得大于255个字节。
 *
 *
 */
public class UHFProtocolAnalysisSLR extends UHFProtocolAnalysisBase implements IUHFProtocolAnalysis {
    private String TAG = "UHFProtocolAnalysisSLR";
    private byte[] rawPack = null;
    private final int HEADDATA = 0XFF;
    private Object lock=new Object();
    private IUHFCheckCodeErrorCallback iuhfCheckCodeErrorCallback;
    @Override
    public void analysis(byte[] data) {
        if (rawPack == null) {
            rawPack = data;
        } else {
            LoggerUtils.d(TAG, "拼接数据!");
            //拼接数据
            int len = rawPack.length + data.length;
            byte[] newData = new byte[len];
            System.arraycopy(rawPack, 0, newData, 0, rawPack.length);
            System.arraycopy(data, 0, newData, rawPack.length, data.length);
            rawPack = newData;
        }
        LoggerUtils.d(TAG, "原始数据="+DataConverter.bytesToHex(rawPack));
        //最后解析成功的数据索引
        int lastSuccessIndex = 0;
        //解析数据
        int index = -1;
        //0   1  2  3  4  5  6   index
        //FF 00 72 01 01 DB 14   len=7
        while (rawPack.length > index) {
            index++;
            LoggerUtils.d(TAG, "analysis index="+index);
            if (rawPack.length - index < 7) {
                //一个完整的数据帧至少有7个字节，Header(1byte)+DataLength(1byte)+Command(1byte)+Status(2byte)+ Data(dataLen)+ CRC-16(2byte)
                if (index > 0) {
                    //只截取没有被解析的数据
                    rawPack = Arrays.copyOfRange(rawPack, lastSuccessIndex, rawPack.length);
                    LoggerUtils.d(TAG, "数据不完整没有被解析的数据:" + DataConverter.bytesToHex(rawPack));
                }
                LoggerUtils.d(TAG, "数据不完整没有被解析的数据: index=0");
                return;
            }

            if ((rawPack[index] & 0xFF) == HEADDATA) {
                LoggerUtils.d(TAG, "校验数据帧");
                //需要校验的数据 DataLength(1byte)  +  Command(1byte) +  Status(2byte)   +  Data(dataLen)
                //此时的下标k在数据头的位置,start表示需要校验的数据的起始地址
                int start = index + 1;
                int dataLen = rawPack[start] & 0xFF;
                if(rawPack.length-(index+1+1+2+dataLen+2)<0){
                   // 0   1  2  3  4  5  6  7  8  9
                   // dd cc ff 01 29 00 00 11 xx xx
                    rawPack = Arrays.copyOfRange(rawPack, lastSuccessIndex, rawPack.length);
                    if(LoggerUtils.isDebug()) LoggerUtils.d(TAG, "数据不完整没有被解析的数据:" + DataConverter.bytesToHex(rawPack));
                    return;
                }

                //需要校验的结束地址,要校验带数据从数组下标 stat(包含) 到  end(不包含)
                int end = start + (1 + 1 + 2 + dataLen);
                //取出需要校验的数据
                byte[] validateData = Arrays.copyOfRange(rawPack, start, end);
                //取出crc校验码，crc校验码在需要校验的数据后面
                byte[] crc = Arrays.copyOfRange(rawPack, end, end + 2);
                byte[] crcTemp=new byte[2];
                ModuleAPI.getInstance().CalcCRC(validateData,validateData.length,crcTemp);
                Log.d(TAG, "数据校验索引! start="+start +" end="+end);
                //LoggerUtils.d(TAG, "数据帧crc=" + Integer.toHexString(crc[0]&0xff) + Integer.toHexString(crc[1]&0xff) );
                //LoggerUtils.d(TAG, "解析后crc=" + Integer.toHexString(crcTemp[0]&0xff) + Integer.toHexString(crcTemp[1]&0xff) );
                if (crc[0] == crcTemp[0] && crc[1] == crcTemp[1]) {
                    Log.d(TAG, "数据校验完成! ");
                    //状态数据是在命令字后面，两个字节，高字节在前
                    int cmdIndex = index + 2;
                    int statusIndex = index + 3;
                    int statusEnd = statusIndex + 2;
                    byte[] status = Arrays.copyOfRange(rawPack, statusIndex, statusEnd);
                    DataFrameInfo dataFrameInfo = new DataFrameInfo();
                    dataFrameInfo.command = rawPack[cmdIndex] & 0xFF;
                    dataFrameInfo.time = SystemClock.elapsedRealtime();
                    dataFrameInfo.status = ((status[0] & 0xFF) << 8) | (status[1] & 0xFF);
                    if (dataLen > 0) {
                        int dataIndex = statusEnd;
                        int dataEnd = statusEnd + dataLen;
                        dataFrameInfo.data = Arrays.copyOfRange(rawPack, dataIndex, dataEnd);
                      //  if(LoggerUtils.isDebug())LoggerUtils.d(TAG, "解析后的纯数据 =" + DataConverter.bytesToHex(dataFrameInfo.data));
                    }
                    addData(dataFrameInfo);
                    // /*整个数据帧
                    byte[] allData = Arrays.copyOfRange(rawPack, index, statusEnd + dataLen + 2);
                    if(LoggerUtils.isDebug())LoggerUtils.d(TAG, "解析后的整个数据帧 =" + DataConverter.bytesToHex(allData));
                    // */
                    //当前数据帧解析完成，数组下标跳转到下一个数据帧
                    index = statusEnd + dataLen + 2 -1;
                    lastSuccessIndex = index;
                } else {
                    lastSuccessIndex=index;
                    LoggerUtils.d(TAG, "数据校验出错，重新寻找数据头!");
                    if(iuhfCheckCodeErrorCallback!=null){
                        iuhfCheckCodeErrorCallback.checkCodeError(0,rawPack[index+2] & 0xFF,null);
                    }
                }
            }else{
                lastSuccessIndex=index;
            }
            if (rawPack.length - 1 == index) {
                if (lastSuccessIndex == index) {
                    //所有数据帧解析完成
                    rawPack = null;
                } else {
                    rawPack = Arrays.copyOfRange(rawPack, lastSuccessIndex, rawPack.length);
                    if(LoggerUtils.isDebug()) LoggerUtils.d(TAG, "没有被解析的数据:" + DataConverter.bytesToHex(rawPack));
                }
                LoggerUtils.d(TAG, "数据解析完成!");
                return;
            }

        }
    }

    @Override
    public void setCheckCodeErrorCallback(IUHFCheckCodeErrorCallback iuhfCheckCodeErrorCallback) {
        this.iuhfCheckCodeErrorCallback=iuhfCheckCodeErrorCallback;
    }

    //获取非连续盘点的标签数据
    public DataFrameInfo getOtherInfo(int cmd,int timeOut){
        long startTime=SystemClock.uptimeMillis();
        while (SystemClock.uptimeMillis()-startTime<timeOut){
            if(listCmd!=null && listCmd.size()>0){
                synchronized (lock) {
                    for (int k = 0; k < listCmd.size(); k++) {
                        DataFrameInfo dataFrameInfo = listCmd.get(k);
                        if (dataFrameInfo.command == cmd) {
                            listCmd.remove(dataFrameInfo);
                            return dataFrameInfo;
                        }
                    }
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;

    }

    //增加解析后端命令数据
    protected void addData(DataFrameInfo dataFrameInfo){
        if(dataFrameInfo.command==0x29){
            //标签数据
            if(dataFrameInfo.status==0) {
                byte[] taginfo = dataFrameInfo.data;
                int tagsTotal = taginfo[3] & 0xFF;//标签张数
                if (tagsTotal > 0) {
                    LoggerUtils.d(TAG, "增加标签数据.");
                    queueTaginfo.offer(dataFrameInfo);
                }
            }
        }else if(dataFrameInfo.command==0xAA){
              //”Moduletech” //4D 6F 64 75 6C 65 74 65 63 68
            byte[] data=dataFrameInfo.data;
            if(data!=null && data.length>=10){
              boolean flag= (data[0] & 0xFF)==0x4D && (data[1] & 0xFF)==0x6F && (data[2] & 0xFF)==0x64 &&
                            (data[3] & 0xFF)==0x75 && (data[4] & 0xFF)==0x6C && (data[5] & 0xFF)==0x65 &&
                            (data[6] & 0xFF)==0x74 && (data[7] & 0xFF)==0x65 && (data[8] & 0xFF)==0x63 && (data[9] & 0xFF)==0x68;
              if(flag){
                  //开始盘点或者停止盘点的数据
                  addOtherInfoData(dataFrameInfo);
              }else{
                  //标签数据, 连续盘点标签数据0xAA
                  if(dataFrameInfo.status==0) {
                      byte[] taginfo = dataFrameInfo.data;
                      int tagsTotal = taginfo[3] & 0xFF;//标签张数
                      if (tagsTotal > 0) {
                          LoggerUtils.d(TAG, "增加标签数据.标签个数 = "+ tagsTotal);
                          queueTaginfo.offer(dataFrameInfo);
                      }
                  }
              }
            }
        }else{
            addOtherInfoData(dataFrameInfo);
        }
    }
    private void addOtherInfoData(DataFrameInfo dataFrameInfo){
        synchronized (lock) {
            Iterator<DataFrameInfo> iterator= listCmd.iterator();
            while (iterator.hasNext()){
                DataFrameInfo info= iterator.next();
                //删除5秒钟之前的数据
                if(SystemClock.elapsedRealtime()-info.time>5000){
                    iterator.remove();
                }
            }
            //其他数据
            listCmd.add(dataFrameInfo);
        }
    }

}

