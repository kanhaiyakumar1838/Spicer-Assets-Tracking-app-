package com.xlzn.hcpda.uhf.enums;

/**
 *  Session 枚举
 */
public enum UHFSession {

    S0(0),
    S1(1),
    S2(2),
    S3(3);
    int session=0;
    private UHFSession(int session) {
        this.session=session;
    }

    public int getValue(){
        return session;
    }
    public static UHFSession getValue(int value) {
        UHFSession session = null;
        switch (value) {
            case 0:
                session = S0;
                break;
            case 1:
                session = S1;
                break;
            case 2:
                session = S2;
                break;
            case 3:
                session = S3;
                break;
        }
        return session;
    }
}
