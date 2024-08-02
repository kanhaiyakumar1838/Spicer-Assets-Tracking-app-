//
// Created by admin on 2022/4/3.
//
#include <stdint.h>

#ifndef API_LOGGER_H
#define API_LOGGER_H


extern void LOGI(const char* tag, const char* log, ...);

extern void LOGD(const char* tag, const char* log, ...);

extern void LOGE(const char* tag, const char* log, ...);

#endif
