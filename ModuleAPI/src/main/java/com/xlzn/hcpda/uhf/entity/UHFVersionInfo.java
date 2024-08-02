package com.xlzn.hcpda.uhf.entity;

/**
 *
 * UHF 固件版本、硬件版本
 */
public class UHFVersionInfo {
    private String   firmwareVersion;
    private String   hardwareVersion;

    /**
     * 获取UHF硬件版本
     * @return 硬件版本号
     */
    public String getHardwareVersion() {
        return hardwareVersion;
    }
    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }
    /**
     * 获取UHF软件版本
     * @return 软件版本号
     */
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }



}
