//
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>

#include "htmltotxt.h"
#include "prctotxt.h"

// adb push /Work/Java/android/DictMaker/enghun.dat /data/data/org.landroo.enghunbig/cache/enghun.dat
// Java org.landroo.textreader TextLink htmToTxt
jbyteArray  Java_org_landroo_textreader_TextLink_htmToTxt(JNIEnv* env, jobject thiz, jstring path, int mode)
{
	jboolean iscopy;
    const char *cPath = (*env)->GetStringUTFChars(env, path, &iscopy);

	jbyteArray jb = NULL;
	char *resBuff = NULL;
	long lSize = 0;
	char cBuff[32];
	
	if(mode == 10)
	{
		resBuff = readUTF(cPath);
		if(resBuff != NULL)
		{
			lSize = strlen(resBuff);
			jb = (*env)->NewByteArray(env, lSize);
			(*env)->SetByteArrayRegion(env, jb, 0, lSize, (jbyte *)resBuff);
		}
	}
	else
	{
		lSize = getText(cPath, &resBuff, mode);

		if(lSize > 0)
		{
			jb = (*env)->NewByteArray(env, lSize);
			(*env)->SetByteArrayRegion(env, jb, 0, lSize, (jbyte *)resBuff);
		}
		else
		{
			sprintf(cBuff, "No result:%d", lSize);
			lSize = strlen(cBuff) + 1;
			resBuff = (char*)malloc(sizeof(char) * lSize);
			strcpy(resBuff, cBuff);
			jb = (*env)->NewByteArray(env, lSize - 1);
			(*env)->SetByteArrayRegion(env, jb, 0, lSize - 1, (jbyte *)resBuff);
		}
	}
	
    (*env)->ReleaseStringUTFChars(env, path, cPath);

    free(resBuff);

    return jb;
}

// Java org.landroo.textreader TextLink prcToTxt
jbyteArray  Java_org_landroo_textreader_TextLink_prcToTxt(JNIEnv* env, jobject thiz, jstring inFile, jstring outPath)
{
	jboolean iscopy;
    const char *inPrc = (*env)->GetStringUTFChars(env, inFile, &iscopy);
	const char *outFolder = (*env)->GetStringUTFChars(env, outPath, &iscopy);

	jbyteArray jb = NULL;
	char *resBuff = NULL;
	long lSize = 0;
	char cBuff[32];

	lSize = loadPrc(inPrc, outFolder);
	sprintf(cBuff, "Result:%d", lSize);
	lSize = strlen(cBuff) + 1;
	resBuff = (char*)malloc(sizeof(char) * lSize);
	strcpy(resBuff, cBuff);
	jb = (*env)->NewByteArray(env, lSize - 1);
	(*env)->SetByteArrayRegion(env, jb, 0, lSize - 1, (jbyte *)resBuff);

    (*env)->ReleaseStringUTFChars(env, inFile, inPrc);
	(*env)->ReleaseStringUTFChars(env, outPath, outFolder);

    free(resBuff);

    return jb;
}

