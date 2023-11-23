
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "htmltotxt.h"

int getText(const char * fileName, char **resBuff, int mode)
{
	unsigned char *fileBuff;
	long lSize = readFile(fileName, &fileBuff);
	if(lSize < 0) return -1;

	*resBuff = (char*)malloc(sizeof(char) * lSize);
	if(*resBuff == NULL) return -2;

	int parBuffCnt = 0;
	char in = 0;
	char enter = 0;
	char codeBuff[16];
	int numCnt;

	// <img alt="9780470584460-fg0103.tif" src="images/9780470584460-fg0103_fmt.jpeg"/>
	char imgBuff[10240];
	int imgBuffCnt = 0;
	char inImg = 0;
	char inImgSrc = 0;

	// <a href="http://www.elephantsdream.org">
	// <a class="calibre4" href="../Text/kindle_split_007.html#pref03">Acknowledgments</a>
	char lnkBuff[10240];
	int lnkBuffCnt = 0;
	char inLnk = 0;
	char inLnkOk = 0;
	char inLnkLine = 0;

	// <script></script>
	char inScript = 0;

	// <title></title>
	char inTitle = 0;

	// <head></head>
	char inHead = 0;

	// <w:drawing>
	char inDocxImg = 0;

	int iCharCnt = 0;
	char utfCnt = 0;

	char e8852 = strchr((char*)fileBuff, 0xe9) == NULL ? 0: 1;

	int i, j;
	for(i = 0; i < lSize; i++)
	{
		// xml tag start
		if(fileBuff[i] == '<')
		{
			in = 1;
			enter = 0;

			// p tag
			if((fileBuff[i + 1] == 'p' || fileBuff[i + 1] == 'P'))
			{
				enter = 1;
			}

			// br tag, li tag, hx tag, blockquote, div tag, span tag
			if((fileBuff[i + 1] == 'b' || fileBuff[i + 1] == 'B' && fileBuff[i + 2] == 'r' || fileBuff[i + 2] == 'R')
			|| (fileBuff[i + 1] == 'l' || fileBuff[i + 1] == 'L' && fileBuff[i + 2] == 'i' || fileBuff[i + 2] == 'I')
			|| (fileBuff[i + 1] == 'h' || fileBuff[i + 1] == 'H' && fileBuff[i + 2] == 'x' || fileBuff[i + 2] == 'X')
			|| (fileBuff[i + 1] == 'b' || fileBuff[i + 1] == 'B' && fileBuff[i + 2] == 'l' || fileBuff[i + 2] == 'L')
			|| (fileBuff[i + 1] == 'd' || fileBuff[i + 1] == 'D' && fileBuff[i + 2] == 'i' || fileBuff[i + 2] == 'I')
			|| (fileBuff[i + 1] == 's' || fileBuff[i + 1] == 'S' && fileBuff[i + 2] == 'p' || fileBuff[i + 2] == 'P'))
			{
				enter = 1;
			}

			// <img src="images/img1.png" alt="img1.png" style="width:84px;height:96px"/>
			// <image width="582" height="700" xlink:href="images/cover.png"/>
			if(strncmp((char*)(fileBuff + i + 1), "img", 3) == 0 || strncmp((char*)(fileBuff + i + 1), "IMG", 3) == 0
			|| strncmp((char*)(fileBuff + i + 1), "ima", 3) == 0 || strncmp((char*)(fileBuff + i + 1), "IMA", 3) == 0)
			{
				inImg = 1;
				sprintf(codeBuff, "%d", iCharCnt);
				strcpy(imgBuff + imgBuffCnt, codeBuff);
				imgBuffCnt += strlen(codeBuff);
				imgBuff[imgBuffCnt++] = '\t';
			}

			// docx drawing tag
			if(strncmp((char*)(fileBuff + i + 1), "w:drawing", 9) == 0)
			{
				inDocxImg = 1;
				sprintf(codeBuff, "%d", iCharCnt);
				strcpy(imgBuff + imgBuffCnt, codeBuff);
				imgBuffCnt += strlen(codeBuff);
				imgBuff[imgBuffCnt++] = '\t';
			}
			// docx image id tag
			if(strncmp((char*)(fileBuff + i + 1), "a:blip", 6) == 0)
			{
				inDocxImg = 0;
				char* cnt = (char*)(fileBuff + i + 6);
				while(*cnt != '>')
				{
					if(strncmp(cnt, "embed", 5) == 0)
					{
						while(*cnt != '"')
						{
							cnt++;
						}
						cnt++;
						while(*cnt != '"')
						{
							imgBuff[imgBuffCnt++] = *cnt;
							cnt++;
						}
						imgBuff[imgBuffCnt++] = '\n';
					}
					cnt++;
				}
			}

			// a tag
			if((fileBuff[i + 1] == 'a' || fileBuff[i + 1] == 'A')
			&& (fileBuff[i + 2] == ' '))
			//&& (fileBuff[i + 3] == 'h' || fileBuff[i + 3] == 'H'))
			{
				inLnkLine = 3;
				while(strncmp((char*)(fileBuff + i + 1), "href", 4) != 0)
				{
					i++;
					if(fileBuff[i] == '>' || i > lSize)
					{
						in = 0;
						inLnkLine = 0;
						break;
					}
				}

				if(inLnkLine > 0)
				{
					inLnk = 1;
					inLnkOk = 1;

					sprintf(codeBuff, "%d", iCharCnt);
					strcpy(lnkBuff + lnkBuffCnt, codeBuff);
					lnkBuffCnt += strlen(codeBuff);
					lnkBuff[lnkBuffCnt++] = '\t';
					i += 6;
				}
			}
			
			// script tag
			if(fileBuff[i + 1] == '/'
			&& (fileBuff[i + 2] == 's' || fileBuff[i + 2] == 'S')
			&& (fileBuff[i + 3] == 'c' || fileBuff[i + 3] == 'C'))
			{
				inScript = 0;
			}
			// script tag
			if((fileBuff[i + 1] == 's' || fileBuff[i + 1] == 'S')
			&& (fileBuff[i + 2] == 'c' || fileBuff[i + 2] == 'C'))
			{
				inScript = 1;
			}

			// title tag
			if(fileBuff[i + 1] == '/'
			&& (fileBuff[i + 2] == 't' || fileBuff[i + 2] == 'T')
			&& (fileBuff[i + 3] == 'i' || fileBuff[i + 3] == 'I'))
			{
				inTitle = 0;
			}
			// title tag
			if((fileBuff[i + 1] == 't' || fileBuff[i + 1] == 'T')
			&& (fileBuff[i + 2] == 'i' || fileBuff[i + 2] == 'I'))
			{
				inTitle = 1;
			}
			
			// a tag end
			if(fileBuff[i + 1] == '/' 
			&& (fileBuff[i + 2] == 'a' || fileBuff[i + 2] == 'A')) 
			{
				inLnkLine = 0;
			}
			
			// head tag
			if(fileBuff[i + 1] == '/' 
			&& (fileBuff[i + 2] == 'h' || fileBuff[i + 2] == 'H') 
			&& (fileBuff[i + 3] == 'e' || fileBuff[i + 3] == 'E'))
			{
				inHead = 0;
			}
			// head tag
			if((fileBuff[i + 1] == 'h' || fileBuff[i + 1] == 'H') 
			&& (fileBuff[i + 2] == 'e' || fileBuff[i + 2] == 'E'))
			{
				inHead = 1;
			}

			// paragraph end tag
			if(fileBuff[i + 1] == '/'
			&& (fileBuff[i + 2] == 'p' || fileBuff[i + 2] == 'P'))
			{
				if(*((*resBuff) + parBuffCnt - 1) != '\n')
				{
					*((*resBuff) + parBuffCnt++) = '\n';
				}
			}

			// div end tag
			if(fileBuff[i + 1] == '/'
			&& (fileBuff[i + 2] == 'd' || fileBuff[i + 2] == 'D')
			&& (fileBuff[i + 3] == 'i' || fileBuff[i + 3] == 'I'))
			{
				if(*((*resBuff) + parBuffCnt - 1) != '\n')
				{
					*((*resBuff) + parBuffCnt++) = '\n';
				}
			}

			// span end tag
			if(fileBuff[i + 1] == '/'
			&& (fileBuff[i + 2] == 's' || fileBuff[i + 2] == 'S')
			&& (fileBuff[i + 3] == 'p' || fileBuff[i + 3] == 'P'))
			{
				if(*((*resBuff) + parBuffCnt - 1) != '\n')
				{
					*((*resBuff) + parBuffCnt++) = '\n';
				}
			}

			// docx paragraph end tag
			if(fileBuff[i + 1] == '/'
			&& (fileBuff[i + 2] == 'w' || fileBuff[i + 2] == 'W')
			&& (fileBuff[i + 3] == ':')
			&& (fileBuff[i + 4] == 'p' || fileBuff[i + 4] == 'P'))
			{
				if(*((*resBuff) + parBuffCnt - 1) != '\n')
				{
					*((*resBuff) + parBuffCnt++) = '\n';
				}
			}

			continue;
		}

		// tag end \> 
		if(in == 1 && fileBuff[i + 1] == '/' && fileBuff[i + 2] == '>')
		{
			if(inTitle == 1) inTitle = 0;
			if(inHead == 1) inHead = 0;
			if(inLnkLine == 1) inLnkLine = 0;
			if(inHead == 1) inHead = 0;
			if(inScript == 1) inScript = 0;
		}

		// xml tag end
		if(in == 1 && fileBuff[i] == '>')
		{
			in = 0;

			if(inImg)
			{
				inImg = 0;
				inImgSrc = 0;
				imgBuff[imgBuffCnt++] = '\n';
			}

			if(inLnk)
			{
				inLnk = 0;
				lnkBuff[lnkBuffCnt++] = '\n';
			}

			// line breake end
			if(enter && parBuffCnt > 1)
			{
				// add enter to the line end
				if(*((*resBuff) + parBuffCnt - 1) != '\n')
				{
					*((*resBuff) + parBuffCnt++) = '\n';
					//iCharCnt++;
				}
				enter = 0;
			}

			continue;
		}

		// enter, tab stb.
		if(fileBuff[i] < ' ')
		{
			continue;
		}

		// more space
		if(fileBuff[i] == ' ' && *((*resBuff) + parBuffCnt - 1) == ' ')
		{
			continue;
		}

		// start space
		if(fileBuff[i] == ' ' && *((*resBuff) + parBuffCnt - 1) == '\n')
		{
			continue;
		}

		// start space
		if(fileBuff[i] == ' ' && parBuffCnt == 0)
		{
			continue;
		}

		// html code
		if(fileBuff[i] == '&')
		{
			// html character code
			if(fileBuff[i + 1] == '#')
			{
				i++;
				numCnt = 0;
				while(fileBuff[i++] != ';')
				{
					codeBuff[numCnt++] = fileBuff[i];
				}
				codeBuff[numCnt - 1] = 0;
				parBuffCnt += toUTF8((*resBuff) + parBuffCnt, (unsigned int) atoi(codeBuff));
				iCharCnt++;
			}
			else
			{
				numCnt = 0;
				while(fileBuff[i++] != ';')
				{
					codeBuff[numCnt++] = fileBuff[i];
				}
				codeBuff[numCnt - 1] = 0;
				numCnt = htmlDeCode(codeBuff);
				if(numCnt)
				{
					parBuffCnt += toUTF8((*resBuff) + parBuffCnt, (unsigned int) numCnt);
					iCharCnt++;
				}
			}
			i--;
			continue;
		}

		// add text
		if(in == 0 && parBuffCnt < lSize && inScript == 0 && inTitle == 0 && inHead == 0)
		{
			utfCnt = isUTF8((unsigned char*)(fileBuff + i));
			if(utfCnt != 0) iCharCnt++;

			// 8859 to UTF-8 conversion
			if(fileBuff[i] < 128)
			{
				if(inLnkLine > 0 && fileBuff[i] == ' ')
				{
					*((*resBuff) + parBuffCnt++) = '-';
					inLnkLine--;
				}
				else *((*resBuff) + parBuffCnt++) = fileBuff[i];
				
			}
			else if (fileBuff[i] < 192)
			{
				if(inLnkLine > 0 && fileBuff[i] == ' ')
				{
					*((*resBuff) + parBuffCnt++) = '-';
					inLnkLine--;
				}
				else *((*resBuff) + parBuffCnt++) = fileBuff[i];
				
			}
			else
			{
				if(e8852)
				{
					*((*resBuff) + parBuffCnt++) = 0xc2 + (fileBuff[i] > 0xbf);
					*((*resBuff) + parBuffCnt++) = (fileBuff[i] & 0x3f) + 0x80;
				}
				else
				{
					if(inLnkLine > 0 && fileBuff[i] == ' ')
					{
						*((*resBuff) + parBuffCnt++) = '-';
						inLnkLine--;
					}
					else *((*resBuff) + parBuffCnt++) = fileBuff[i];
					
				}
			}
		}

		// add image
		if(inImg && imgBuffCnt < 10240)
		{
			// src ref
			if((fileBuff[i] == 's' && fileBuff[i + 1] == 'r' && fileBuff[i + 2] == 'c')
			|| (fileBuff[i] == 'S' && fileBuff[i + 1] == 'R' && fileBuff[i + 2] == 'C')
			|| (fileBuff[i] == 'r' && fileBuff[i + 1] == 'e' && fileBuff[i + 2] == 'f')
			|| (fileBuff[i] == 'R' && fileBuff[i + 1] == 'E' && fileBuff[i + 2] == 'F'))
			{
				inImgSrc = 1;
				i += 4;
			}

			// recindex
			if((fileBuff[i] == 'r' && fileBuff[i + 1] == 'e' && fileBuff[i + 2] == 'c')
			|| (fileBuff[i] == 'R' && fileBuff[i + 1] == 'E' && fileBuff[i + 2] == 'C'))
			{
				inImgSrc = 1;
				i += 9;
			}

			if(inImgSrc && fileBuff[i] != '"' && fileBuff[i + 1] != '>')
			{
				imgBuff[imgBuffCnt++] = fileBuff[i];
			}

			if(fileBuff[i] == '"')
			{
				if(inImgSrc == 1)
				{
					inImgSrc = 2;
				}
				else
				{
					inImgSrc = 0;
				}
			}
		}

		// add link
		if(inLnk && fileBuff[i] != '"' && lnkBuffCnt < 10240 && inLnkOk)
		{
			if(fileBuff[i] == '#')
			{
				inLnkOk = 0;
			}
			else
			{
				lnkBuff[lnkBuffCnt++] = fileBuff[i];
			}
		}
	}

	*((*resBuff) + parBuffCnt++) = '\n';
	*((*resBuff) + parBuffCnt++) = '\r';

	imgBuff[imgBuffCnt] = 0;
	strcpy((*resBuff) + parBuffCnt, imgBuff);
	parBuffCnt += strlen(imgBuff);

	*((*resBuff) + parBuffCnt++) = '\r';

	lnkBuff[lnkBuffCnt] = 0;
	strcpy((*resBuff) + parBuffCnt, lnkBuff);
	parBuffCnt += strlen(lnkBuff);

	free(fileBuff);

	return parBuffCnt;
}

// Load files
long readFile(const char *fileName, unsigned char **buffer)
{
	FILE *pFile = NULL;
	long lSize = 0;
	size_t tSize;

	pFile = fopen(fileName, "r+b");
	if(pFile == NULL) return -1;

	fseek(pFile, 0, SEEK_END);
	lSize = ftell(pFile);
	rewind(pFile);

	*buffer = (unsigned char*)malloc(sizeof(char) * lSize);
	if(*buffer == NULL) return -2;

	tSize = fread(*buffer, sizeof(char), lSize, pFile);
	if(tSize != lSize){free(*buffer); lSize = -3;}

	fclose(pFile);

	return lSize;
}

// int to utf chars
int toUTF8(char *dest, unsigned int ch)
{
	// 0xxxxxxx
    if (ch < 0x80)
	{
        dest[0] = (char)ch;

        return 1;
    }
	// 110yyyyy 10xxxxxx
    if (ch < 0x800)
	{
        dest[0] = (ch >> 6) | 0xC0;
        dest[1] = (ch & 0x3F) | 0x80;

        return 2;
    }
	// 1110zzzz 10yyyyyy 10xxxxxx
    if (ch < 0x10000)
	{
        dest[0] = (ch >> 12) | 0xE0;
        dest[1] = ((ch >> 6) & 0x3F) | 0x80;
        dest[2] = (ch & 0x3F) | 0x80;

        return 3;
    }
	// 11110www 10zzzzzz 10yyyyyy 10xxxxxx
    if (ch < 0x110000)
	{
        dest[0] = (ch >> 18) | 0xF0;
        dest[1] = ((ch >> 12) & 0x3F) | 0x80;
        dest[2] = ((ch >> 6) & 0x3F) | 0x80;
        dest[3] = (ch & 0x3F) | 0x80;

        return 4;
    }

    return 0;
}

//	√Å  &#193;  &Aacute;          √°  &#225;  &aacute;
//	√â  &#201;  &Eacute;          √©  &#233;  &eacute;
//	√ç  &#205;  &Iacute;          √≠  &#237;  &iacute;
//	√ì  &#211;  &Oacute;          √≥  &#243;  &oacute;
//	√ï  &#213;  &Ocirc;           √µ  &#245;  &ocirc;
//	√ñ  &#214;  &Ouml;            √∂  &#246;  &ouml;
//	√ö  &#218;  &Uacute;          √∫  &#250;  &uacute;
//	√õ  &#219;  &Ucirc;           √ª  &#251;  &ucirc;
//	√ú  &#220;  &Uuml;            √º  &#252;  &uuml;
//  <  &#60;   &lt;  >  &#62;   &gt;  &  &#38;   &amp; &nbsp; &#160;
unsigned int htmlDeCode(char* sCode)
{
	if(strcmp(sCode, "Aacute") == 0) return 193;
	if(strcmp(sCode, "aacute") == 0) return 225;
	if(strcmp(sCode, "Eacute") == 0) return 201;
	if(strcmp(sCode, "eacute") == 0) return 233;
	if(strcmp(sCode, "Iacute") == 0) return 205;
	if(strcmp(sCode, "iacute") == 0) return 237;
	if(strcmp(sCode, "Oacute") == 0) return 211;
	if(strcmp(sCode, "oacute") == 0) return 243;
	if(strcmp(sCode, "Ocirc") == 0) return 213;
	if(strcmp(sCode, "ocirc") == 0) return 245;
	if(strcmp(sCode, "Ouml") == 0) return 214;
	if(strcmp(sCode, "ouml") == 0) return 246;
	if(strcmp(sCode, "Uacute") == 0) return 218;
	if(strcmp(sCode, "uacute") == 0) return 250;
	if(strcmp(sCode, "Ucirc") == 0) return 219;
	if(strcmp(sCode, "ucirc") == 0) return 251;
	if(strcmp(sCode, "Uuml") == 0) return 220;
	if(strcmp(sCode, "uuml") == 0) return 252;
	if(strcmp(sCode, "lt") == 0) return 60;
	if(strcmp(sCode, "gt") == 0) return 62;
	if(strcmp(sCode, "amp") == 0) return 38;
	if(strcmp(sCode, "nbsp") == 0) return 160;

	return 0;
}

char isUTF8(const unsigned char* bytes)
{
    if(!bytes)
	{
        return 0;
	}

	char cRes = 0;

	// ASCII
    if(bytes[0] == 0x09 || bytes[0] == 0x0A || bytes[0] == 0x0D || (0x20 <= bytes[0] && bytes[0] <= 0x7E))
	{
        cRes = 1;
    }

	// non-overlong 2-byte
    if((0xC2 <= bytes[0] && bytes[0] <= 0xDF) && (0x80 <= bytes[1] && bytes[1] <= 0xBF))
	{
        cRes = 2;
    }

	// excluding overlongs
    if((bytes[0] == 0xE0 && (0xA0 <= bytes[1] && bytes[1] <= 0xBF) && (0x80 <= bytes[2] && bytes[2] <= 0xBF))
	// straight 3-byte
	|| (((0xE1 <= bytes[0] && bytes[0] <= 0xEC) || bytes[0] == 0xEE || bytes[0] == 0xEF) && (0x80 <= bytes[1] && bytes[1] <= 0xBF) && (0x80 <= bytes[2] && bytes[2] <= 0xBF))
	// excluding surrogates
	|| (bytes[0] == 0xED && (0x80 <= bytes[1] && bytes[1] <= 0x9F) && (0x80 <= bytes[2] && bytes[2] <= 0xBF)))
	{
            cRes = 3;
    }

	// planes 1-3
    if((bytes[0] == 0xF0 && (0x90 <= bytes[1] && bytes[1] <= 0xBF) && (0x80 <= bytes[2] && bytes[2] <= 0xBF) && (0x80 <= bytes[3] && bytes[3] <= 0xBF))
	// planes 4-15
	|| ((0xF1 <= bytes[0] && bytes[0] <= 0xF3) && (0x80 <= bytes[1] && bytes[1] <= 0xBF) && (0x80 <= bytes[2] && bytes[2] <= 0xBF) && (0x80 <= bytes[3] && bytes[3] <= 0xBF))
	// plane 16
	|| (bytes[0] == 0xF4 && (0x80 <= bytes[1] && bytes[1] <= 0x8F) && (0x80 <= bytes[2] && bytes[2] <= 0xBF) && (0x80 <= bytes[3] && bytes[3] <= 0xBF)))
	{
            cRes = 4;
    }

    return cRes;
}

//
char* readUTF(const char* fileName)
{
	unsigned char *text;
	long lSize = readFile(fileName, &text);
	if(lSize < 0) return NULL;

	char *res = (char*)malloc(sizeof(char) * lSize);
	if(res == NULL) return NULL;
	memset(res, 0, lSize);

	int j = 0;
	int i = 0;
	for(i = 0; i < lSize; i++)
	{
		if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0xB6){res[j++] = 'ˆ'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0x96){res[j++] = '÷'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0xBC){res[j++] = '¸'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0x9C){res[j++] = '‹'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0xB3){res[j++] = 'Û'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0x93){res[j++] = '”'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x85 && text[i + 2] == 0x91){res[j++] = 'ı'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x85 && text[i + 2] == 0x90){res[j++] = '’'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0xBA){res[j++] = '˙'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0x9A){res[j++] = '⁄'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0xA9){res[j++] = 'È'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0x89){res[j++] = '…'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0xA1){res[j++] = '·'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0x81){res[j++] = '¡'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x85 && text[i + 2] == 0xB1){res[j++] = '˚'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x85 && text[i + 2] == 0xB0){res[j++] = '€'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0xAD){res[j++] = 'Ì'; i += 2;}
		else if(text[i] == 0xC3 && text[i + 1] == 0x83 && text[i + 2] == 0x8D){res[j++] = 'Õ'; i += 2;}
		else res[j++] = text[i];
	}

	free(text);
		
	return res;
}
