package com.xlzn.hcpda.uhf.interfaces;

import com.xlzn.hcpda.uhf.analysis.UHFProtocolAnalysisBase;

public interface IUHFProtocolAnalysis {
    /**
     *解析数据
     */
    public void analysis(byte[] data);
    /**
     *校验码出错
     */
    public void setCheckCodeErrorCallback(IUHFCheckCodeErrorCallback iuhfCheckCodeErrorCallback);
    /**
     *获取标签信息
     */
    public UHFProtocolAnalysisBase.DataFrameInfo getTagInfo();
    /**
     *获取其他信息
     */
    public UHFProtocolAnalysisBase.DataFrameInfo getOtherInfo(int cmd, int timeOut);

    public void cleanTagInfo();
}
