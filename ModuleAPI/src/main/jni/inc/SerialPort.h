#ifndef SERIALPORT_H
#define SERIALPORT_H
#include <termios.h>

#define UINT8 		unsigned char

speed_t getBaudrate(int baudrate);



int SerialPort_Open(const char *uart, int baudrate, int databits, int stopbits,int parity) ;
int SerialPort_Close(int uart_fd);
int SerialPort_Send(const UINT8 *pszData, int iLength, int uart_fd);
int SerialPort_Receive(UINT8 *pszBuffer, int iLength, int uart_fd) ;


#endif
