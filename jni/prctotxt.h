//
typedef unsigned char byte; 
#define DATA_LENGTH 4096

byte decomp[DATA_LENGTH];

int loadPrc(const char* sFileName, const char* sFolder);
long decompress(byte* compressed_data, int len);

enum 
{  
	LZ_LITERAL = 0,  
	LZ_LEN_DIS_PAIR,  
	LZ_BYTE_PAIR,  
	LZ_LITERAL_COUNT   
}; 

enum PDBFlags 
{
	pdbResourceFlag = 0x0001,          // Is this a resource file ?
	pdbReadOnlyFlag = 0x0002,          // Is database read only ?
	pdbAppInfoDirtyFlag = 0x0004,      // Is application info block dirty ?
	pdbBackupFlag = 0x0008,            // Back up to PC if no conduit defined
	pdbOKToInstallNewer = 0x0010,      // OK to install a newer version if current database open
	pdbResetAfterInstall = 0x0020,     // Must reset machine after installation
	pdbStream = 0x0080,                // Used for file streaming
	pdbOpenFlag = 0x8000               // Not closed properly
};

typedef struct prcHead
{
	char name[32];
	unsigned short flags;
	unsigned short version;
	unsigned long create_time;
	unsigned long mod_time;
	unsigned long backup_time;
	unsigned long mod_num;
	unsigned long app_info;
	unsigned long sort_info;
	char type[4];
	char id[4];
	unsigned long unique_id_seed;
	unsigned long next_record_list;
	unsigned short num_records;
}prcHead;

typedef struct prcResEnt
{
	unsigned long type;
	unsigned short id;
	unsigned long offset;
}prcResEnt;

typedef struct prcRecEnt
{
	unsigned long offset;
	unsigned char attr;
	unsigned long uniqueID;
}prcRecEnt;
