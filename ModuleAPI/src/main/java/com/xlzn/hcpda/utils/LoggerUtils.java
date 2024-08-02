package com.xlzn.hcpda.utils;

import android.util.Log;

public class LoggerUtils {

    private static String _TAG="CHLOG";

    private static boolean debugFlag=true;
//    public static void d(String TAG,String data){
////        if(debugFlag)Log.d(_TAG,TAG+"==>data="+data);
//        print(TAG+"==>data="+data);
//    }
//    public static void d(String TAG,byte[] data){
////        if(debugFlag)Log.d(_TAG,TAG+"==>data="+DataConverter.bytesToHex(data));
//        print(TAG+"==>data="+DataConverter.bytesToHex(data));
//    }
    public static boolean isDebug(){
        return debugFlag;
    }
    public static void d(String TAG,byte[] data) {

        int index = 3;
        StringBuilder stringBuffer = new StringBuilder();
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String className = stackTrace[index].getFileName();
            String methodName = stackTrace[index].getMethodName();
            int lineNumber = stackTrace[index].getLineNumber();
            methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
            stringBuffer.append("[---(").append(className).append(":").append(lineNumber).append(")#").append(methodName).append("---] ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        String finalMsg = stringBuffer.toString()+": "+DataConverter.bytesToHex(data);
        Log.e(_TAG,  " ========> "+finalMsg);

    }
    public static void d(String TAG,String msg) {

            int index = 3;
            StringBuilder stringBuffer = new StringBuilder();
            try {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                String className = stackTrace[index].getFileName();
                String methodName = stackTrace[index].getMethodName();
                int lineNumber = stackTrace[index].getLineNumber();
                methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
                stringBuffer.append("[---(").append(className).append(":").append(lineNumber).append(")#").append(methodName).append("---] ");
            } catch (Exception e) {
                e.printStackTrace();
            }
            String finalMsg = stringBuffer.toString()+": "+msg;
            Log.e(_TAG,  " ========> "+finalMsg);

    }
}
