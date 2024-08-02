package com.xlzn.hcpda.uhf.analysis;

import android.util.Log;

import com.xlzn.hcpda.ModuleAPI;
import com.xlzn.hcpda.uhf.analysis.UHFProtocolAnalysisBase.DataFrameInfo;
import com.xlzn.hcpda.uhf.entity.SelectEntity;
import com.xlzn.hcpda.uhf.entity.UHFReaderResult;
import com.xlzn.hcpda.uhf.entity.UHFTagEntity;
import com.xlzn.hcpda.uhf.entity.UHFVersionInfo;
import com.xlzn.hcpda.uhf.enums.LockActionEnum;
import com.xlzn.hcpda.uhf.enums.LockMembankEnum;
import com.xlzn.hcpda.uhf.enums.UHFSession;
import com.xlzn.hcpda.uhf.interfaces.IBuilderAnalysis;
import com.xlzn.hcpda.utils.DataConverter;
import com.xlzn.hcpda.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * 芯联模块，构建发送的数据和解析接收的数据
 */
public class BuilderAnalysisSLR implements IBuilderAnalysis {
    private String TAG = "BuilderAnalysisSLR";
    public boolean isTID = false;

    /**
     * 制作单次盘点的发生命令
     *
     * @return return
     */
    @Override
    public byte[] makeSingleTagInventorySendData(SelectEntity selectEntity) {
        if (selectEntity == null) {
            byte[] data = new byte[5];
            //Timeout
            data[0] = 0x01;
            data[1] = (byte) 0xE8;
            //Option
            data[2] = 0x10;
            //Metadata Flags
            data[3] = 0x00;
            data[4] = 0x06;//ant+RSSI
            return buildSendData(0x21, data);
        }

        int len = selectEntity.getLength() / 8;
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }

        byte[] data = new byte[10 + len];
        //Timeout
        data[0] = 0x01;
        data[1] = (byte) 0xE8;
        //Option
        data[2] = (byte) (selectEntity.getOption() + 0x10);
        //Metadata Flags
        data[3] = 0x00;
        data[4] = 0x06;//ant+RSSI

        // Select Address(bits)
        int address = selectEntity.getAddress();
        data[5] = (byte) ((address >> 24) & 0xFF);
        data[6] = (byte) ((address >> 16) & 0xFF);
        data[7] = (byte) ((address >> 8) & 0xFF);
        data[8] = (byte) (address & 0xFF);
        //Select data length(bits)
        data[9] = (byte) selectEntity.getLength();
        //Select data
        byte[] byteData = DataConverter.hexToBytes(selectEntity.getData());

        for (int k = 0; k < len; k++) {
            data[10 + k] = byteData[k];
        }

        return buildSendData(0x21, data);
    }

    /**
     * 解析单次盘点返回带数据
     *
     * @return return
     */
    @Override
    public UHFReaderResult<UHFTagEntity> analysisSingleTagInventoryResultData(DataFrameInfo data) {
        if (data != null) {
            if (data.status == 00) {
                //   Length       Status   Option       rssi  ant        epc                                   epc crc      crc
                //FF 13       21   00 00   10     0006  D1    11   E2 80 68 94 00 00 40 0C 6E E2 FE 08    8F 77       92F2
                //FF 13       21   00 00   14     0006  D0    11   E2 80 68 94 00 00 50 0C 6E E2 FE 06    74 3D       AA1B
                LoggerUtils.d(TAG, "单标签盘点指令返回Data:" + DataConverter.bytesToHex(data.data));
                byte[] bytes = data.data;
                int rssi = bytes[3];
                int ant = (bytes[4] & 0xFF) >> 4;
                byte[] epcbytes = Arrays.copyOfRange(bytes, 5, bytes.length - 2);
                UHFTagEntity uhfTagEntity = new UHFTagEntity();
                uhfTagEntity.setEcpHex(DataConverter.bytesToHex(epcbytes));
                uhfTagEntity.setCount(1);
                uhfTagEntity.setAnt(ant);
                uhfTagEntity.setRssi(rssi);
                return new UHFReaderResult<UHFTagEntity>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", uhfTagEntity);
            }
        }
        return new UHFReaderResult<UHFTagEntity>(UHFReaderResult.ResultCode.CODE_FAILURE, "", null);
    }

    /**
     * 制作设置连续盘点的过滤数据
     *
     * @return return
     */
    @Override
    public byte[] makeInventorySelectEntity(SelectEntity selectEntity) {
        //FF+DATALEN+AA+”Moduletech”+AA+4C+data+SubCrc+bb+CRC


        byte[] senddata = new byte[512];
        byte[] moduletech = "Moduletech".getBytes();
        //************Moduletech*************
        System.arraycopy(moduletech, 0, senddata, 0, moduletech.length);
        //***********SubCmdHighByte+SubCmdLowByte************
        int index = moduletech.length;
        int subcrcIndex = index;
        senddata[index++] = (byte) 0xAA;
        senddata[index++] = (byte) 0x4C;
        //************data*******************
        //-----SELFLAG----
        senddata[index++] = (byte) 0xFF;
        senddata[index++] = (byte) 0xFF;
        //-----SELTAGCNT--------
        senddata[index++] = (byte) 1;
        //*******SELTAGDATAN****************************
        byte[] byteData = DataConverter.hexToBytes(selectEntity.getData());
        int len = selectEntity.getLength() / 8;
        //selLEN+
        senddata[index++] = (byte) (len + 7);
        //selBANK+
        int selBANK = selectEntity.getOption();
        if (selBANK == 4) {
            selBANK = 1;
        }
        senddata[index++] = (byte) selBANK;
        //selADDR+
        int address = selectEntity.getAddress();
        senddata[index++] = (byte) ((address >> 24) & 0xFF);
        senddata[index++] = (byte) ((address >> 16) & 0xFF);
        senddata[index++] = (byte) ((address >> 8) & 0xFF);
        senddata[index++] = (byte) (address & 0xFF);
        //selbitsLEN+
        senddata[index++] = (byte) selectEntity.getLength();
        //Select data
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }
        for (int k = 0; k < len; k++) {
            senddata[index++] = byteData[k];
        }
        //****************SubCrc****************
        int subcrcTemp = 0;
        for (int k = subcrcIndex; k < index; k++) {
            subcrcTemp = subcrcTemp + (senddata[k] & 0xFF);
        }
        senddata[index++] = (byte) (subcrcTemp & 0xFF);
        //****************SubCrc****************
        senddata[index++] = (byte) 0xbb;
        //FF 22 AA 4D6F64756C6574656368  AA4C FFFF 01 13 01 00000020 60 E2000017030B020118205C16 87C7
        //FF 23 AA 4D6F64756C6574656368  AA4C FFFF 01 13 01 00000020 60 E2000017030B020118205C16 3D 4167
        return buildSendData(0XAA, Arrays.copyOf(senddata, index));
    }

    /**
     * 解析设置连续盘点过滤数据的返回结果
     *
     * @return return
     */
    @Override
    public UHFReaderResult<Boolean> analysisInventorySelectEntityResultData(DataFrameInfo data) {
        if (data != null) {
            if (data.status == 00) {
                //FF+DATALEN+AA+STATUS +”Moduletech”+AA+4C+data+CRC
                LoggerUtils.d(TAG, "设置连续盘点指定标签指令返回Data:" + DataConverter.bytesToHex(data.data));
                if ((data.data[10] & 0xFF) == 0xAA && (data.data[11] & 0xFF) == 0x4C) {
                    return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
                }
            }
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);
    }


    /**
     * 获取发送盘点的指令（省电模式）
     *
     * @return return
     */
    @Override
    public byte[] makeStartInventorySendData(SelectEntity selectEntity, boolean isTID) {
        LoggerUtils.d(TAG, "开启普通盘点模式 isTID=" + isTID);
        this.isTID = isTID;
        //FF 05 0x22 0x00 0x00 0x00 0x00 0xC8 xx xx;
        if (selectEntity == null) {
            if (isTID) {
                LoggerUtils.d(TAG, "开启普通盘点模式 盘点TID");
                byte[] data = new byte[17];
                //Option
                data[0] = 0x00;
                //Search Flags
                data[1] = (byte) 0x00;
                data[2] = (byte) 0x04;
                // Timeout
                data[3] = 0x00;
                data[4] = (byte) 0x96;
                ;//  0x03E8(1000), 0x0320 (800) ,0x02BC (700) ,0x0258 (600),0x01F4(500),0x0190(400),0x012C(300), 0xC8(200),0x96(150),0x64(100)
                //嵌入命令数量，目前该值只能为1.
                data[5] = (byte) 0x01;
                //嵌入命令的数据域的字节长度。
                data[6] = (byte) 0x09;
                //嵌入的命令码。目前只能嵌入（0X28命令）
                data[7] = (byte) 0x28;
                //嵌入命令的数据域
                //Emb Cmd Timeout
                data[8] = (byte) 0x00;
                data[9] = (byte) 0x00;
                //Emb Cmd Option
                data[10] = (byte) 0x00;
                //Read Membank
                data[11] = (byte) 0x02;
                //Read Address
                data[12] = (byte) 0x00;
                data[13] = (byte) 0x00;
                data[14] = (byte) 0x00;
                data[15] = (byte) 0x00;
                //Read Word Count
                data[16] = (byte) 0x06;
                return buildSendData(0x22, data);
            }
            byte[] data = new byte[5];
            //Option
            data[0] = 0x00;
            //Search Flags
            data[1] = 0x00;
            data[2] = 0x00;
            // Timeout
            data[3] = 0x00;
            data[4] = (byte) 0x96;
            ;//  0x03E8(1000), 0x0320 (800) ,0x02BC (700) ,0x0258 (600),0x01F4(500),0x0190(400),0x012C(300), 0xC8(200),0x96(150),0x64(100)
            return buildSendData(0x22, data);
        }

        int len = selectEntity.getLength() / 8;
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }
        byte[] data = new byte[14 + len];
        data[0] = (byte) selectEntity.getOption();
        //Search Flags
        data[1] = 0x00;
        data[2] = 0x00;
        // Timeout
        data[3] = 0x00;
        data[4] = (byte) 0x96;
        //AccessPassword
        data[5] = 0x00;
        data[6] = 0x00;
        data[7] = 0x00;
        data[8] = 0x00;
        // Select Address(bits)
        int address = selectEntity.getAddress();
        data[9] = (byte) ((address >> 24) & 0xFF);
        data[10] = (byte) ((address >> 16) & 0xFF);
        data[11] = (byte) ((address >> 8) & 0xFF);
        data[12] = (byte) (address & 0xFF);
        //Select data length(bits)
        data[13] = (byte) selectEntity.getLength();
        //Select data
        byte[] byteData = DataConverter.hexToBytes(selectEntity.getData());

        for (int k = 0; k < len; k++) {
            data[14 + k] = byteData[k];
        }
        return buildSendData(0x22, data);
    }

    /**
     * 解析接收的开始盘点的指令（省电模式）
     *
     * @return return
     */
    @Override
    public UHFReaderResult<Integer> analysisStartInventoryReceiveData(DataFrameInfo data) {
        if (data != null) {
            if (data.status == 0) {
                int tagCount = 0;
                //Search Flags
                if (((data.data[2] & 0xFF) >> 4) == 1) {
                    //标签数大于255,Tag Found为4个字节长度
                    tagCount = 256 + (data.data[6] & 0xFF);//第6,7个字节是标签张数
                } else {
                    tagCount = data.data[3] & 0xFF;//第四个字节是标签张数
                }

                return new UHFReaderResult<Integer>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", tagCount);
            }
        }
        return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    /**
     * 获取发送盘点的指令（快速模式）
     *
     * @return return
     */
    public byte[] makeStartFastModeInventorySendData(SelectEntity selectEntity, boolean isTID) {

        if (selectEntity == null) {
            // 0xFF+DATALEN+0xAA+”Moduletech”+SubCmdHighByte+SubCmdLowByte+data+SubCrc+0xbb+CRC
            //FF+DATALEN+ 0XAA+”Moduletech”+AA+48+data+SubCrc+0xbb+CRC
            //data:N字节，2字节METADATAFLAG+1字节OPTION+2字节SEARCHFLAGS+匹配过滤相关数据（由OPTION决定，同0X22指令）+盘存嵌入命令（由SEARCHFLAGS决定，同0X22指令）
            // 1.SubCrc：1字节，为SubCmdHighByte开始到data结束的所有数据相加结果的低8位值；
            byte[] senddata = new byte[512];
            byte[] moduletech = "Moduletech".getBytes();
            //************Moduletech*************
            System.arraycopy(moduletech, 0, senddata, 0, moduletech.length);
            //***********SubCmdHighByte+SubCmdLowByte************
            int index = moduletech.length;
            int subcrcIndex = index;
            senddata[index++] = (byte) 0xAA;
            senddata[index++] = (byte) 0x48;//R2000
//            senddata[index++] = (byte) 0x58;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
            //************data*******************
            //2字节METADATAFLAG
            final int count = 0X0001;//Bit0置位即标签在盘存时间内被盘存到的次数将会返回
            final int rssi = 0x0002;//BIT1置位即标签的RSSI信号值将会被返回
            final int ant = 0X0004;//BIT2置位即标签 被盘存到时所用的天线 ID号将会被返回。（逻辑天线号）
            final int tagData = 0x0080;//返回嵌入命令内存数据
            final int flag = count | rssi | ant | tagData;

            if (isTID) {
                senddata[index++] = (flag >> 8) & 0xFF;
                senddata[index++] = (byte) (flag & 0xFF);
                senddata[index++] = 0x00;//不启用匹配过滤
                //2字节SEARCHFLAGS,SEARCHFLAGS高字节的低4位表示不停止盘存过程中的停顿时间dd
                //0x10:每工作1秒中盘存时间950毫秒，停顿时间50毫秒
                //0x20:停顿时间100毫秒,0x30:停顿150，0x00:不停顿
                senddata[index++] = (0x00 | 0x10);
            } else {
                //            senddata[index++] = (flag >> 8) & 0xFF;
                senddata[index++] = 0x00;
//            senddata[index++] = (byte) (flag & 0xFF);
                senddata[index++] = 0x06;
                //1字节OPTION
                senddata[index++] = 0x00;//不启用匹配过滤
                //2字节SEARCHFLAGS,SEARCHFLAGS高字节的低4位表示不停止盘存过程中的停顿时间dd
                //0x10:每工作1秒中盘存时间950毫秒，停顿时间50毫秒
                //0x20:停顿时间100毫秒,0x30:停顿150，0x00:不停顿
//            senddata[index++] = (0x00 | 0x20);
                senddata[index++] = (byte) 0x90;//todo 0x00
            }


            if (!isTID) {
//                senddata[index++] = 0x00;
                senddata[index++] = 0x03;
                Log.e(TAG, "makeStartFastModeInventorySendData:不需要TID ");
            } else {
                Log.e(TAG, "makeStartFastModeInventorySendData:需要TID ");
                senddata[index++] = 0x04;
                //**********************************************
                //嵌入命令数量，目前该值只能为1.
                senddata[index++] = (byte) 0x01;
                //嵌入命令的数据域的字节长度。
                senddata[index++] = (byte) 0x09;
                //嵌入的命令码。目前只能嵌入（0X28命令）
                senddata[index++] = (byte) 0x28;
                //嵌入命令的数据域
                //Emb Cmd Timeout
                senddata[index++] = (byte) 0x00;
                senddata[index++] = (byte) 0x00;
                //Emb Cmd Option
                senddata[index++] = (byte) 0x00;
                //Read Membank
                senddata[index++] = (byte) 0x02;
                //Read Address
                senddata[index++] = (byte) 0x00;
                senddata[index++] = (byte) 0x00;
                senddata[index++] = (byte) 0x00;
                senddata[index++] = (byte) 0x00;
                //Read Word Count
                senddata[index++] = (byte) 0x06;
            }

            //****************data****************
            //****************SubCrc****************
            int subcrcTemp = 0;
            for (int k = subcrcIndex; k < index; k++) {
                subcrcTemp = subcrcTemp + (senddata[k] & 0xFF);
            }
            senddata[index++] = (byte) (subcrcTemp & 0xFF);
            //****************SubCrc****************
            senddata[index++] = (byte) 0xbb;
            return buildSendData(0XAA, Arrays.copyOf(senddata, index));
        }

        // 0xFF+DATALEN+0xAA+”Moduletech”+SubCmdHighByte+SubCmdLowByte+data+SubCrc+0xbb+CRC
        //FF+DATALEN+ 0XAA+”Moduletech”+AA+48+data+SubCrc+0xbb+CRC
        //data:N字节，2字节METADATAFLAG+1字节OPTION+2字节SEARCHFLAGS+匹配过滤相关数据（由OPTION决定，同0X22指令）+盘存嵌入命令（由SEARCHFLAGS决定，同0X22指令）
        // 1.SubCrc：1字节，为SubCmdHighByte开始到data结束的所有数据相加结果的低8位值；
        byte[] senddata = new byte[512];
        byte[] moduletech = "Moduletech".getBytes();
        //************Moduletech*************
        System.arraycopy(moduletech, 0, senddata, 0, moduletech.length);
        //***********SubCmdHighByte+SubCmdLowByte************
        int index = moduletech.length;
        int subcrcIndex = index;
        senddata[index++] = (byte) 0xAA;
        senddata[index++] = (byte) 0x48;
        //************data*******************
        //2字节METADATAFLAG
        final int count = 0X0001;//Bit0置位即标签在盘存时间内被盘存到的次数将会返回
        final int rssi = 0x0002;//BIT1置位即标签的RSSI信号值将会被返回
        final int ant = 0X0004;//BIT2置位即标签 被盘存到时所用的天线 ID号将会被返回。（逻辑天线号）
        final int flag = count | rssi | ant;
        senddata[index++] = (flag >> 8) & 0xFF;
        senddata[index++] = flag & 0xFF;
        //1.字节OPTION
        senddata[index++] = (byte) (selectEntity.getOption());// 启用匹配过滤
        //2.字节SEARCHFLAGS,SEARCHFLAGS高字节的低4位表示不停止盘存过程中的停顿时间dd
        //0x10:每工作1秒中盘存时间950毫秒，停顿时间50毫秒
        //0x20:停顿时间100毫秒,0x30:停顿150，0x00:不停顿
        senddata[index++] = (0x00 | 0x10);
        senddata[index++] = 0x00;
        //3. 4字节AccessPassword
        senddata[index++] = 0x00;
        senddata[index++] = 0x00;
        senddata[index++] = 0x00;
        senddata[index++] = 0x00;
        //4. Select Address(bits)
        int address = selectEntity.getAddress();
        senddata[index++] = (byte) ((address >> 24) & 0xFF);
        senddata[index++] = (byte) ((address >> 16) & 0xFF);
        senddata[index++] = (byte) ((address >> 8) & 0xFF);
        senddata[index++] = (byte) (address & 0xFF);
        //Select data length(bits)
        senddata[index++] = (byte) selectEntity.getLength();
        //Select data
        byte[] byteData = DataConverter.hexToBytes(selectEntity.getData());
        int len = selectEntity.getLength() / 8;
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }
        for (int k = 0; k < len; k++) {
            senddata[index++] = byteData[k];
        }

        //****************data****************
        //****************SubCrc****************
        int subcrcTemp = 0;
        for (int k = subcrcIndex; k < index; k++) {
            subcrcTemp = subcrcTemp + (senddata[k] & 0xFF);
        }
        senddata[index++] = (byte) (subcrcTemp & 0xFF);
        //****************SubCrc****************
        senddata[index++] = (byte) 0xbb;
        return buildSendData(0XAA, Arrays.copyOf(senddata, index));

    }

    /**
     * 获取发送快速模式连续盘点的指令
     *
     * @param selectEntity
     * @param isTID
     * @return return
     */
    @Override
    public byte[] makeStartFastModeInventorySendDataNeedTid(SelectEntity selectEntity, boolean isTID) {
        if (selectEntity == null) {
            // 0xFF+DATALEN+0xAA+”Moduletech”+SubCmdHighByte+SubCmdLowByte+data+SubCrc+0xbb+CRC
            //FF+DATALEN+ 0XAA+”Moduletech”+AA+48+data+SubCrc+0xbb+CRC
            //data:N字节，2字节METADATAFLAG+1字节OPTION+2字节SEARCHFLAGS+匹配过滤相关数据（由OPTION决定，同0X22指令）+盘存嵌入命令（由SEARCHFLAGS决定，同0X22指令）
            // 1.SubCrc：1字节，为SubCmdHighByte开始到data结束的所有数据相加结果的低8位值；
            byte[] senddata = new byte[512];
            byte[] moduletech = "Moduletech".getBytes();
            //************Moduletech*************
            System.arraycopy(moduletech, 0, senddata, 0, moduletech.length);
            //***********SubCmdHighByte+SubCmdLowByte************
            int index = moduletech.length;
            int subcrcIndex = index;
            senddata[index++] = (byte) 0xAA;
            senddata[index++] = (byte) 0x48;//R2000
//            senddata[index++] = (byte) 0x58;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
//            senddata[index++] = (byte) 0x00;//E710
            //************data*******************
            //2字节METADATAFLAG
            final int count = 0X0001;//Bit0置位即标签在盘存时间内被盘存到的次数将会返回
            final int rssi = 0x0002;//BIT1置位即标签的RSSI信号值将会被返回
            final int ant = 0X0004;//BIT2置位即标签 被盘存到时所用的天线 ID号将会被返回。（逻辑天线号）
            final int tagData = 0x0080;//返回嵌入命令内存数据
            final int flag = count | rssi | ant | tagData;

            if (isTID) {
                senddata[index++] = (flag >> 8) & 0xFF;
                senddata[index++] = (byte) (flag & 0xFF);
                senddata[index++] = 0x00;//不启用匹配过滤
                //2字节SEARCHFLAGS,SEARCHFLAGS高字节的低4位表示不停止盘存过程中的停顿时间dd
                //0x10:每工作1秒中盘存时间950毫秒，停顿时间50毫秒
                //0x20:停顿时间100毫秒,0x30:停顿150，0x00:不停顿
                senddata[index++] = (0x00 | 0x10);
            } else {
                //            senddata[index++] = (flag >> 8) & 0xFF;
                senddata[index++] = 0x00;
//            senddata[index++] = (byte) (flag & 0xFF);
                senddata[index++] = 0x06;
                //1字节OPTION
                senddata[index++] = 0x00;//不启用匹配过滤
                //2字节SEARCHFLAGS,SEARCHFLAGS高字节的低4位表示不停止盘存过程中的停顿时间dd
                //0x10:每工作1秒中盘存时间950毫秒，停顿时间50毫秒
                //0x20:停顿时间100毫秒,0x30:停顿150，0x00:不停顿
//            senddata[index++] = (0x00 | 0x20);
                senddata[index++] = (byte) 0x90;//todo 0x00
            }


            if (!isTID) {
//                senddata[index++] = 0x00;
                senddata[index++] = 0x03;
                Log.e(TAG, "makeStartFastModeInventorySendData:不需要TID ");
            } else {
                Log.e(TAG, "makeStartFastModeInventorySendData:需要TID ");
                senddata[index++] = 0x04;
                //**********************************************
                //嵌入命令数量，目前该值只能为1.
                senddata[index++] = (byte) 0x01;
                //嵌入命令的数据域的字节长度。
                senddata[index++] = (byte) 0x09;
                //嵌入的命令码。目前只能嵌入（0X28命令）
                senddata[index++] = (byte) 0x28;
                //嵌入命令的数据域
                //Emb Cmd Timeout
                senddata[index++] = (byte) 0x00;
                senddata[index++] = (byte) 0x00;
                //Emb Cmd Option
                senddata[index++] = (byte) 0x00;
                //Read Membank
                senddata[index++] = (byte) 0x02;
                //Read Address
                senddata[index++] = (byte) 0x00;
                senddata[index++] = (byte) 0x00;
                senddata[index++] = (byte) 0x00;
                senddata[index++] = (byte) 0x00;
                //Read Word Count
                senddata[index++] = (byte) 0x06;
            }

            //****************data****************
            //****************SubCrc****************
            int subcrcTemp = 0;
            for (int k = subcrcIndex; k < index; k++) {
                subcrcTemp = subcrcTemp + (senddata[k] & 0xFF);
            }
            senddata[index++] = (byte) (subcrcTemp & 0xFF);
            //****************SubCrc****************
            senddata[index++] = (byte) 0xbb;
            return buildSendData(0XAA, Arrays.copyOf(senddata, index));
        }

        // 0xFF+DATALEN+0xAA+”Moduletech”+SubCmdHighByte+SubCmdLowByte+data+SubCrc+0xbb+CRC
        //FF+DATALEN+ 0XAA+”Moduletech”+AA+48+data+SubCrc+0xbb+CRC
        //data:N字节，2字节METADATAFLAG+1字节OPTION+2字节SEARCHFLAGS+匹配过滤相关数据（由OPTION决定，同0X22指令）+盘存嵌入命令（由SEARCHFLAGS决定，同0X22指令）
        // 1.SubCrc：1字节，为SubCmdHighByte开始到data结束的所有数据相加结果的低8位值；
        byte[] senddata = new byte[512];
        byte[] moduletech = "Moduletech".getBytes();
        //************Moduletech*************
        System.arraycopy(moduletech, 0, senddata, 0, moduletech.length);
        //***********SubCmdHighByte+SubCmdLowByte************
        int index = moduletech.length;
        int subcrcIndex = index;
        senddata[index++] = (byte) 0xAA;
        senddata[index++] = (byte) 0x48;
        //************data*******************
        //2字节METADATAFLAG
        final int count = 0X0001;//Bit0置位即标签在盘存时间内被盘存到的次数将会返回
        final int rssi = 0x0002;//BIT1置位即标签的RSSI信号值将会被返回
        final int ant = 0X0004;//BIT2置位即标签 被盘存到时所用的天线 ID号将会被返回。（逻辑天线号）
        final int flag = count | rssi | ant;
        senddata[index++] = (flag >> 8) & 0xFF;
        senddata[index++] = flag & 0xFF;
        //1.字节OPTION
        senddata[index++] = (byte) (selectEntity.getOption());// 启用匹配过滤
        //2.字节SEARCHFLAGS,SEARCHFLAGS高字节的低4位表示不停止盘存过程中的停顿时间dd
        //0x10:每工作1秒中盘存时间950毫秒，停顿时间50毫秒
        //0x20:停顿时间100毫秒,0x30:停顿150，0x00:不停顿
        senddata[index++] = (0x00 | 0x10);
        senddata[index++] = 0x00;
        //3. 4字节AccessPassword
        senddata[index++] = 0x00;
        senddata[index++] = 0x00;
        senddata[index++] = 0x00;
        senddata[index++] = 0x00;
        //4. Select Address(bits)
        int address = selectEntity.getAddress();
        senddata[index++] = (byte) ((address >> 24) & 0xFF);
        senddata[index++] = (byte) ((address >> 16) & 0xFF);
        senddata[index++] = (byte) ((address >> 8) & 0xFF);
        senddata[index++] = (byte) (address & 0xFF);
        //Select data length(bits)
        senddata[index++] = (byte) selectEntity.getLength();
        //Select data
        byte[] byteData = DataConverter.hexToBytes(selectEntity.getData());
        int len = selectEntity.getLength() / 8;
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }
        for (int k = 0; k < len; k++) {
            senddata[index++] = byteData[k];
        }

        //****************data****************
        //****************SubCrc****************
        int subcrcTemp = 0;
        for (int k = subcrcIndex; k < index; k++) {
            subcrcTemp = subcrcTemp + (senddata[k] & 0xFF);
        }
        senddata[index++] = (byte) (subcrcTemp & 0xFF);
        //****************SubCrc****************
        senddata[index++] = (byte) 0xbb;
        return buildSendData(0XAA, Arrays.copyOf(senddata, index));

    }


    /**
     * 解析接收的开始盘点的指令（多标签手持机模式）
     *
     * @return return
     */
    public List<UHFTagEntity> analysisFastModeTagInfoReceiveData(DataFrameInfo data) {
        //今天的06指令
        //                     RSSI   天线   EPC长度   PC
        //FF 15 AA 00 00 00 06 D8     11     10        34 00   7A 7A 32 38 2D 30 30 30 30 39 39 00 E1 8A 8052

        //E710返回
        //FF 18 AA 00 00 00 87 01 C4 01 00 00 10 34 00 7A 7A 32 38 2D 30 30 30 30 39 39 00 E1 8A 6E19


        //FF 14 AA 00 00 00 04 11 10 30 00 E2 00 53 80 80 04 00 41 12 60 95 87 16 5C   6AD8


        //FF 数据头
        //14 数据长度
        //AA 命令字
        //00 00 状态
        //00 04  flag值
        //11 天线
        //10 EPC长度，包括 pc值+epc+epcCRC
        //30 00  PC值
        //E2 00 53 80 80 04 00 41 12 60 95 87  epc数据
        //16 5C 标签CRC
        if (data != null) {
            //00 87 01 C9 11 00 60 E2 00 34 12 01 2F FC 00 0B 45 DE 87   103000E2000017010B014318405BA1B2F2
            //00 87 01 BD 01 00 00 1030001DDD2CCC0000501158456DA117B9
            if (data.status == 0) {

                byte[] taginfo = data.data;
                String s = DataConverter.bytesToHex(taginfo);
                Log.e("UTU", "盘点接收: " + s);
                UHFTagEntity uhfTagEntity = new UHFTagEntity();
                int ant = (taginfo[3] & 0xFF) >> 4; //天线
                int epcLen = (taginfo[4] & 0xFF);   //EPC长度 包括:pc值+epc+epcCRC
                byte[] pcBytes = new byte[]{
                        taginfo[5],
                        taginfo[6]
                };
                int epcIdLen = epcLen - 2 - 2;//长度减去2个字节pc和两个字节epccrc
                byte[] epcBytes = new byte[epcIdLen];
                for (int m = 0; m < epcIdLen; m++) {
                    epcBytes[m] = taginfo[7 + m];
                }
                uhfTagEntity.setAnt(ant);
                byte b = taginfo[2];
                int flag = (int) b;
                uhfTagEntity.setRssi(flag);
                uhfTagEntity.setCount(1);
                uhfTagEntity.setEcpHex(DataConverter.bytesToHex(epcBytes));
                if (uhfTagEntity.getEcpHex() == null) {
                    uhfTagEntity.setEcpHex("");
                }
                uhfTagEntity.setPcHex(DataConverter.bytesToHex(pcBytes));
                List<UHFTagEntity> list = new ArrayList<>();
                list.add(uhfTagEntity);
                return list;
            } //00 07 00 01    01  C9 11   00 80   30 00 E2 00 00 17 01 0B 00 50 17 50 61 70 BB 55
        }
        return null;
    }

    /**
     * 解析连续盘点数据-之前的快速模式
     *
     * @return return
     */

    public List<UHFTagEntity> analysisFastModeTagInfoReceiveDataOld(DataFrameInfo data) {
        //FF 16 AA 00 00   00 07 01 DB 11 10 3000E2000017010B020718205C25378C9A63
        //FF 数据头
        //16 数据长度
        //AA 命令字
        //00 00 状态
        //______________________纯数据段___________________________________________
        //00 07  flag值，和发送的数据一样
        //01    标签张数
        //DB    RSSI
        //11    天线ID ，高4位为发射天线，低4位为接收天线。
        //10 EPC长度，包括 pc值+epc+epcCRC
        //30 00  PC值
        //E2 00 00 17 01 0B 00 46 17 50 61 6E  epc数据
        //81 75 标签CRC
        //__________________________________________________________________
        //58 BD 整个数据CRC
        if (data != null) {
            //00 87 01 C9 11 00 60 E2 00 34 12 01 2F FC 00 0B 45 DE 87   103000E2000017010B014318405BA1B2F2
            //
            // 得到EPC = E6898BE5B895
            // 得到EPC = 0006E3010A1C00 E6898BE5B895A2E9
            if (data.status == 0) {
                LoggerUtils.d(TAG, "解析盘点数据：2222222222 = " + DataConverter.bytesToHex(data.data));
                byte[] taginfo = data.data;
                int tagsTotal = taginfo[2] & 0xFF;//标签张数
                int statIndex = 3;
                List<UHFTagEntity> list = new ArrayList<>();
                for (int k = 0; k < tagsTotal; k++) {
                    int rssi = taginfo[statIndex++];   //RSSI
                    int ant = (taginfo[statIndex++] & 0xFF) >> 4; //天线
                    byte[] tidBytes = null;
                    int tidLen = ((taginfo[statIndex++] & 0xFF) << 8) | (taginfo[statIndex++] & 0xFF);//嵌入命令的数据
                    if (tidLen > 0) {
                        tidLen = (tidLen / 8);
                        int starAdd = statIndex;
                        int endAdd = starAdd + tidLen;
                        tidBytes = Arrays.copyOfRange(taginfo, starAdd, endAdd);
                        statIndex = endAdd;
                        // LoggerUtils.d(TAG, "解析盘点数据 statIndex："+statIndex);
                    }
                    UHFTagEntity uhfTagEntity = new UHFTagEntity();
                    if (tidBytes != null) {
                        uhfTagEntity.setTidHex(DataConverter.bytesToHex(tidBytes));
                    } else {
                        if (isTID) {
                            continue;
                        }
                    }

                    int epcLen = (taginfo[statIndex++] & 0xFF);   //EPC长度 包括:pc值+epc+epcCRC
                    byte[] pcBytes = new byte[]{
                            taginfo[statIndex++],
                            taginfo[statIndex++]
                    };
                    int epcIdLen = epcLen - 2 - 2;
                    byte[] epcBytes = new byte[epcIdLen];
                    for (int m = 0; m < epcIdLen; m++) {
                        epcBytes[m] = taginfo[statIndex++];
                    }
                    //最后有两个字节的epc校验码，所以加3，就直接跳到下一张标签
                    statIndex = statIndex + 2;

                    uhfTagEntity.setAnt(ant);
                    uhfTagEntity.setRssi(rssi);
                    uhfTagEntity.setCount(1);
                    uhfTagEntity.setEcpHex(DataConverter.bytesToHex(epcBytes));
                    if (uhfTagEntity.getEcpHex() == null) {
                        uhfTagEntity.setEcpHex("");
                    }
                    uhfTagEntity.setPcHex(DataConverter.bytesToHex(pcBytes));
                    list.add(uhfTagEntity);
                }
                if (list == null || list.size() < tagsTotal) {
                    LoggerUtils.d(TAG, "解析盘点数据异常 tagsTotal：" + tagsTotal + "  list.size()=" + list.size());
                }
                return list;
            } //00 07 00 01    01  C9 11   00 80   30 00 E2 00 00 17 01 0B 00 50 17 50 61 70 BB 55
        }
        return null;
    }

    public UHFReaderResult<Boolean> analysisStartFastModeInventoryReceiveData(DataFrameInfo data, boolean isTID) {
        this.isTID = isTID;
        //0xFF+DATALEN+0XAA+STATUS +”Moduletech”+SubCmdHighByte+SubCmdLowByte+data+CRC
        if (data != null) {
            if (data.status == 00) {
                LoggerUtils.d(TAG, "开始盘点指令返回Data:" + DataConverter.bytesToHex(data.data));
                if ((data.data[10] & 0xFF) == 0xAA && (data.data[11] & 0xFF) == 0x48) {
                    return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
                }
                //4D 6F 64 75 6C 65 74 65 63 68 AA 48
            }
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);
    }

    /**
     * 解析接收的发送快速模式连续盘点的指令AA48 带TID的
     *
     * @param data
     * @param isTID
     * @return return
     */
    @Override
    public UHFReaderResult<Boolean> analysisStartFastModeInventoryReceiveDataNeedTid(DataFrameInfo data, boolean isTID) {
        this.isTID = isTID;
        //0xFF+DATALEN+0XAA+STATUS +”Moduletech”+SubCmdHighByte+SubCmdLowByte+data+CRC
        if (data != null) {
            if (data.status == 00) {
                LoggerUtils.d(TAG, "开始盘点指令返回Data:" + DataConverter.bytesToHex(data.data));
                if ((data.data[10] & 0xFF) == 0xAA && (data.data[11] & 0xFF) == 0x48) {
                    return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
                }
                //4D 6F 64 75 6C 65 74 65 63 68 AA 48
            }
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);

    }

    public byte[] makeStopFastModeInventorySendData() {
        //FF+DATALEN+0XAA+”Moduletech”+AA+49+SubCrc+0xbb+CRC
        byte[] data = new byte[14];
        byte[] moduletech = "Moduletech".getBytes();
        System.arraycopy(moduletech, 0, data, 0, moduletech.length);
        data[10] = (byte) 0xAA;
        data[11] = (byte) 0x49;
        int subcrcTemp = 0xAA + 0x49;
        data[12] = (byte) (subcrcTemp & 0xFF);
        data[13] = (byte) 0xbb;
        return buildSendData(0XAA, data);
    }

    public UHFReaderResult<Boolean> analysisStopFastModeInventoryReceiveData(DataFrameInfo data) {
        //0xFF+DATALEN+0XAA+STATUS +”Moduletech”+SubCmdHighByte+SubCmdLowByte+data+CRC
        if (data != null) {
            if (data.status == 00) {
                LoggerUtils.d(TAG, "停止盘点指令返回Data:" + DataConverter.bytesToHex(data.data));
                if ((data.data[10] & 0xFF) == 0xAA && (data.data[11] & 0xFF) == 0x49) {
                    return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
                }
            }
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);
    }

    /**
     * 获取标签数据的命令
     *
     * @return return
     */
    @Override
    public byte[] makeGetTagInfoSendData() {
        final int count = 0X0001;//Bit0置位即标签在盘存时间内被盘存到的次数将会返回
        final int rssi = 0x0002;//BIT1置位即标签的RSSI信号值将会被返回
        final int ant = 0X0004;//BIT2置位即标签 被盘存到时所用的天线 ID号将会被返回。（逻辑天线号）
        final int tagData = 0x0080;//返回嵌入命令内存数据
        final int flag = count | rssi | ant | tagData;
//
        byte[] data = new byte[3];
        data[0] = (byte) ((flag >> 8) & 0xFF);
        data[1] = (byte) (flag & 0xFF);//返回还没有被获取的标签信息
        data[2] = 0x00;
        //只获取标签次数+EPC数据+RSSI+天线号
        return buildSendData(0x29, data);
    }

    /**
     * 解析盘点数据
     *
     * @return return
     */
    @Override
    public List<UHFTagEntity> analysisTagInfoReceiveData(DataFrameInfo data) {


        //  FF	 4A	      29	 00 00	     00  BF	          00	         02
        //  SOH   Length  Opcode   Status    Metadata Flags   Read Options  Tag Count
        //    07	        E3	     11	   0E 22 2A  	00 00 8D 8F	  00 00       	00 00
        //  Read Count   RSSI   Ant ID  Frequency    Timestamp      RFU        Tag Data Length
        //  00 60         	20 00     	  11 11 22 22 33 33 44 44	      C2 41
        //  EPC length       PC word           Tag EPC ID                  Tag CRC

        //FFEC2900000087 00 08 01 CB 11 00 60 01 00 00 00 00 00 00 00 00 00 00 00    00 80 3400E2000017010B011018405B59DAA0
        //                     01 C5 11 00 00    00 803000E2000017010B016418425BBC706E
        //                     01 C9 11 00 00    00 8030008888222233334444555566663B49
        //                     01 CA 11 00 60 00 00 00 00 00 00 00 00 00 00 00 00    00 803000E2000017010B001718405AA26834
        //                     01 C9 11 00 00 00 803000E2000017010B022718205C4773DC
        //                     01 C7 11 00 60 00 00 00 00 00 00 00 00 00 00 00 00    00803000E2000017010B005018405ADBF136
        //                     01 C3 11 00 00 00 803000E2000017010B006518405B02B7EA
        //                     01 CB 11 00 60 000000000000000000000000  00803000E2000017010B021118205C27DE11505D

        //FFE72900000087 00 09 01 C8 11 00 00  008030008888222233334444555566663B49
        //                     01 CB 11 00 00  00803000111168940000400C6EE2FE02016D
        //                     01CA1100200000000000803000E2000017010B012818405B7A3DC702D51100200000000000803000E2000017010B001918405AA3B7BD01D11100200000000000803000E2000017010B012718405B81064A01C811000000803000E2000017010B005018405ADBF13601CD1100200004000000803000E2000017010B003518405AC3588401C711000000803000E2000017010B006918405B045C0701C71100200000000000803000E2000017010B006218405AF9BD7BC0DD

        //FF192900000007 00 01 01 C7 11 00803000E2000017010B00461750616E817558BD
        //FF 数据头
        //19 数据长度
        //29 命令字
        //00 00 状态
        //______________________纯数据段___________________________________________
        //00 07  flag值，和发送的数据一样
        //00    读取操作
        //01    标签张数
        //01    读取次数
        //C7    RSSI
        //11    天线ID ，高4位为发射天线，低4位为接收天线。
        //00 80  EPC长度，包括 pc值+epc+epcCRC
        //30 00  PC值
        //E2 00 00 17 01 0B 00 46 17 50 61 6E  epc数据
        //81 75 标签CRC
        //__________________________________________________________________
        //58 BD 整个数据CRC
        if (data != null) {
            if (data.status == 0) {

                //LoggerUtils.d(TAG, "解析盘点数据："+DataConverter.bytesToHex(data.data));
                byte[] taginfo = data.data;
                int tagsTotal = taginfo[3] & 0xFF;//标签张数
                int statIndex = 4;

                List<UHFTagEntity> list = new ArrayList<>();
                for (int k = 0; k < tagsTotal; k++) {
                    int count = taginfo[statIndex] & 0xFF;// 读取次数
                    int rssi = taginfo[++statIndex];   //RSSI
                    int ant = (taginfo[++statIndex] & 0xFF) >> 4; //天线
                    byte[] tidBytes = null;
                    int tidLen = ((taginfo[++statIndex] & 0xFF) << 8) | (taginfo[++statIndex] & 0xFF);//嵌入命令的数据
                    // LoggerUtils.d(TAG, "解析盘点数据 k："+k +"  tidLen="+tidLen);
                    if (tidLen > 0) {
                        tidLen = (tidLen / 8);
                        int starAdd = (++statIndex);
                        int endAdd = starAdd + tidLen;
                        tidBytes = Arrays.copyOfRange(taginfo, starAdd, endAdd);
                        statIndex = endAdd - 1;
                        // LoggerUtils.d(TAG, "解析盘点数据 statIndex："+statIndex);
                    }
                    int epcLen = ((taginfo[++statIndex] & 0xFF) << 8) | (taginfo[++statIndex] & 0xFF);   //EPC长度 包括:pc值+epc+epcCRC
                    // LoggerUtils.d(TAG, "解析盘点数据 epcLen："+epcLen);
                    byte[] pcBytes = new byte[]{
                            taginfo[++statIndex],
                            taginfo[++statIndex]
                    };
                    int epcIdLen = (epcLen / 8) - 2 - 2;
                    byte[] epcBytes = new byte[epcIdLen];
                    for (int m = 0; m < epcIdLen; m++) {
                        epcBytes[m] = taginfo[++statIndex];
                    }
                    //最后有两个字节的epc校验码，所以加3，就直接跳到下一张标签
                    statIndex = statIndex + 3;
                    UHFTagEntity uhfTagEntity = new UHFTagEntity();
                    if (tidBytes != null) {
                        uhfTagEntity.setTidHex(DataConverter.bytesToHex(tidBytes));
                    } else {
                        if (isTID) {
                            continue;
                        }
                    }
                    uhfTagEntity.setAnt(ant);
                    uhfTagEntity.setRssi(rssi);
                    uhfTagEntity.setCount(count);
                    uhfTagEntity.setEcpHex(DataConverter.bytesToHex(epcBytes));
                    if (uhfTagEntity.getEcpHex() == null) {
                        uhfTagEntity.setEcpHex("");
                    }
                    uhfTagEntity.setPcHex(DataConverter.bytesToHex(pcBytes));
                    list.add(uhfTagEntity);
                }
                return list;
            } //00 07 00 01    01  C9 11   00 80   30 00 E2 00 00 17 01 0B 00 50 17 50 61 70 BB 55
        }
        return null;
    }


    /**
     * 获取模块版本
     *
     * @return return
     */
    @Override
    public byte[] makeGetVersionSendData() {
        //FF  00	03	1D 0C
        return buildSendData(0x03, null);
    }

    /**
     * 解析获取温度返回的数据
     *
     * @return return
     */
    @Override
    public UHFReaderResult<UHFVersionInfo> analysisVersionData(DataFrameInfo data) {
        if (data != null) {
            if (data.status == 0) {
                byte[] version = data.data;
                //FF 14 03 00 00 15010400A1000201202007032007030000000010DEE7
                //BootLoader     Hardware      Firmware data   Firmware Version
                //15 01 04 00    A1 00 02 01   20 20 07 03      20 07 03 00         00000010DEE7
                // BootLoader Ver 0-3
                // Hardware Ver  4-7
                //Firmware data 8-11
                //Firmware Version 12-15   15 01 04 00 A1 00 02 01 20 20 07 03    20 07 03 00
                //Supported Protocol 16-19
                UHFVersionInfo versionInfo = new UHFVersionInfo();
                versionInfo.setFirmwareVersion(DataConverter.bytesToHex(Arrays.copyOfRange(version, 8, 12)));
                versionInfo.setHardwareVersion(DataConverter.bytesToHex(Arrays.copyOfRange(version, 4, 8)));
                LoggerUtils.d(TAG, "固件版本:" + versionInfo.getFirmwareVersion());
                LoggerUtils.d(TAG, "硬件版本:" + versionInfo.getHardwareVersion());
                return new UHFReaderResult<UHFVersionInfo>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", versionInfo);
            }
        }
        return new UHFReaderResult(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    /**
     * 设置session
     */
    @Override
    public byte[] makeSetSessionSendData(UHFSession value) {
        byte[] data = new byte[3];
        data[0] = 0x05;
        data[1] = 0x00;
        data[2] = (byte) value.getValue();
        return buildSendData(0x9B, data);
    }

    /**
     * 解析设置session返回的数据
     */
    @Override
    public UHFReaderResult<Boolean> analysisSetSessionResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            LoggerUtils.d(TAG, "SetSession success");
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        LoggerUtils.d(TAG, "SetSession fail");
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    /**
     * 获取session
     */
    @Override
    public byte[] makeGetSessionSendData() {
        byte[] data = new byte[2];
        data[0] = 0x05;
        data[1] = 0x00;
        return buildSendData(0x6B, data);
    }

    /**
     * 解析获取session
     */
    @Override
    public UHFReaderResult<UHFSession> analysisGetSessionResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            if (data.data[0] == 0x05 && data.data[1] == 0x00) {
                return new UHFReaderResult<UHFSession>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", UHFSession.getValue(data.data[2]));
            }
        }
        return new UHFReaderResult<UHFSession>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    @Override
    public byte[] makeSetPowerSendData(int power) {
        power = power * 100;
        byte[] data = new byte[6];
        data[0] = 0x03;//potion
        data[1] = 0x01;//天线
        data[2] = (byte) ((power >> 8) & 0xff);//读功率
        data[3] = (byte) (power & 0xff);//读功率
        data[4] = (byte) ((power >> 8) & 0xff);//写功率
        data[5] = (byte) (power & 0xff);//写功率
        return buildSendData(0x91, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisSetPowerResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);
    }

    @Override
    public byte[] makeGetPowerSendData() {
        byte[] data = new byte[1];
        data[0] = 0x03;
        return buildSendData(0x61, data);
    }

    @Override
    public UHFReaderResult<Integer> analysisGetPowerResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            byte[] temp = data.data;
            //Option
            //TX ant num
            int power = (((temp[2] & 0xFF) << 8) | (temp[3] & 0xFF)) / 100;
            //FF 29 61 00 00
            //03 01  0B B8  0B B8 0200000000030000000004000000000500000000060000000007000000000800000000B027

             /*
             if(temp!=null && temp.length==7){
                 int curr=((temp[1]&0xFF)<<8) | (temp[2]&0xFF);
                 int max=((temp[3]&0xFF)<<8) | (temp[4]&0xFF);
                 int min=((temp[5]&0xFF)<<8) | (temp[6]&0xFF);
             }
             */
            LoggerUtils.d(TAG, "power=" + power);
            return new UHFReaderResult<Integer>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", power);
        }
        return new UHFReaderResult<Integer>(UHFReaderResult.ResultCode.CODE_FAILURE, "", 0);
    }

    @Override
    public byte[] makeSetFrequencyRegionSendData(int FrequencyRegion) {
        byte[] data = new byte[1];
        data[0] = (byte) FrequencyRegion;
        return buildSendData(0x97, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisSetFrequencyRegionResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);
    }

    @Override
    public byte[] makeGetFrequencyRegionSendData() {
        return buildSendData(0x67, null);
    }

    @Override
    public UHFReaderResult<Integer> analysisGetFrequencyRegionResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            int f = data.data[0] & 0xFF;
            return new UHFReaderResult<Integer>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", f);
        }
        return new UHFReaderResult<Integer>(UHFReaderResult.ResultCode.CODE_FAILURE, "", -1);
    }

    @Override
    public byte[] makeGetTemperatureSendData() {
        return buildSendData(0x72, null);
    }

    @Override
    public UHFReaderResult<Integer> analysisGetTemperatureResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            int f = data.data[0];
            return new UHFReaderResult<Integer>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", f);
        }
        return new UHFReaderResult<Integer>(UHFReaderResult.ResultCode.CODE_FAILURE, "", 0);
    }

    @Override
    public byte[] makeSetDynamicTargetSendData(int value) {
        byte[] data = new byte[3];
        data[0] = 0x05;
        data[1] = 0x01;
        data[2] = 0x00;
        data[3] = (byte) value;
        return buildSendData(0x9B, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisSetDynamicTargetResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);
    }

    @Override
    public byte[] makeSetStaticTargetSendData(int value) {
        byte[] data = new byte[3];
        data[0] = 0x05;
        data[1] = 0x01;
        data[2] = 0x01;
        data[3] = (byte) value;
        return buildSendData(0x9B, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisSetStaticTargetResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);
    }

    @Override
    public byte[] makeGetTargetSendData() {
        byte[] data = new byte[2];
        data[0] = 0x05;
        data[1] = 0x01;
        return buildSendData(0x6B, data);
    }

    @Override
    public UHFReaderResult<int[]> analysisGetTargetResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            if (data.data[0] == 0x05 && data.data[1] == 0x01) {
                return new UHFReaderResult<int[]>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", new int[]{data.data[2], data.data[3]});
            }
        }
        return new UHFReaderResult<int[]>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    @Override
    public byte[] makeReadSendData(String password, int membank, int address, int wordCount, SelectEntity selectEntity) {
        if (selectEntity == null) {
            byte[] data = new byte[13];
            data[0] = 0x07;//0x03; //timeout
            data[1] = (byte) 0xD0;//0xE8;//timeout
            data[2] = (byte) 0x05;//Option 0x00 不需要密码，  0x05 需要密码
            data[3] = (byte) membank;
            data[4] = (byte) ((address >> 24) & 0xFF);
            data[5] = (byte) ((address >> 16) & 0xFF);
            data[6] = (byte) ((address >> 8) & 0xFF);
            data[7] = (byte) (address & 0xFF);
            data[8] = (byte) wordCount;
            byte[] pwd = DataConverter.hexToBytes(password);
            data[9] = pwd[0];
            data[10] = pwd[1];
            data[11] = pwd[2];
            data[12] = pwd[3];
            return buildSendData(0x28, data);
        }
        int len = selectEntity.getLength() / 8;
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }

        byte[] data = new byte[18 + len];
        data[0] = 0x03; //timeout
        data[1] = (byte) 0xE8;//timeout
        data[2] = (byte) selectEntity.getOption();//Option
        data[3] = (byte) membank;
        data[4] = (byte) ((address >> 24) & 0xFF);
        data[5] = (byte) ((address >> 16) & 0xFF);
        data[6] = (byte) ((address >> 8) & 0xFF);
        data[7] = (byte) (address & 0xFF);
        data[8] = (byte) wordCount;
        byte[] pwd = DataConverter.hexToBytes(password);
        data[9] = pwd[0];
        data[10] = pwd[1];
        data[11] = pwd[2];
        data[12] = pwd[3];
        data[13] = (byte) ((selectEntity.getAddress() >> 24) & 0xFF);//address
        data[14] = (byte) ((selectEntity.getAddress() >> 16) & 0xFF);//address
        data[15] = (byte) ((selectEntity.getAddress() >> 8) & 0xFF);//address
        data[16] = (byte) (selectEntity.getAddress() & 0xFF);//address
        data[17] = (byte) selectEntity.getLength();
        byte[] byteData = DataConverter.hexToBytes(selectEntity.getData());

        for (int k = 0; k < len; k++) {
            data[18 + k] = byteData[k];
        }
        return buildSendData(0x28, data);
    }

    @Override
    public UHFReaderResult<String> analysisReadResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            byte[] tag = Arrays.copyOfRange(data.data, 1, data.data.length);
            return new UHFReaderResult<String>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", DataConverter.bytesToHex(tag));
        }
        return new UHFReaderResult<String>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    @Override
    public byte[] makeWriteSendData(String password, int membank, int address, int wordCount, String hexData, SelectEntity selectEntity) {

        if (hexData.length() / 4 > wordCount) {
            hexData = hexData.substring(0, wordCount * 4);
        }

        if (selectEntity == null) {
            byte[] byteData = DataConverter.hexToBytes(hexData);
            byte[] data = new byte[12 + byteData.length];
            data[0] = 0x07;//0x03; //timeout
            data[1] = (byte) 0xD0;//0xE8;//timeout
            data[2] = (byte) 0x05;//Option 0x00 不需要密码，  0x05 需要密码
            data[3] = (byte) ((address >> 24) & 0xFF);
            data[4] = (byte) ((address >> 16) & 0xFF);
            data[5] = (byte) ((address >> 8) & 0xFF);
            data[6] = (byte) (address & 0xFF);
            data[7] = (byte) membank;//写入的区域
            byte[] pwd = DataConverter.hexToBytes(password);
            data[8] = pwd[0];
            data[9] = pwd[1];
            data[10] = pwd[2];
            data[11] = pwd[3];
            for (int k = 0; k < byteData.length; k++) {
                data[12 + k] = byteData[k];
            }
            return buildSendData(0x24, data);
        }
        //------过滤的数据长度--------
        int len = selectEntity.getLength() / 8;
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }

        byte[] byteData = DataConverter.hexToBytes(hexData);
        byte[] data = new byte[12 + 4 + 1 + byteData.length + len];
        data[0] = 0x03; //timeout
        data[1] = (byte) 0xE8;//timeout
        data[2] = (byte) selectEntity.getOption();//Option 0x00 不需要密码，  0x05 需要密码
        data[3] = (byte) ((address >> 24) & 0xFF);
        data[4] = (byte) ((address >> 16) & 0xFF);
        data[5] = (byte) ((address >> 8) & 0xFF);
        data[6] = (byte) (address & 0xFF);
        data[7] = (byte) membank;//写入的区域
        byte[] pwd = DataConverter.hexToBytes(password);
        data[8] = pwd[0];
        data[9] = pwd[1];
        data[10] = pwd[2];
        data[11] = pwd[3];

        //--------过滤-------------
        data[12] = (byte) ((selectEntity.getAddress() >> 24) & 0xFF);
        data[13] = (byte) ((selectEntity.getAddress() >> 16) & 0xFF);
        data[14] = (byte) ((selectEntity.getAddress() >> 8) & 0xFF);
        data[15] = (byte) (selectEntity.getAddress() & 0xFF);
        data[16] = (byte) selectEntity.getLength();
        byte[] selectData = DataConverter.hexToBytes(selectEntity.getData());
        for (int k = 0; k < len; k++) {
            data[17 + k] = selectData[k];
        }
        //--------过滤-------------

        int index = 17 + len;
        for (int k = 0; k < byteData.length; k++) {
            data[index + k] = byteData[k];
        }
        return buildSendData(0x24, data);


    }

    @Override
    public UHFReaderResult<Boolean> analysisWriteResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    @Override
    public byte[] makeKillSendData(String password, SelectEntity selectEntity) {
        if (selectEntity == null) {
            byte[] data = new byte[8];
            data[0] = 0x03; //timeout
            data[1] = (byte) 0xE8;//timeout
            data[2] = (byte) 0x00;//只支持0x00
            byte[] pwd = DataConverter.hexToBytes(password);
            data[3] = pwd[0];
            data[4] = pwd[1];
            data[5] = pwd[2];
            data[6] = pwd[3];
            data[7] = 0x00;//RFU
            return buildSendData(0x26, data);
        }
        int len = selectEntity.getLength() / 8;
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }

        byte[] data = new byte[13 + len];
        data[0] = 0x03; //timeout
        data[1] = (byte) 0xE8;//timeout
        data[2] = (byte) selectEntity.getOption();//Option
        byte[] pwd = DataConverter.hexToBytes(password);
        data[3] = pwd[0];
        data[4] = pwd[1];
        data[5] = pwd[2];
        data[6] = pwd[3];
        data[7] = 0x00;//RFU

        data[8] = (byte) ((selectEntity.getAddress() >> 24) & 0xFF);//address
        data[9] = (byte) ((selectEntity.getAddress() >> 16) & 0xFF);//address
        data[10] = (byte) ((selectEntity.getAddress() >> 8) & 0xFF);//address
        data[11] = (byte) (selectEntity.getAddress() & 0xFF);//address
        data[12] = (byte) selectEntity.getLength();
        byte[] byteData = DataConverter.hexToBytes(selectEntity.getData());

        for (int k = 0; k < len; k++) {
            data[13 + k] = byteData[k];
        }

        return buildSendData(0x26, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisKillResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    @Override
    public byte[] makeLockSendData(String password, LockMembankEnum membankEnum, LockActionEnum actionEnum, SelectEntity selectEntity) {
        byte[] membankByte = new byte[2];
        byte[] actionByte = new byte[2];
        switch (membankEnum) {
            case EPC:
                switch (actionEnum) {
                    case LOCK:
                        membankByte[0] = 32;
                        actionByte[0] = 32;
                        break;
                    case UNLOCK:
                        membankByte[0] = 32;
                        actionByte[0] = 0;
                        break;
                    case PERMANENT_LOCK:
                        membankByte[0] = 48;
                        actionByte[0] = 32;
                        break;
                    case PERMANENT_UNLOCK:
                        membankByte[0] = 48;
                        actionByte[0] = 16;
                        break;
                }
                break;
            case TID:
                switch (actionEnum) {
                    case LOCK:
                        membankByte[0] = 8;
                        actionByte[0] = 8;
                        break;
                    case UNLOCK:
                        membankByte[0] = 8;
                        actionByte[0] = 0;
                        break;
                    case PERMANENT_LOCK:
                        membankByte[0] = 12;
                        actionByte[0] = 12;
                        break;
                    case PERMANENT_UNLOCK:
                        membankByte[0] = 12;
                        actionByte[0] = 4;
                        break;
                }
                break;
            case USER:
                switch (actionEnum) {
                    case LOCK:
                        membankByte[0] = 2;
                        actionByte[0] = 2;
                        break;
                    case UNLOCK:
                        membankByte[0] = 2;
                        actionByte[0] = 0;
                        break;
                    case PERMANENT_LOCK:
                        membankByte[0] = 3;
                        actionByte[0] = 3;
                        break;
                    case PERMANENT_UNLOCK:
                        membankByte[0] = 3;
                        actionByte[0] = 1;
                        break;
                }
                break;
            case KillPwd:
                switch (actionEnum) {
                    case LOCK:
                        membankByte[1] = 2;
                        actionByte[1] = 2;
                        break;
                    case UNLOCK:
                        membankByte[1] = 2;
                        actionByte[1] = 0;
                        break;
                    case PERMANENT_LOCK:
                        membankByte[1] = 3;
                        actionByte[1] = 3;
                        break;
                    case PERMANENT_UNLOCK:
                        membankByte[1] = 3;
                        actionByte[1] = 1;
                        break;
                }

                break;
            case AccessPwd:
                switch (actionEnum) {
                    case LOCK:
                        membankByte[0] = (byte) 128;
                        actionByte[0] = (byte) 128;
                        break;
                    case UNLOCK:
                        membankByte[0] = (byte) 128;
                        actionByte[0] = 0;
                        break;
                    case PERMANENT_LOCK:
                        membankByte[0] = (byte) 192;
                        actionByte[0] = (byte) 192;
                        break;
                    case PERMANENT_UNLOCK:
                        membankByte[0] = (byte) 192;
                        actionByte[0] = 64;
                        break;
                }
                break;
        }

        //-------------------------------

        byte temp = membankByte[0];
        membankByte[0] = membankByte[1];
        membankByte[1] = temp;

        temp = actionByte[0];
        actionByte[0] = actionByte[1];
        actionByte[1] = temp;

        String hexMask = DataConverter.bytesToHex(membankByte);
        String hexAction = DataConverter.bytesToHex(actionByte);
        LoggerUtils.d(TAG, "lock hexMask=" + hexMask);
        LoggerUtils.d(TAG, "lock hexAction=" + hexAction);

        if (selectEntity == null) {
            byte[] data = new byte[11];
            data[0] = 0x03; //timeout
            data[1] = (byte) 0xE8;//timeout
            data[2] = (byte) 0x00;//只支持0x00
            byte[] pwd = DataConverter.hexToBytes(password);
            data[3] = pwd[0];
            data[4] = pwd[1];
            data[5] = pwd[2];
            data[6] = pwd[3];
            byte[] byteMask = DataConverter.hexToBytes(hexMask);
            data[7] = byteMask[0];
            data[8] = byteMask[1];
            byte[] byteAction = DataConverter.hexToBytes(hexAction);
            data[9] = byteAction[0];
            data[10] = byteAction[1];
            return buildSendData(0x25, data);
        }
        int len = selectEntity.getLength() / 8;
        if (selectEntity.getLength() % 8 != 0) {
            len += 1;
        }
        byte[] data = new byte[16 + len];
        data[0] = 0x03; //timeout
        data[1] = (byte) 0xE8;//timeout
        data[2] = (byte) selectEntity.getOption();
        byte[] pwd = DataConverter.hexToBytes(password);
        data[3] = pwd[0];
        data[4] = pwd[1];
        data[5] = pwd[2];
        data[6] = pwd[3];
        byte[] byteMask = DataConverter.hexToBytes(hexMask);
        data[7] = byteMask[0];
        data[8] = byteMask[1];
        byte[] byteAction = DataConverter.hexToBytes(hexAction);
        data[9] = byteAction[0];
        data[10] = byteAction[1];

        data[11] = (byte) ((selectEntity.getAddress() >> 24) & 0xFF);//address
        data[12] = (byte) ((selectEntity.getAddress() >> 16) & 0xFF);//address
        data[13] = (byte) ((selectEntity.getAddress() >> 8) & 0xFF);//address
        data[14] = (byte) (selectEntity.getAddress() & 0xFF);//address
        data[15] = (byte) selectEntity.getLength();

        byte[] byteData = DataConverter.hexToBytes(selectEntity.getData());
        for (int k = 0; k < len; k++) {
            data[16 + k] = byteData[k];
        }

        return buildSendData(0x25, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisLockResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }


    /*
     * 1.主机到模块的通信格式：
     * Header   +   Data Length  +  Command  +  Data  +  CRC-16
     * Header: 一字节 固定0XFF
     * DataLength: 一字节，Data数据块的字节数
     * Command：一字节，命令码
     * Data：数据块，高字节在前面。
     * CRC-16: 二字节循环冗余码，高字节在前，从DataLength到Data结束的所有数据参与计算所得。
     * 备注：整个通信数据串的字节数不得大于255个字节。
     *
     */
    public byte[] buildSendData(int cmd, byte[] data) {
        LoggerUtils.d(TAG,"组合数据-------"+DataConverter.bytesToHex(data));
        int index = 0;
        if (data != null && data.length > 0) {
            byte[] sendData = new byte[5 + data.length];
            sendData[index++] = (byte) 0xFF;
            sendData[index++] = (byte) data.length;
            sendData[index++] = (byte) cmd;
            for (int k = 0; k < data.length; k++) {
                sendData[index++] = data[k];
            }
            byte[] crc = new byte[2];
            //需要校验的数据
            byte[] check = Arrays.copyOfRange(sendData, 1, 1 + 1 + 1 + data.length);
            ModuleAPI.getInstance().CalcCRC(check, check.length, crc);
            sendData[index++] = crc[0];
            sendData[index++] = crc[1];
            LoggerUtils.d(TAG, "构建发送数据buildSendData=>" + DataConverter.bytesToHex(sendData));
            return sendData;
        } else {
            byte[] sendData = new byte[5];
            sendData[0] = (byte) 0xFF;
            sendData[1] = (byte) 0;
            sendData[2] = (byte) cmd;
            byte[] crc = new byte[2];
            ModuleAPI.getInstance().CalcCRC(Arrays.copyOfRange(sendData, 1, 3), 2, crc);//
            sendData[3] = crc[0];
            sendData[4] = crc[1];
            LoggerUtils.d(TAG, "构建发送数据buildSendData=>" + DataConverter.bytesToHex(sendData));
            return sendData;
        }

    }

    @Override
    public byte[] makeSetBaudRate(int baudrate) {
        byte[] data = new byte[4];
        data[0] = (byte) ((baudrate >> 24) & 0xFF);
        data[1] = (byte) ((baudrate >> 16) & 0xFF);
        data[2] = (byte) ((baudrate >> 8) & 0xFF);
        data[3] = (byte) (baudrate & 0xFF);
        return buildSendData(0x06, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisSetBaudRateResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    @Override
    public byte[] makeSetFrequencyPoint(int frequencyPoint) {
        byte[] data = new byte[4];
        data[0] = (byte) ((frequencyPoint >> 24) & 0xFF);
        data[1] = (byte) ((frequencyPoint >> 16) & 0xFF);
        data[2] = (byte) ((frequencyPoint >> 8) & 0xFF);
        data[3] = (byte) (frequencyPoint & 0xFF);
        return buildSendData(0x95, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisSetFrequencyPointResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }

    @Override
    public byte[] makeSetRFLink(int mode) {

        byte[] data = new byte[3];
        data[0] = 0x05;
        data[1] = 0x02;
        if (mode == 0) {
            data[2] = 0x6F;//FM0
        }
        if (mode == 1) {
            data[2] = 0x65;//M2 640k
        }
        if (mode == 2) {
            data[2] = 0x6B;//M4 250k
        }
        if (mode == 3) {
            data[2] = 0x71;//M8 160k
        }

        return buildSendData(0x9B, data);
    }

    @Override
    public UHFReaderResult<Boolean> analysisSetRFLinkResultData(DataFrameInfo data) {
        if (data != null && data.status == 0) {
            return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE);
    }
}
