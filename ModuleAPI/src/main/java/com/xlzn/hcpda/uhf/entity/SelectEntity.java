package com.xlzn.hcpda.uhf.entity;

/**
 * 选择匹配指定标签
 */
public class SelectEntity {

    public static final int OPTION_EPC=4;
    public static final int OPTION_TID=2;
    public static final int OPTION_USER=3;

    //单位：bit
    private int address;
    //单位：bit
    private int  length;
    private String data;
    //2:tid  3:user  4:epc
    private int option;


    public int getAddress() {
        return address;
    }

    /**
     * 设置指定标签的起始地址
     * @param address 起始地址,单位bit
     */
    public void setAddress(int address) {
        this.address = address;
    }

    public int getLength() {
        return length;
    }

    /**
     * 设置指定标签的长度
     * @param length 长度,单位bit
     */
    public void setLength(int length) {
        this.length = length;
    }

    public String getData() {
        return data;
    }
    /**
     * 设置指定标签的数据
     * @param data 十六进制数据
     */
    public void setData(String data) {
        this.data = data;
    }

    public int getOption() {
        return option;
    }
    /**
     * 设置指定标签的区域
     * @param option EPC区域、TID区域、USER区域
     *  @see #OPTION_EPC
     *  @see #OPTION_TID
     *  @see #OPTION_USER
     */
    public void setOption(int option) {
        this.option = option;
    }

}
