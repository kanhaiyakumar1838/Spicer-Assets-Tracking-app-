#include <jni.h>
#include <ModuleAPI.h>
#include <Logger.h>
#include <SerialPort.h>
#include <jni.h>
static const char *TAG = "ModuleAPI";
//#define MSG_CRC_INIT		    0xFFFF
//#define MSG_CCITT_CRC_POLY		0x1021
//void CRC_calcCrc8(uint16_t *crcReg, uint16_t poly, uint16_t u8Data)
//{
//    uint16_t i;
//    uint16_t xorFlag;
//    uint16_t bit;
//    uint16_t dcdBitMask = 0x80;
//    for(i=0; i<8; i++)
//    {
//        xorFlag = *crcReg & 0x8000;
//        *crcReg <<= 1;
//        bit = ((u8Data & dcdBitMask) == dcdBitMask);
//        *crcReg |= bit;
//        if(xorFlag)
//        {
//            *crcReg = *crcReg ^ poly;
//        }
//        dcdBitMask >>= 1;
//    }
//}
//
//uint16_t CalcCRC(uint8_t *msgbuf,uint8_t msglen)
//{
//    uint16_t calcCrc = MSG_CRC_INIT;
//    uint8_t  i;
//    for (i = 1; i < msglen; ++i)
//        CRC_calcCrc8(&calcCrc, MSG_CCITT_CRC_POLY, msgbuf[i]);
//    return calcCrc;
//}



#define MSG_CRC_INIT		    0xFFFF
#define MSG_CCITT_CRC_POLY		0x1021
#define uint16 unsigned short
#define uint8 unsigned char

void CRC_calcCrc8(uint16 *crcReg, uint16 poly, uint16 u8Data) {
    uint16 i;
    uint16 xorFlag;
    uint16 bit;
    uint16 dcdBitMask = 0x80;
    for (i = 0; i < 8; i++) {
        xorFlag = *crcReg & 0x8000;
        *crcReg <<= 1;
        bit = ((u8Data & dcdBitMask) == dcdBitMask);
        *crcReg |= bit;
        if (xorFlag) {
            *crcReg = *crcReg ^ poly;
        }
        dcdBitMask >>= 1;
    }
}

uint16 CalcCRC(uint8 *msgbuf, uint8 msglen) {
    uint16 calcCrc = MSG_CRC_INIT;
    uint8 i;
    for (i = 0; i < msglen; ++i)
        CRC_calcCrc8(&calcCrc, MSG_CCITT_CRC_POLY, msgbuf[i]);
    return calcCrc;
}


JNIEXPORT jint JNICALL Java_com_xlzn_hcpda_ModuleAPI_SerailOpen(JNIEnv* env, jobject thiz, jstring juart,jint baudrate, jint databits,jint stopbits, jint check) {
    jboolean iscopy;
     LOGD(TAG, "Java_com_xlzn_hcpda_ModuleAPI_SerailOpen");
     const char *path_uart = (*env)->GetStringUTFChars(env, juart, &iscopy);
    int result= SerialPort_Open(path_uart,   baudrate,   databits,   stopbits,  check);
    (*env)->ReleaseStringUTFChars(env, juart, path_uart);
    return result;
    return 0;
}


JNIEXPORT jint JNICALL Java_com_xlzn_hcpda_ModuleAPI_SerailClose(JNIEnv *env, jobject thiz,int uart_fd) {
    int result= SerialPort_Close(uart_fd);
    return result;
}

JNIEXPORT jint JNICALL Java_com_xlzn_hcpda_ModuleAPI_SerailSendData(JNIEnv *env, jobject thiz, int uart_fd,jbyteArray send_data,int sendLen) {
    unsigned char uData[sendLen];
    jbyte *jpszData = (*env)->GetByteArrayElements(env, send_data, 0);
    for (int i = 0; i < sendLen; i++) {
        uData[i] = jpszData[i];
    }
    int reuslt= SerialPort_Send(uData,sendLen,uart_fd);
    (*env)->ReleaseByteArrayElements(env, send_data , jpszData, 0);
    return reuslt;
}

JNIEXPORT jint JNICALL Java_com_xlzn_hcpda_ModuleAPI_SerailReceive(JNIEnv *env, jobject thiz, jint uart_fd,jbyteArray receive_data,int receive_dataLen) {
    unsigned char uData[receive_dataLen];
    int reuslt= SerialPort_Receive(uData,receive_dataLen,uart_fd);
    if(reuslt>0){
        jbyte *jpszData = (*env)->GetByteArrayElements(env, receive_data, 0);
        for (int i = 0; i < reuslt; i++) {
            jpszData[i] = uData[i];
        }
        (*env)->ReleaseByteArrayElements(env, receive_data , jpszData, 0);
    }
    return reuslt;
}
JNIEXPORT jint JNICALL Java_com_xlzn_hcpda_ModuleAPI_CalcCRC(JNIEnv *env, jobject thiz, jbyteArray jdata, jint data_len,jbyteArray jout_crc){
    jbyte *jpszData = (*env)->GetByteArrayElements(env, jdata, 0);
    jbyte *outData = (*env)->GetByteArrayElements(env, jout_crc, 0);
    uint16_t result=CalcCRC(jpszData,data_len);
    outData[0]=result>>8;
    outData[1]=result&0xFF;
    (*env)->ReleaseByteArrayElements(env, jdata , jpszData, 0);
    (*env)->ReleaseByteArrayElements(env, jout_crc , outData, 0);
    return 0;
}



