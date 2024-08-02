package com.xlzn.hcpda.uhf.analysis;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class UHFProtocolAnalysisBase {
    public LinkedBlockingQueue<DataFrameInfo>  queueTaginfo=new LinkedBlockingQueue<>(2000);
    public List<DataFrameInfo> listCmd=new ArrayList<>();
    //数据帧信息
    public static class DataFrameInfo{
        //命令字,一个字节
        public int command;
        //状态，两个字节
        public int status;
        //数据
        public byte[] data;
        //时间
        public long time;
    }

    public DataFrameInfo getTagInfo(){
       return queueTaginfo.poll();
    }
    public void cleanTagInfo() {
        queueTaginfo.clear();
    }



}
