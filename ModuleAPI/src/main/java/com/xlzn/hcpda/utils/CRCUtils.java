package com.xlzn.hcpda.utils;

public class CRCUtils {
        /**
         * 一个字节包含位的数量 8
         */
        private static final int BITS_OF_BYTE = 8;
        /**
         * 多项式
         */
        private static final int POLYNOMIAL = 0x1021;
        /**
         * 初始值
         */
        private static final int INITIAL_VALUE = 0xFFFF;

        /**
         * CRC16 编码
         *
         * @param bytes 编码内容
         * @return 编码结果
         */
        public static int crc16(byte[] bytes) {
            int res = INITIAL_VALUE;
            for (byte data : bytes) {
                res = res ^ (data&0xFF);
                for (int i = 0; i < BITS_OF_BYTE; i++) {
                    res = (res & 0x0001) == 1 ? (res >> 1) ^ POLYNOMIAL : res >> 1;
                }
            }
            return revert(res);
        }

        public static byte[] crc16Bytes(byte[] bytes) {
            int res = INITIAL_VALUE;
            for (byte data : bytes) {
                res = res ^ (data&0xFF);
                for (int i = 0; i < BITS_OF_BYTE; i++) {
                    res = (res & 0x0001) == 1 ? (res >> 1) ^ POLYNOMIAL : res >> 1;
                }
            }
            int r= revert(res);
            byte[] result=new byte[2];
            result[0]= (byte)((r>>8)&0xFF);
            result[1]= (byte)(r&0xFF);
            return result;
        }
        /**
         * 翻转16位的高八位和低八位字节
         *
         * @param src 翻转数字
         * @return 翻转结果
         */
        private static int revert(int src) {
            int lowByte = (src & 0xFF00) >> 8;
            int highByte = (src & 0x00FF) << 8;
            return lowByte | highByte;
        }
    }



