package com.xlzn.hcpda;

public class ModuleAPI {


    private static ModuleAPI moduleAPI=new ModuleAPI();
    private ModuleAPI(){}

    public static ModuleAPI getInstance(){
        return moduleAPI;
    }
    static {
        System.loadLibrary("ModuleAPI");
    }
    public native int SerailOpen(String uart, int baudrate, int databits, int stopbits, int parity);
    public native int SerailClose(int uart_fd);
    public native int SerailSendData(int uart_fd,byte[] sendData,int sendLen);
    public native int SerailReceive(int uart_fd,byte[] receiveData,int receiveDataLen);
    public native int CalcCRC(byte[] data,int dataLen,byte[] outCrc);

    public static int getVersionCode = BuildConfig.API_VERSION;



}
