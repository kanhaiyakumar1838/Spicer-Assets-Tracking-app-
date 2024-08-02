//
// Created by admin on 2022/4/3.
//
#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "android/log.h"
#include <sys/time.h>



void LOGI(const char* tag, const char* log, ...) {
    va_list arg;
    va_start(arg, log);
    __android_log_vprint(ANDROID_LOG_INFO, tag, log, arg);
    va_end(arg);
}

void LOGD(const char* tag, const char* log, ...) {
    va_list arg;
    va_start(arg, log);
    __android_log_vprint(ANDROID_LOG_DEBUG, tag, log, arg);
    va_end(arg);
}

void LOGE(const char* tag, const char* log, ...) {
    va_list arg;
    va_start(arg, log);
    __android_log_vprint(ANDROID_LOG_ERROR, tag, log, arg);
    va_end(arg);
}



