package com.adnet.archat.QuickSample.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.adnet.archat.NativeFunc;
import com.quickblox.videochat.webrtc.QBRTCClient;
import com.quickblox.videochat.webrtc.view.QBRTCSurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by TanZhegui on 9/22/2016.
 */
public class OpenCV_RTCVideoView extends QBRTCSurfaceView {

    private boolean inited;

    public interface bitmapListner{
        void getBitmapFromVideoRenderGUI(Bitmap bmp);
        void onGetRemoteMatData(Mat mat, float x, float y, float width, float height, float[] ptArray, int color);
    }
    public static bitmapListner myBmpListener;
    public static ArrayList<Rect> arrayTrackBox = new ArrayList<org.opencv.core.Rect>();
    public static ArrayList<Point> freeDrawPoint = new ArrayList<Point>();
    public static final int VIEW_MODE_CMT = 8;
    public static final int START_CMT = 7;
    public static int mViewMode = START_CMT;
    public static boolean isResetCMT = false;
    public static int drawColor = Color.GREEN;

    public static void initListener(bitmapListner listner){
        myBmpListener = listner;
    }

    public OpenCV_RTCVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenCV_RTCVideoView(Context context) {
        super(context);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        this.init();
    }

    protected void init() {
        if(!this.inited) {
            EglBase eglContext = QBRTCClient.getInstance(this.getContext()).getEglContext();
            if(eglContext != null) {
                this.init(eglContext.getEglBaseContext(), (RendererCommon.RendererEvents)null);
                this.inited = true;
            }
        }
    }

    public byte[] getActiveArray(ByteBuffer buffer)
    {
        byte[] ret = new byte[buffer.remaining()];
        if (buffer.hasArray())
        {
            byte[] array = buffer.array();
            System.arraycopy(array, buffer.arrayOffset() + buffer.position(), ret, 0, ret.length);
        }
        else
        {
            buffer.slice().get(ret);
        }
        return ret;
    }

    public void renderFrame(VideoRenderer.I420Frame frame){
        byte[] buf = new byte[frame.width * frame.height];
        if(!frame.yuvFrame) {
            return;
        }else{
//            for(int i = 0; i < buf.length; i++){
//                buf[i] = frame.yuvPlanes[0].get(i);
//            }

            if(frame.yuvPlanes != null)
                drawRegions(getActiveArray(frame.yuvPlanes[0]), frame.yuvStrides[0], frame.width, frame.height);
        }
        super.renderFrame(frame);
    }

    private void drawRegions(byte[] yuv0, int strides0, int frameWidth, int frameHeight){
        if(myBmpListener != null) {
            if(isResetCMT){
                freeDrawPoint = new ArrayList<Point>();
                arrayTrackBox = new ArrayList<Rect>();
                NativeFunc.ResetCMT();
                isResetCMT = false;
                myBmpListener.getBitmapFromVideoRenderGUI(null);
            }else{
                switch (mViewMode) {
                    case START_CMT: {
                        if (arrayTrackBox.size() != 0){
                            //
                            Rect rt = arrayTrackBox.get(arrayTrackBox.size() - 1);
                            float[] ptArray = new float[freeDrawPoint.size() * 2];
                            for(int i = 0; i < freeDrawPoint.size(); i++){
                                ptArray[i * 2] = (float)(freeDrawPoint.get(i).x);
                                ptArray[i * 2 + 1] = (float)(freeDrawPoint.get(i).y);
                            }
                            Mat grayMat = new Mat();

                            NativeFunc.getFeatureMat(grayMat.getNativeObjAddr(), frameWidth, frameHeight,
                                    getMeasuredWidth(), getMeasuredHeight(),
                                    yuv0,
                                    strides0);
                            if(grayMat.width() > 0) {
                                myBmpListener.onGetRemoteMatData(grayMat, (float)rt.x,
                                        (float)rt.y,
                                        (float)rt.width,
                                        (float)rt.height, ptArray,
                                        drawColor);
                            }
                        }
                        mViewMode = VIEW_MODE_CMT;
                    }
                    break;
                    case VIEW_MODE_CMT:{
                        Mat markMat = new Mat();
                        NativeFunc.ProcessCMT(markMat.getNativeObjAddr(), frameWidth, frameHeight,
                                getMeasuredWidth(), getMeasuredHeight(),
                                yuv0,
                                strides0);
                        if(markMat.width() > 0){
                            Bitmap bmp = Bitmap.createBitmap(markMat.width(), markMat.height(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(markMat, bmp);
                            myBmpListener.getBitmapFromVideoRenderGUI(bmp);
                        }else{
                            myBmpListener.getBitmapFromVideoRenderGUI(null);
                        }
                    }
                    break;
                }
            }

        }

    }
}
