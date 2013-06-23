package com.google.libvorbis;

public class VorbisEnc {
	public static final int OV_ECTL_RATEMANAGE2_GET = 0x14;
	public static final int OV_ECTL_RATEMANAGE2_SET = 0x15;
	public static final int OV_ECTL_LOWPASS_GET = 0x20;
	public static final int OV_ECTL_LOWPASS_SET = 0x21;
	public static final int OV_ECTL_IBLOCK_GET = 0x30;
	public static final int OV_ECTL_IBLOCK_SET = 0x31;
	public static final int OV_ECTL_COUPLING_GET = 0x40;
	public static final int OV_ECTL_COUPLING_SET = 0x41;
	public static final int OV_ECTL_RATEMANAGE_GET = 0x10;
	public static final int OV_ECTL_RATEMANAGE_SET = 0x11;
	public static final int OV_ECTL_RATEMANAGE_AVG = 0x12;
	public static final int OV_ECTL_RATEMANAGE_HARD = 0x13;

	public static int vorbisEncodeSetupManaged(VorbisInfo vi, long channels,
			long rate, long maxBitrate, long nominalBitrate, long minBitrate) {
		return vorbisEncodeSetupManaged(vi.getNativePointer(), channels, rate,
				maxBitrate, nominalBitrate, minBitrate);
	}

	public static int vorbisEncodeSetupInit(VorbisInfo vi) {
		return vorbisEncodeSetupInit(vi.getNativePointer());
	}

	public static int vorbisEncodeCtlSetNull(VorbisInfo vi, int number) {
		return vorbisEncodeCtlSetNull(vi.getNativePointer(), number);
	}
	public static int vorbisEncodeCtlSetInt(VorbisInfo vi, int number, int arg) {
		return vorbisEncodeCtlSetInt(vi.getNativePointer(), number, arg);
	}
	public static int vorbisEncodeCtlSetDouble(VorbisInfo vi, int number, double arg) {
		return vorbisEncodeCtlSetDouble(vi.getNativePointer(), number, arg);
	}

	private static native int vorbisEncodeSetupManaged(long vi, long channels,
			long rate, long maxBitrate, long nominalBitrate, long minBitrate);
	private static native int vorbisEncodeSetupInit(long vi);

	private static native int vorbisEncodeCtlSetNull(long jvi, int number);
	private static native int vorbisEncodeCtlSetInt(long jvi, int number, int arg);
	private static native int vorbisEncodeCtlSetDouble(long jvi, int number, double arg);
}
