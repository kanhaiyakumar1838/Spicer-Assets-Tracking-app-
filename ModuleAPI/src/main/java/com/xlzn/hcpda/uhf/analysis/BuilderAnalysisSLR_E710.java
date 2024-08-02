package com.xlzn.hcpda.uhf.analysis;

import android.util.Log;

import com.xlzn.hcpda.uhf.entity.SelectEntity;
import com.xlzn.hcpda.uhf.entity.UHFReaderResult;
import com.xlzn.hcpda.uhf.entity.UHFTagEntity;
import com.xlzn.hcpda.utils.DataConverter;
import com.xlzn.hcpda.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuilderAnalysisSLR_E710 extends BuilderAnalysisSLR {

    private String TAG = "BuilderAnalysisSLR_E710";

    /**
     * 解析接收的发送快速模式连续盘点的指令AA48 带TID的
     *
     * @param data
     * @param isTID
     * @return return
     */
    @Override
    public UHFReaderResult<Boolean> analysisStartFastModeInventoryReceiveDataNeedTid(UHFProtocolAnalysisBase.DataFrameInfo data, boolean isTID) {
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
     * 获取发送盘点的指令（快速模式）AA58  大量标签读取不支持嵌入TID 只支持fastid
     *
     * @return return
     */
    public byte[] makeStartFastModeInventorySendData(SelectEntity selectEntity,boolean isTID) {

        if(selectEntity==null) {
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
            senddata[index++] = (byte) 0x58;

            for(int k=0;k<20;k++){
                senddata[index++] = (byte) 0x00;
            }

            //************data*******************
            //2字节METADATAFLAG
            final int count = 0X0001;//Bit0置位即标签在盘存时间内被盘存到的次数将会返回
            final int rssi = 0x0002;//BIT1置位即标签的RSSI信号值将会被返回
            final int ant = 0X0004;//BIT2置位即标签 被盘存到时所用的天线 ID号将会被返回。（逻辑天线号）
            final int tagData=0x0080;//返回嵌入命令内存数据
//            final int flag = count | rssi | ant | tagData;
            final int flag = rssi | ant ;
            senddata[index++] = (flag >> 8) & 0xFF;
            senddata[index++] = (byte) (flag & 0xFF);
            //1字节OPTION
            senddata[index++] = 0x00;//不启用匹配过滤
            //2字节SEARCHFLAGS,SEARCHFLAGS高字节的低4位表示不停止盘存过程中的停顿时间dd
            //0x10:每工作1秒中盘存时间950毫秒，停顿时间50毫秒
            //0x20:停顿时间100毫秒,0x30:停顿150，0x00:不停顿
            senddata[index++] = (0x10);
            //senddata[index++] = 0x04;//todo 0x00
            if(!isTID) {
                LoggerUtils.d(TAG,"不需要TID");
                senddata[index++] = 0x00;
            }else {
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
        senddata[index++] = (byte) 0x58;

        for(int k=0;k<20;k++){
            senddata[index++] = (byte) 0x00;
        }
        //************data*******************
        //2字节METADATAFLAG
        final int count = 0X0001;//Bit0置位即标签在盘存时间内被盘存到的次数将会返回
        final int rssi = 0x0002;//BIT1置位即标签的RSSI信号值将会被返回
        final int ant = 0X0004;//BIT2置位即标签 被盘存到时所用的天线 ID号将会被返回。（逻辑天线号）
        //TODO 需要增加获取嵌入式命令数据, 要不然analysisFastModeTagInfoReceiveData 函数在解析数据的时候，下面这行会获取错误数据导致数组内存溢出
        //TODO int tidLen= ((taginfo[++statIndex] & 0xFF) << 8) | (taginfo[++statIndex] & 0xFF);//嵌入命令的数据
        final int tagData=0x0080;//返回嵌入命令内存数据
        final int flag = count | rssi | ant |tagData;
        senddata[index++] = (flag >> 8) & 0xFF;
        senddata[index++] = (byte)(flag & 0xFF);
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
        int address=selectEntity.getAddress();
        senddata[index++]=(byte)((address>>24) &0xFF);
        senddata[index++]=(byte)((address>>16) &0xFF);
        senddata[index++]=(byte)((address>>8) &0xFF);
        senddata[index++]=(byte)(address&0xFF);
        //Select data length(bits)
        senddata[index++]=(byte)selectEntity.getLength();
        //Select data
        byte[] byteData=DataConverter.hexToBytes(selectEntity.getData());
        int len=selectEntity.getLength()/8;
        if(selectEntity.getLength()%8!=0) {
            len += 1;
        }
        for(int k=0;k<len;k++){
            senddata[index++]=byteData[k];
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
     * 解析接收的开始盘点的指令（快速模式）AA58 的
     *
     * @return return
     */
    public UHFReaderResult<Boolean> analysisStartFastModeInventoryReceiveData(UHFProtocolAnalysisBase.DataFrameInfo data, boolean isTID) {
        this.isTID = isTID;
        //0xFF+DATALEN+0XAA+STATUS +”Moduletech”+SubCmdHighByte+SubCmdLowByte+data+CRC
        if (data != null) {
            if (data.status == 00) {
                LoggerUtils.d(TAG, "E710开始盘点指令返回Data:" + DataConverter.bytesToHex(data.data));
                if ((data.data[10] & 0xFF) == 0xAA && (data.data[11] & 0xFF) == 0x58) {
                    return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_SUCCESS, "", true);
                }
                //4D 6F 64 75 6C 65 74 65 63 68 AA 48
            }
        }
        return new UHFReaderResult<Boolean>(UHFReaderResult.ResultCode.CODE_FAILURE, "", false);
    }


    /**
     * 解析连续盘点数据-之前的快速模式  AA48
     *
     * @return return
     */

    public List<UHFTagEntity> analysisFastModeTagInfoReceiveDataOld(UHFProtocolAnalysisBase.DataFrameInfo data) {
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

    /**
     * 获取发送盘点的指令（快速模式）
     *
     * @return return
     */
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


}
