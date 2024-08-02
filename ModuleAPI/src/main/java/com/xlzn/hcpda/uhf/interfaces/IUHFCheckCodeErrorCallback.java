package com.xlzn.hcpda.uhf.interfaces;

public interface IUHFCheckCodeErrorCallback {
    /**
     *校验码出错
     * mode 校验模式（0：crc 目前只有这一种）
     * cmd 命令字
     * errorData 错误的数据帧
     */
    public void checkCodeError(int mode,int cmd,byte[] errorData);
}
