package com.xlzn.hcpda.uhf.enums;

/**
 * 锁标区域签枚举
 */
public enum LockMembankEnum {
    /**
     * 销毁密码区
     */
    KillPwd(0),
    /**
     * 访问密码区
     */
    AccessPwd(1),
    /**
     * EPC区域
     */
    EPC(2),
    /**
     *TID区域
     */
    TID(3),
    /**
     * 用户区域
     */
    USER(4);
    int lock=0;
    private LockMembankEnum(int lock) {
        this.lock=lock;
    }

    public int getValue(){
        return lock;
    }

    public static LockMembankEnum getValue(int value) {
        LockMembankEnum lockActionEnum = null;
        switch (value) {
            case 0:
                lockActionEnum = KillPwd;
                break;
            case 1:
                lockActionEnum = AccessPwd;
                break;
            case 2:
                lockActionEnum = EPC;
                break;
            case 3:
                lockActionEnum = TID;
                break;
            case 4:
                lockActionEnum = USER;
                break;
        }
        return lockActionEnum;
    }
}
