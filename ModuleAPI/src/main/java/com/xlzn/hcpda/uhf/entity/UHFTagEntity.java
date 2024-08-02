package com.xlzn.hcpda.uhf.entity;

/**
 *
 * UHF 标签实体类
 */
public class UHFTagEntity {
    private String tidHex;
    private String ecpHex;
    private String pcHex;
    private int rssi;
    private int ant;
    private int count;
    private String machineName; // Machine name (additional property)
    private String operation;   // Operation (additional property)
    private String cellName; // New field
    private String assetCode; // New field
    private String scannedTime;
    /**
     * 获取EPC数据
     * @return 十六字节EPC数据
     */
    public String getEcpHex() {
        return ecpHex;
    }

    public void setEcpHex(String ecpHex) {
        this.ecpHex = ecpHex;
    }
    /**
     * 获取PC数据
     * @return 十六字节PC数据
     */
    public String getPcHex() {
        return pcHex;
    }

    public void setPcHex(String pcHex) {
        this.pcHex = pcHex;
    }
    /**
     * 获取RSSI数据
     * @return RSSI数据
     */
    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
    /**
     * 获取ANT数据
     * @return ANT数据
     */
    public int getAnt() {
        return ant;
    }

    public void setAnt(int ant) {
        this.ant = ant;
    }
    /**
     * 获取标签次数数据
     * @return 标签次数数据
     */
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
    /**
     * 获取标签TID数据
     * @return 十六字节TID数据
     */
    public String getTidHex() {
        return tidHex;
    }

    public void setTidHex(String tidHex) {
        this.tidHex = tidHex;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
    public String getCellName() {
        return cellName;
    }

    public void setCellName(String cellName) {
        this.cellName = cellName;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }
    public String getScannedTime() {
        return scannedTime;
    }

    public void setScannedTime(String scannedTime) {
        this.scannedTime = scannedTime;
    }
}
