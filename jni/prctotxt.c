//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "prctotxt.h"

short inshort(FILE* pFile)
{
    unsigned char b[2];

    fread(b, 2, 1, pFile);
    return (b[0] << 8) | b[1];
}

long inlong(FILE* pFile)
{
    unsigned char b[4];

    fread(b, 4, 1, pFile);
    return (b[0] << 24) | (b[1] << 16) | (b[2] << 8) | b[3];
}

// http://wiki.mobileread.com/wiki/MOBI
int loadPrc(const char* sFileName, const char* sFolder)
{
	FILE *inFile;					// PDB input file
	struct prcHead head;			// PRC header
	short nrecords;					// Number of records
	int isResource;					// Nonzero if this is a resource file
	struct prcResEnt *rsp = NULL;	// Resource entry array
	struct prcRecEnt *rcp = NULL;	// Record entry array
	long fileLength;				// Total length of file
	long *lengths;					// Computed lengths of records
	
	int i;
	long l;
	
	FILE *outFile;
	char fname[256];
	unsigned char* data;
	int bText = 1;
	int num = 1;
	long len = 0;
	long lid = 0;
	
	// open prc
	inFile = fopen(sFileName, "r+b");
	if(inFile == NULL) return -1;

	// get file length
	fseek(inFile, 0, SEEK_END);
	fileLength = ftell(inFile);
	fseek(inFile, 0, SEEK_SET);

	// load file header
	fread(&head.name, 1, 32, inFile);
	head.flags = inshort(inFile);
	head.version = inshort(inFile);
	head.create_time = inlong(inFile);
	head.mod_time = inlong(inFile);
	head.backup_time = inlong(inFile);
	head.mod_num = inlong(inFile);
	head.app_info = inlong(inFile);
	head.sort_info = inlong(inFile);
	fread(&head.type, 1, 4, inFile);
	fread(&head.id, 1, 4, inFile);
	head.unique_id_seed = inlong(inFile);
	head.next_record_list = inlong(inFile);
	head.num_records = inshort(inFile);

	// set record num
	nrecords = head.num_records;

	// set file type
	isResource = !!(head.flags & pdbResourceFlag);

	// record length list
	lengths = (long*)malloc(sizeof(long) * nrecords);
	if(lengths == NULL) return -2;
	
    if(isResource) 
	{
		rsp = (struct prcResEnt *) malloc(sizeof(struct prcResEnt) * nrecords);
		if(rsp == NULL) 
		{
			free(lengths);
			return -3;
		}

		for(i = 0; i < nrecords; i++) 
		{
			rsp[i].type = inlong(inFile);
			rsp[i].id = inshort(inFile);
			rsp[i].offset = inlong(inFile);
			if(i > 0) 
			{
				lengths[i - 1] = rsp[i].offset - rsp[i - 1].offset;
				//printf("Record %d offset: %d ID: %d, length: %d\n", i, rsp[i].offset, rsp[i].id, lengths[i - 1]);
			}
		}

		if(nrecords > 0) 
		{
			lengths[nrecords - 1] = fileLength - rsp[nrecords - 1].offset;
		}
    } 
	else 
	{
		rcp = (struct prcRecEnt*) malloc(sizeof(struct prcRecEnt) * nrecords);
		if(rcp == NULL) 
		{
			free(lengths);
			return -4;
		}

		for(i = 0; i < nrecords; i++) 
		{
			rcp[i].offset = inlong(inFile);
			l = inlong(inFile);
			rcp[i].attr = (l >> 24) & 0xFF;
			rcp[i].uniqueID = l & 0xFFFFFF;
			if(i > 0) 
			{
				lengths[i - 1] = rcp[i].offset - rcp[i - 1].offset;
				//printf("Record %d offset: %d ID: %d, length: %d\n", i, rcp[i].offset, rcp[i].uniqueID, lengths[i - 1]);
			}
		}

		if(nrecords > 0) 
		{
			lengths[nrecords - 1] = fileLength - rcp[nrecords - 1].offset;
		}
	}
	
	for(i = 1; i < nrecords; i++) 
	{
		l = lengths[i];
		if(rcp != NULL)
		{
			fseek(inFile, rcp[i].offset, SEEK_SET);
			lid = rcp[i].uniqueID;
		}
		if(rsp != NULL)
		{
			fseek(inFile, rsp[i].offset, SEEK_SET);
			lid = rsp[i].id;
		}
		
		data = (unsigned char*)malloc(sizeof(char) * l);
		if(data == NULL) return -5;
		fread(data, sizeof(char), l, inFile);

		if(bText == 1)
		{
			len = decompress(data, l);
			sprintf(fname, "%s/index.html", sFolder);
			outFile = fopen(fname, "a+");
			if(outFile == NULL) return -6;
			fwrite(decomp, sizeof(char), len, outFile);
			fclose(outFile);

			if(strstr((char*)(decomp + len - 20), "</html>") != NULL || strstr((char*)(decomp + len - 20), "</HTML>") != NULL)
			{
				bText = 0;
			}
		}
		else
		{
			if(l < 1024)
			{
				sprintf(fname, "%s/%05d", sFolder, lid);
			}
			else
			{
				sprintf(fname, "%s/%05d", sFolder, num++);
			}
			outFile = fopen(fname, "w+b");
			if(outFile == NULL) return -7;
			fwrite(data, sizeof(char), l, outFile);
			fclose(outFile);
		}

		free(data);
	}

	fclose(inFile);

	return 0;
}

int get_byte_type(byte b) 
{  
	if(b <= 0xbf && b >= 0x80)
	{  
		return LZ_LEN_DIS_PAIR;  
    }
	else if(b >= 0xc0)
	{  
		return LZ_BYTE_PAIR;  
	}
	else if(b >= 0x01 && b <= 0x08) 
	{  
		return LZ_LITERAL_COUNT;  
	}
	else
	{  
		return LZ_LITERAL;  
	}  
}  

// http://wiki.mobileread.com/wiki/PalmDOC
long decompress(byte* comp, int size)
{  
	memset(decomp, 0, DATA_LENGTH);  
      
	int src = 0;
	int des = 0;  
	int dis = 0;  
	int len = 0;
	int i;
	
	while(src < size) 
	{  
		switch(get_byte_type(comp[src]))
		{  
			case LZ_LITERAL:
				decomp[des] = comp[src];  
				break;  

			case LZ_LEN_DIS_PAIR:
				len = (comp[src + 1] & 0x7) + 3;
				dis = ((comp[src] & 0x3f) << 5) | ((comp[src + 1] & 0xf8) >> 3);
				if(len > dis)
				{
					for(i = 0; i < len; i++)
					{
						decomp[des + i] = decomp[des - dis + i];
					}
				}
				else
				{
					memcpy(decomp + des, decomp + des - dis, len);
				}
				des += len - 1;
				src++;  
                break;  
  
            case LZ_BYTE_PAIR:  
				decomp[des] = ' ';  
				des++;  
				decomp[des] = comp[src] ^ 0x80;  
                break;  

            case LZ_LITERAL_COUNT:  
				len = comp[src];
				memcpy(decomp + des, comp + src + 1, len);  
				src += len;  
				des += len - 1;  
                break;  

            default:  
                break;  
        }  

		src++;
		des++;
    }  

	return strlen((char*)decomp);
}  
