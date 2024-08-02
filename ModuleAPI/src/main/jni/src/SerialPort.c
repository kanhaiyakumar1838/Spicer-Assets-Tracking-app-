#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>
#include <errno.h>
#include "SerialPort.h"
#include <Logger.h>

static const char *TAG = "ModuleAPI";
speed_t getBaudrate(int baudrate) {
	switch (baudrate) {
		case 300:
			return B300;
		case 600:
			return B600;
		case 1200:
			return B1200;
		case 2400:
			return B2400;
		case 4800:
			return B4800;
		case 9600:
			return B9600;
		case 19200:
			return B19200;
		case 38400:
			return B38400;
		case 57600:
			return B57600;
		case 115200:
			return B115200;
		case 230400:
			return B230400;
		case 921600:
			return B921600;
		default:
			return -1;
	}
}



int SerialPort_Open(const char *uart, int baudrate, int databits, int stopbits,int parity) {
	speed_t speed;
	int fd = -1;
	speed = getBaudrate(baudrate);
	if (speed == -1)
	{
		LOGE(TAG, "This baudrate is not support.........");
		return -1;
	}
	fd = open(uart, O_RDWR | O_NOCTTY | O_NDELAY);
	if (fd == -1)
	{
		LOGE(TAG, "SerialPort_Open fail  UART: %s  [%d]: %s", uart,errno, strerror(errno));
		return -1;
	}
	/* Configure device */
	{
		struct termios cfg;
		if (tcgetattr(fd, &cfg))
		{
			LOGE(TAG, "SerialPort_Open  fail");
			close(fd);
			return -1;
		}
		cfmakeraw(&cfg);
		cfsetispeed(&cfg, speed);
		cfsetospeed(&cfg, speed);
		if (tcsetattr(fd, TCSANOW, &cfg))
		{
			LOGE(TAG, "SerialPort_Open  fail1");
			close(fd);
			return -1;
		}
		if (tcgetattr(fd, &cfg))
		{
			LOGE(TAG, "SerialPort_Open  fail2");
			close(fd);
			return -1;
		}
		cfg.c_cflag &= ~CSIZE;
		switch (databits)
		{
			case 7:
				cfg.c_cflag |= CS7;
				break;
			case 8:
				cfg.c_cflag |= CS8;
				break;
			default:
				break;
		}
		switch (parity)
		{
			case 0:
				cfg.c_cflag &= ~PARENB;
				cfg.c_iflag &= ~INPCK;
				break;
			case 1:
				cfg.c_cflag |= (PARODD | PARENB);
				cfg.c_iflag |= INPCK;
				break;
			case 2:
				cfg.c_cflag |= PARENB;
				cfg.c_cflag &= ~PARODD;
				cfg.c_iflag |= INPCK;
				break;
			default:
				break;
		}
		switch (stopbits)
		{
			case 1:
				cfg.c_cflag &= ~CSTOPB;
				break;
			case 2:
				cfg.c_cflag |= CSTOPB;
				break;
			default:
				break;
		}

		if (tcsetattr(fd, TCSANOW, &cfg))
		{
			LOGE(TAG, "SerialPort_Open  fail3");
			close(fd);
			return -1;
		}
		if (tcgetattr(fd, &cfg))
		{
			LOGE(TAG, "SerialPort_Open  fail4");
			close(fd);
			return -1;
		}
		if((cfg.c_cflag  & PARENB) !=0)
		{

		}
		else
		{

		}
		if((cfg.c_cflag  & PARODD) ==0)
		{

		}
		else
		{

		}
	}
	return fd;
}

int SerialPort_Close(int uart_fd) {
	if (uart_fd == -1) {
		return -1;
	}
	return close(uart_fd);
}
int SerialPort_Send(const UINT8 *pszData, int iLength, int uart_fd) {
	int iRes = 0,j;
	if (uart_fd == -1) {
		return 0;
	}
	iRes = write(uart_fd, pszData, iLength * sizeof(char));
	return iRes;
}

int SerialPort_Receive(UINT8 *pszBuffer, int iLength, int uart_fd) {
	int iRes = 0;
	if (uart_fd == -1) {
		return -1;
	}
	iRes = read(uart_fd, pszBuffer, iLength);
	return iRes;
}

