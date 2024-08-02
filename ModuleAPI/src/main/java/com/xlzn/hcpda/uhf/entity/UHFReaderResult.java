package com.xlzn.hcpda.uhf.entity;

/**
 * UHF操作返回的结果
 * @param <T>
 */
public class UHFReaderResult<T> {

    /**
     * 结果码类
     */
    public class ResultCode{
        /*
         * 成功
         * */
        public static final int CODE_SUCCESS=0;
        /*
         *  失败
         * */
        public static final int CODE_FAILURE=1;
        /*
         *  未连接读写器
         * */
        public static final int CODE_READER_NOT_CONNECTED=2;
        /*
         *  打开串口失败
         * */
        public static final int CODE_OPEN_SERIAL_PORT_FAILURE=3;
        /*
         *  上电失败
         * */
        public static final int CODE_POWER_ON_FAILURE=4;

    }

    /**
     * 返回的消息类
     */
    public class ResultMessage{
        //uhf读写器没有连接.
        public static final String READER_NOT_CONNECTED="没有连接UHF模块.";
        public static final String OPEN_SERIAL_PORT_FAILURE="打开串口失败.";
        public static final String CODE_POWER_ON_FAILURE="模块上电失败!";
    }
    public UHFReaderResult(int resultCode){
        this.resultCode=resultCode;
    }
    public UHFReaderResult(int resultCode,String message){
        this.resultCode=resultCode;
        this.message=message;
    }
    public UHFReaderResult(int resultCode,String msg,T data){
        this.resultCode=resultCode;
        this.message=message;
        this.data=data;
    }
    /*
     * 返回结果
     * */
    private int resultCode;
    /*
    * 返回结果
    * */
    private T data;
    /*
     * 消息
     * */
    private String message;

    /**
     * 获取结果码
     *
     * @return uhf操作返回的结果码，参考ResultCode里面常量值
     *
     * @see ResultCode
     */
    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    /**
     * 获取结果消息
     *
     * @return 结果消息
     *
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    /**
     * 获取结果数据
     *
     * @return 结果数据
     *
     */
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
