package com.xlzn.hcpda;

import android.os.Build;

import com.xlzn.hcpda.utils.LoggerUtils;

public class DeviceConfigManage {
    private String TAG = "DeviceConfigManage";
    private final String Device_HC720S = "HC720S";
    private final String Device_HC710S = "HC710S";
    private final String Device_HC706S = "HC706";
    private final String Device_HC605S = "HC605S";
    private static DeviceConfigManage deviceConfigInfo = new DeviceConfigManage();
    public static String SLR1200 = "R2000";
    public static String SLRE710 = "E710";
    public static String SLR5300 = "R5300";
    public static String module_type = "R2000";
    private UHFConfig uhfConfig = null;

    public enum Platform {
        MTK,
        QUALCOMM
    }

    //设备型号
    private String model;
    //平台 高通、MTK
    private Platform platform;
    //uhf串口号
    private String uhfUart;

    public static DeviceConfigManage getInstance() {
        return deviceConfigInfo;
    }

    private DeviceConfigManage() {
//        model="HC720S";// Build.MODEL;
        model = Build.MODEL;// Build.MODEL;
        LoggerUtils.d(TAG, " model=" + model + " Build.DISPLAY=" + Build.DISPLAY);
//        if (model.equals(Device_HC720S)) {
//            LoggerUtils.d(TAG, " 设置uhf串口720s");
//            uhfUart = "/dev/ttysWK0";
//        }
//        if (model.equals(Device_HC710S) || model.equals(Device_HC605S)) {
//            LoggerUtils.d(TAG, " 设置uhf串口710s 或 HC605S");
//            uhfUart = "/dev/ttysWK0";
//        }
//        if (model.equals(Device_HC706S)) {
//            LoggerUtils.d(TAG, " 设置uhf串口706s");
//            uhfUart = "/dev/ttysWK0";
//        }
//        if (uhfUart == null) {
//            uhfUart = "/dev/ttysWK0";
//            LoggerUtils.d(TAG, " 未知设备，设置WK0");
//        }
        uhfUart = "/dev/ttysWK0";
//        LoggerUtils.d(TAG, " API 版本" + BuildConfig.API_VERSION);
        LoggerUtils.d(TAG, " 获取最终串口" + uhfUart);
    }

    private static class ConfigBase {
        //设备型号
        public String model;
        //平台 高通、MTK
        public Platform platform;

        public String getModel() {
            return model;
        }

        private void setModel(String model) {
            this.model = model;
        }

        public Platform getPlatform() {
            return platform;
        }

        private void setPlatform(Platform platform) {
            this.platform = platform;
        }
    }

    public static class UHFConfig extends ConfigBase {
        //uhf串口号
        private String uhfUart;

        public String getUhfUart() {
            return uhfUart;
        }

        public void setUhfUart(String uhfUart) {
            this.uhfUart = uhfUart;
        }
    }

    public UHFConfig getUhfConfig() {
        if (uhfConfig == null) {
            uhfConfig = new UHFConfig();
            uhfConfig.uhfUart = uhfUart;
            uhfConfig.model = model;
            uhfConfig.platform = platform;
        }
        return uhfConfig;
    }
}
