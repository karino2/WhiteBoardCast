// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
#include <assert.h>
#include <jni.h>
#include <string.h>

#ifdef HAVE_LIBYUV
#include <cstdlib>
#include "libyuv.h"
#endif

#include "vpx/vpx_encoder.h"
#include "vpx/vp8cx.h"
#include "vpx_ports/mem_ops.h"

#ifdef NDEBUG
# define printf(fmt, ...)
#else
# ifdef __ANDROID__
#  include <android/log.h>
#  define printf(fmt, ...) \
   __android_log_print(ANDROID_LOG_DEBUG, "LIBVPX_ENC", fmt, ##__VA_ARGS__)
# else
#  define printf(fmt, ...) \
   printf(fmt "\n", ##__VA_ARGS__)
# endif
#endif

#ifdef HAVE_LIBYUV
#define align_buffer_64(var, size) \
  uint8* var; \
  uint8* var##_mem; \
  var##_mem = reinterpret_cast<uint8*>(malloc((size) + 63)); \
  var = reinterpret_cast<uint8*> \
        ((reinterpret_cast<intptr_t>(var##_mem) + 63) & ~63);

#define free_aligned_buffer_64(var) \
  free(var##_mem);  \
  var = 0;
#endif

#define FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE Java_com_google_libvpx_LibVpxEnc_ ## NAME \
                      (JNIEnv * env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE Java_com_google_libvpx_LibVpxEnc_ ## NAME \
                      (JNIEnv * env, jobject thiz, ##__VA_ARGS__)\

#define STRING_RETURN(JNI_NAME, LIBVPX_NAME) \
  FUNC(jstring, JNI_NAME) { \
    printf(#JNI_NAME); \
    return env->NewStringUTF(LIBVPX_NAME()); \
  }

static const struct codec_item {
  const char              *name;
  const vpx_codec_iface_t *iface;
  unsigned int             fourcc;
} codecs[] = {
  {"vp8",  &vpx_codec_vp8_cx_algo, 0x30385056},
};

#define SET_ENC_CTL_PARAM(JNI_NAME, CTL_NAME, TYPE) \
  FUNC(int, vpxCodecEncCtlSet ##JNI_NAME, jlong jctx, jint jparam) { \
    printf("vpxCodecEncCtlSet" #JNI_NAME); \
    printf("Setting control parameter " #CTL_NAME " to %d", jparam); \
    vpx_codec_ctx_t *ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx); \
    return vpx_codec_control(ctx, VP8E_SET_ ##CTL_NAME, (TYPE)jparam); \
  }

SET_ENC_CTL_PARAM(CpuUsed, CPUUSED, int)
SET_ENC_CTL_PARAM(EnableAutoAltRef, ENABLEAUTOALTREF, unsigned int)
SET_ENC_CTL_PARAM(NoiseSensitivity, NOISE_SENSITIVITY, unsigned int)
SET_ENC_CTL_PARAM(Sharpness, SHARPNESS, unsigned int)
SET_ENC_CTL_PARAM(StaticThreshold, STATIC_THRESHOLD, unsigned int)
SET_ENC_CTL_PARAM(TokenPartitions, TOKEN_PARTITIONS, vp8e_token_partitions)
SET_ENC_CTL_PARAM(ARNRMaxFrames, ARNR_MAXFRAMES, unsigned int)
SET_ENC_CTL_PARAM(ARNRStrength, ARNR_STRENGTH, unsigned int)
SET_ENC_CTL_PARAM(ARNRType, ARNR_TYPE, unsigned int)
SET_ENC_CTL_PARAM(Tuning, TUNING, vp8e_tuning)
SET_ENC_CTL_PARAM(CQLevel, CQ_LEVEL, unsigned int)
SET_ENC_CTL_PARAM(MaxIntraBitratePct, MAX_INTRA_BITRATE_PCT, unsigned int)

FUNC(void, vpxCodecEncInit, jlong jctx, jlong jcfg) {
  printf("vpxCodecEncInit");
  vpx_codec_ctx_t *ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);
  vpx_codec_enc_cfg_t *cfg = reinterpret_cast<vpx_codec_enc_cfg_t *>(jcfg);
  const struct codec_item *codec = codecs;

  vpx_codec_enc_init(ctx, codec->iface, cfg, 0);
}

FUNC(jboolean, vpxCodecEncode, jlong jctx, jbyteArray jframe,
                               jint fmt, jlong pts, jlong duration,
                               jlong flags, jlong deadline) {
  printf("vpxCodecEncode");
  jboolean success = true;
  jbyte *frame = env->GetByteArrayElements(jframe, 0);
  vpx_codec_ctx_t *ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);

  vpx_image_t *img = vpx_img_wrap(NULL,
                                  (vpx_img_fmt)fmt,
                                  ctx->config.enc->g_w,
                                  ctx->config.enc->g_h,
                                  0,
                                  reinterpret_cast<unsigned char *>(frame));

  if (img) {
    vpx_codec_encode(ctx, img, pts, duration, flags, deadline);
    vpx_img_free(img);
  } else {
    success = false;
  }

  env->ReleaseByteArrayElements(jframe, frame, 0);

  return success;
}

#ifdef HAVE_LIBYUV
FUNC(jboolean, vpxCodecHaveLibyuv) {
  return true;
}

static uint8* g_yuvBuf = NULL;
static uint8* g_yuvBufAligned = NULL;
static vpx_image_t *g_img = NULL;
static void ensureYuvBufInit(vpx_codec_ctx_t *ctx, int size) {
   if(g_yuvBuf != NULL)
	return;
  g_yuvBuf = reinterpret_cast<uint8*>(malloc(size+63));
  g_yuvBufAligned = reinterpret_cast<uint8*> ((reinterpret_cast<intptr_t>(g_yuvBuf) + 63) & ~63);

}

static vpx_image_t *my_img_wrap(vpx_codec_ctx_t *ctx, uint8* dstY) {
  if(g_img == NULL) {
    g_img = (vpx_image_t *)malloc(sizeof(vpx_image_t));
    vpx_image_t *img = vpx_img_wrap(NULL,
                                    VPX_IMG_FMT_I420,
                                    ctx->config.enc->g_w,
                                    ctx->config.enc->g_h,
                                    0,
                                    dstY);
    memcpy(g_img, img, sizeof(vpx_image_t));
  }
  g_img->img_data = dstY; 
  return g_img;
}

void yuvBufFinalize() {
  if(g_yuvBuf != NULL)
    free(g_yuvBuf);
  if(g_img != NULL)
    free(g_img);
  g_yuvBuf = NULL;
  g_img = NULL;
}


bool convertEncodeRegion(vpx_codec_ctx_t *ctx, const uint8 *frame,
		int invalX, int invalY, int invalWidth, int invalHeight, 
		   int64_t pts, long duration, long flags, long deadline, long fourcc,
                   int size) {
  printf("convertEncodeRegion");
  bool success = true;

  const int width = ctx->config.enc->g_w;
  const int height = ctx->config.enc->g_h;
  const int dst_y_stride = (width + 1) & ~1;
  const int dst_uv_stride = (width + 1) / 2;
  const int dst_uv_size = dst_uv_stride * ((height + 1) / 2);
  const int alignedInvalWidth = (invalWidth+15) & ~15;
  const int alignedInvalHeight = (invalHeight+15) & ~15;
  const int alignedInvalX = invalX & ~15;
  const int alignedInvalY = invalY & ~15;
/*
  const int alignedInvalX = invalX & ~1;
  const int alignedInvalY = invalY & ~1;
*/

  ensureYuvBufInit(ctx, (dst_y_stride * height) + (2 * dst_uv_size));
  uint8 *dst_y = g_yuvBufAligned;
  // align_buffer_64(dst_y, (dst_y_stride * height) + (2 * dst_uv_size));
  uint8 *dst_u = dst_y + (dst_y_stride * height);
  uint8 *dst_v = dst_u + dst_uv_size;

  uint8 *dst_y_withOffset = dst_y + invalY*dst_y_stride + invalX;
  uint8 *dst_u_withOffset = dst_u + dst_uv_stride*(invalY+1)/2 + (invalX+1)/2;
  uint8 *dst_v_withOffset = dst_v + dst_uv_stride*(invalY+1)/2 + (invalX+1)/2;

  if(invalWidth !=0 && invalHeight != 0) {

     int rv = libyuv::ConvertToI420(frame, size,
                                 dst_y_withOffset, dst_y_stride,
                                 dst_u_withOffset, dst_uv_stride,
                                 dst_v_withOffset, dst_uv_stride,
                                 invalX, invalY,
                                 // 0, 0,
                                 // alignedInvalX, alignedInvalY,
                                 width, height,
				invalWidth, invalHeight,
                                 // alignedInvalWidth, alignedInvalHeight,
                                 // dst_y_stride, height,
                                 libyuv::kRotate0, fourcc);
     if (rv != 0)
        success = false;
   }

  if (success) {
    vpx_image_t *img = my_img_wrap(ctx, dst_y);

    if (img) {
      vpx_codec_encode(ctx, img, pts, duration, flags, deadline);
    } else {
      success = false;
    }
  }

  // free_aligned_buffer_64(dst_y);

  return success;
}

bool convertEncode(vpx_codec_ctx_t *ctx, const uint8 *frame, int64_t pts,
                   long duration, long flags, long deadline, long fourcc,
                   int size) {
  printf("convertEncode");
  bool success = true;

  const int width = ctx->config.enc->g_w;
  const int height = ctx->config.enc->g_h;
  const int dst_y_stride = (width + 1) & ~1;
  const int dst_uv_stride = (width + 1) / 2;
  const int dst_uv_size = dst_uv_stride * ((height + 1) / 2);

  align_buffer_64(dst_y, (dst_y_stride * height) + (2 * dst_uv_size));
  uint8 *dst_u = dst_y + (dst_y_stride * height);
  uint8 *dst_v = dst_u + dst_uv_size;

  int rv = libyuv::ConvertToI420(frame, size,
                                 dst_y, dst_y_stride,
                                 dst_u, dst_uv_stride,
                                 dst_v, dst_uv_stride,
                                 0, 0,
                                 width, height,
                                 dst_y_stride, height,
                                 libyuv::kRotate0, fourcc);
  if (rv != 0)
    success = false;

  if (success) {
    vpx_image_t *img = vpx_img_wrap(NULL,
                                    VPX_IMG_FMT_I420,
                                    ctx->config.enc->g_w,
                                    ctx->config.enc->g_h,
                                    0,
                                    dst_y);

    if (img) {
      vpx_codec_encode(ctx, img, pts, duration, flags, deadline);
      vpx_img_free(img);
    } else {
      success = false;
    }
  }

  free_aligned_buffer_64(dst_y);
  return success;
}

FUNC(jboolean, vpxCodecConvertByteEncode, jlong jctx, jbyteArray jframe,
                                          jlong pts, jlong duration,
                                          jlong flags, jlong deadline,
                                          jlong fourcc, jint size) {
  printf("vpxCodecConvertByteEncode");

  vpx_codec_ctx_t *const ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);
  jbyte *frame = env->GetByteArrayElements(jframe, 0);

  jboolean success = convertEncode(ctx, reinterpret_cast<uint8 *>(frame), pts,
                                   duration, flags, deadline, fourcc, size);
  env->ReleaseByteArrayElements(jframe, frame, 0);
  return success;
}

FUNC(jboolean, vpxCodecConvertIntEncode, jlong jctx, jintArray jframe,
                                         jlong pts, jlong duration,
                                         jlong flags, jlong deadline,
                                         jlong fourcc, jint size) {
  printf("vpxCodecConvertIntEncode");

  vpx_codec_ctx_t *const ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);
  jint *frame = env->GetIntArrayElements(jframe, 0);

  jboolean success = convertEncode(ctx, reinterpret_cast<uint8 *>(frame), pts,
                                   duration, flags, deadline, fourcc, size);
  env->ReleaseIntArrayElements(jframe, frame, 0);
  return success;
}

FUNC(jboolean, vpxCodecConvertIntEncodeRegion, jlong jctx, jintArray jframe,
					jint x, jint y, jint w, jint h, 
                                         jlong pts, jlong duration,
                                         jlong flags, jlong deadline,
                                         jlong fourcc, jint size) {
  printf("vpxCodecConvertIntEncodeRegion");

  vpx_codec_ctx_t *const ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);
  jint *frame = env->GetIntArrayElements(jframe, 0);

  jboolean success = convertEncodeRegion(ctx, reinterpret_cast<uint8 *>(frame), 
				x, y, w, h, 
				pts, duration, flags, deadline, fourcc, size);
  env->ReleaseIntArrayElements(jframe, frame, 0);
  return success;
}
#else
FUNC(jboolean, vpxCodecHaveLibyuv) {
  return false;
}

FUNC(jboolean, vpxCodecConvertByteEncode, jlong jctx, jbyteArray jframe,
                                          jlong pts, jlong duration,
                                          jlong flags, jlong deadline,
                                          jlong fourcc, jint size) {
  printf("vpxCodecConvertEncode byte Stub");
  return false;
}

FUNC(jboolean, vpxCodecConvertIntEncode, jlong jctx, jintArray jframe,
                                         jlong pts, jlong duration,
                                         jlong flags, jlong deadline,
                                         jlong fourcc, jint size) {
  printf("vpxCodecConvertEncode int Stub");
  return false;
}
#endif

FUNC(jobject, vpxCodecEncGetCxData, jlong jctx) {
  printf("vpxCodecEncGetCxData");
  const vpx_codec_cx_pkt_t *pkt;
  vpx_codec_iter_t iter = NULL;
  vpx_codec_ctx_t *ctx = reinterpret_cast<vpx_codec_ctx_t *>(jctx);

  jclass arrayListClass = env->FindClass("java/util/ArrayList");
  assert(arrayListClass != NULL);

  jmethodID alInitMethodId = env->GetMethodID(arrayListClass, "<init>", "()V");
  assert(alInitMethodId != NULL);

  jobject arrayList = env->NewObject(arrayListClass, alInitMethodId);

  jmethodID alAddMethodId = env->GetMethodID(arrayListClass,
                                             "add", "(Ljava/lang/Object;)Z");
  assert(alAddMethodId != NULL);

  jclass codecCxPkt = env->FindClass("com/google/libvpx/VpxCodecCxPkt");
  assert(codecCxPkt != NULL);

  jmethodID cxInitMethodId = env->GetMethodID(codecCxPkt, "<init>", "(J)V");
  assert(cxInitMethodId != NULL);

  jfieldID bufferId = env->GetFieldID(codecCxPkt, "buffer", "[B");
  assert(bufferId != NULL);

  jfieldID ptsId = env->GetFieldID(codecCxPkt, "pts", "J");
  assert(ptsId != NULL);

  jfieldID durationId = env->GetFieldID(codecCxPkt, "duration", "J");
  assert(durationId != NULL);

  jfieldID flagsId = env->GetFieldID(codecCxPkt, "flags", "I");
  assert(flagsId != NULL);

  jfieldID partitionId = env->GetFieldID(codecCxPkt, "partitionId", "I");
  assert(partitionId != NULL);

  while ((pkt = vpx_codec_get_cx_data(ctx, &iter))) {
    printf("vpxCodecEncGetCxData : Data found!");
    if (pkt->kind == VPX_CODEC_CX_FRAME_PKT) {
      jobject cxPkt = env->NewObject(codecCxPkt,
                                     cxInitMethodId,
                                     (jlong)pkt->data.frame.sz);

      env->SetLongField(cxPkt, ptsId, pkt->data.frame.pts);
      env->SetLongField(cxPkt, durationId, pkt->data.frame.duration);
      env->SetIntField(cxPkt, flagsId, pkt->data.frame.flags);
      env->SetIntField(cxPkt, partitionId, pkt->data.frame.partition_id);

      jobject jba = env->GetObjectField(cxPkt, bufferId);
      assert(jba != NULL);

      env->SetByteArrayRegion((jbyteArray)jba, 0, pkt->data.frame.sz,
                              reinterpret_cast<jbyte *>(pkt->data.frame.buf));

      env->CallBooleanMethod(arrayList, alAddMethodId, cxPkt);
      env->DeleteLocalRef(cxPkt);
    }
  }

  return arrayList;
}
