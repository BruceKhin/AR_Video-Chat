package com.adnet.archat;

/**
 * Created by TanZhegui on 9/19/2016.
 */
public class NativeFunc {
    public static void loadLibrary(){
        System.loadLibrary("AR_Video_Track");
    }
    public static native void OpenCMT(long matAddrGr, float x, float y, float w, float h, float[] ptArray, int color);

    public static native void ResetCMT();

    public static native void UndoCMT();

    public static native void getFeatureMat(long grayAddress, int imgWidth, int imgHeight, int displayWidth, int displayHeight,
                                            byte[] yuvPlanes0, int yuvStrides0);

    public static native void ProcessCMT(long markMatAddr, int imgWidth, int imgHeight, int displayWidth, int displayHeight,
                                         byte[] yuvPlanes0, int yuvStrides0);
}
