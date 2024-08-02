package com.xlzn.hcpda.utils;

public class DataConverter {

    public static String bytesToHex(byte[] b) {
        if (b == null || b.length == 0)
            return null;
        StringBuilder sb = new StringBuilder();

        try {
            for (int i = 0; i < b.length; i++) {
                String hex = Integer.toHexString(b[i] & 0xFF);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                sb.append(hex.toUpperCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String s) {
        if(s==null || s.isEmpty()){
            return null;
        }
        byte[] bytes;
        bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

}
