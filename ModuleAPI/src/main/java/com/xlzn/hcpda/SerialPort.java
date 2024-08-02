package com.xlzn.hcpda;

import com.xlzn.hcpda.utils.LoggerUtils;

import java.util.Arrays;

/**
 * 串口控制类
 */
public class SerialPort {
    String TAG = "SerialPort";
    private byte[] receiveData = new byte[1024];
    private byte zero = 0;

    //返回串口句柄
    public int open(String uart, int baudrate, int databits, int stopbits, int parity) {
        LoggerUtils.d(TAG, " uart=" + uart + " baudrate=" + baudrate + "  databits=" + databits + " stopbits=" + stopbits + " parity=" + parity);
        return ModuleAPI.getInstance().SerailOpen(uart, baudrate, databits, stopbits, parity);
    }

    public boolean close(int uart_fd) {
        LoggerUtils.d(TAG, " close uart_fd= " + uart_fd);
        ModuleAPI.getInstance().SerailClose(uart_fd);
        return true;
    }

    public boolean send(int uart_fd, byte[] data) {
        if (uart_fd < 0) {
            return false;
        }
        int reuslt = ModuleAPI.getInstance().SerailSendData(uart_fd, data, data.length);
        if (reuslt == data.length) {
            return true;
        }
        return false;
    }

    public byte[] receive(int uart_fd) {
        if (uart_fd < 0) {
            return null;
        }
        Arrays.fill(receiveData, zero);
        int len = ModuleAPI.getInstance().SerailReceive(uart_fd, receiveData, receiveData.length);
        if (len <= 0) {
            return null;
        }
        return Arrays.copyOf(receiveData, len);
    }
}
