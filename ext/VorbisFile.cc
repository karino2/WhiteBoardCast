// Author: hogeika2@gmail.com

#include <jni.h>

#include "common.h"
#include "ogg/ogg.h"
#include "vorbis/codec.h"
#include "vorbis/vorbisfile.h"

#define FUNCTION(returnType, functionName, ...) \
    FUNC(returnType, VorbisFile, functionName, ##__VA_ARGS__)

static OggVorbis_File gvf;
static int g_current_selection;


FUNCTION(jint, Open, jstring path) {
  jint ret = 0;
  const char* charPath = env->GetStringUTFChars(path, 0);
  ret = ov_fopen(charPath, &gvf);
  env->ReleaseStringUTFChars(path, charPath);
  return ret;
}


FUNCTION(jlong, Read, jbyteArray pcmArray) {
  jboolean isCopy;
  jint length = env->GetArrayLength(pcmArray);
  jbyte* rawjBytes = env->GetByteArrayElements(pcmArray, &isCopy);
  long ret;
  ret = ov_read(&gvf, (char*)rawjBytes, length, 0, 2, 1, &g_current_selection);
  env->ReleaseByteArrayElements(pcmArray, rawjBytes, 0);
  return ret;
}

FUNCTION(void, Clear) {
  ov_clear(&gvf);
}


