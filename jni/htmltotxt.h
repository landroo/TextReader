#define MAX_IMG_NUM 10240

long readFile(const char *fileName, unsigned char **buffer);
int getText(const char * fileName, char **resBuff, int mode);
int toUTF8(char *dest, unsigned int ch);
unsigned int htmlDeCode(char *sCode);
char isUTF8(const unsigned char* bytes);
char* readUTF(const char* fileName);
