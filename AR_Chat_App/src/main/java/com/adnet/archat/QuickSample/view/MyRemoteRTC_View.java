package com.adnet.archat.QuickSample.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.adnet.archat.NativeFunc;
import com.quickblox.videochat.webrtc.QBRTCClient;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.GlUtil;
import org.webrtc.Logging;
import org.webrtc.RendererCommon;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Created by TanZhegui on 9/22/2016.
 */
public class MyRemoteRTC_View extends SurfaceView implements SurfaceHolder.Callback, VideoRenderer.Callbacks {
    private static final String TAG = "MySurfaceViewRenderer";
    private HandlerThread renderThread;
    private final Object handlerLock = new Object();
    private Handler renderThreadHandler;
    private EglBase eglBase;
    private final RendererCommon.YuvUploader yuvUploader = new RendererCommon.YuvUploader();
    private RendererCommon.GlDrawer drawer;
    private int[] yuvTextures = null;
    private final Object frameLock = new Object();
    private VideoRenderer.I420Frame pendingFrame;
    private final Object layoutLock = new Object();
    private Point desiredLayoutSize = new Point();
    private final Point layoutSize = new Point();
    private final Point surfaceSize = new Point();
    private boolean isSurfaceCreated;
    private int frameWidth;
    private int frameHeight;
    private int frameRotation;
    private RendererCommon.ScalingType scalingType;
    private boolean mirror;
    private RendererCommon.RendererEvents rendererEvents;
    private final Object statisticsLock;
    private int framesReceived;
    private int framesDropped;
    private int framesRendered;
    private long firstFrameTimeNs;
    private long renderTimeNs;
    private final Runnable renderFrameRunnable;
    private final Runnable makeBlackRunnable;

    private boolean inited;
    protected void init() {
        if(!this.inited) {
            EglBase eglContext = QBRTCClient.getInstance(this.getContext()).getEglContext();
            if(eglContext != null) {
                this.init(eglContext.getEglBaseContext(), (RendererCommon.RendererEvents)null);
                this.inited = true;
            }
        }
    }

    public interface bitmapListner{
        void getBitmapFromVideoRenderGUI(Bitmap bmp);
        void onGetRemoteMatData(Mat mat, float x, float y, float width, float height, float[] ptArray, int color);
    }
    public static bitmapListner myBmpListener;
    public static ArrayList<Rect> arrayTrackBox = new ArrayList<org.opencv.core.Rect>();
    public static ArrayList<org.opencv.core.Point> freeDrawPoint = new ArrayList<org.opencv.core.Point>();
    public static final int VIEW_MODE_CMT = 8;
    public static final int START_CMT = 7;
    public static int mViewMode = START_CMT;
    public static boolean isResetCMT = false;
    public static int drawColor = Color.GREEN;

    public static void initListener(bitmapListner listner){
        myBmpListener = listner;
    }
    private void drawRegions(byte[] yuv0, int strides0, int frameWidth, int frameHeight){
        if(myBmpListener != null) {
            if(isResetCMT){
                freeDrawPoint = new ArrayList<org.opencv.core.Point>();
                arrayTrackBox = new ArrayList<org.opencv.core.Rect>();
                NativeFunc.ResetCMT();
                isResetCMT = false;
                myBmpListener.getBitmapFromVideoRenderGUI(null);
            }else{
                switch (mViewMode) {
                    case START_CMT: {
                        if (arrayTrackBox.size() != 0){
                            //
                            org.opencv.core.Rect rt = arrayTrackBox.get(arrayTrackBox.size() - 1);
                            float[] ptArray = new float[freeDrawPoint.size() * 2];
                            for(int i = 0; i < freeDrawPoint.size(); i++){
                                ptArray[i * 2] = (float)(freeDrawPoint.get(i).x);
                                ptArray[i * 2 + 1] = (float)(freeDrawPoint.get(i).y);
                            }
                            Mat grayMat = new Mat();
                            Mat tMat = new Mat(frameHeight, frameWidth, CvType.CV_8UC1);
                            tMat.put(0, 0, yuv0);
                            NativeFunc.getFeatureMat(grayMat.getNativeObjAddr(), frameWidth, frameHeight,
                                    getMeasuredWidth(), getMeasuredHeight(),
                                    yuv0,
                                    strides0);
                            if(grayMat.width() > 0) {
                                Bitmap bmp = Bitmap.createBitmap(grayMat.width(), grayMat.height(), Bitmap.Config.ARGB_8888);
                                Utils.matToBitmap(grayMat, bmp);
                                myBmpListener.getBitmapFromVideoRenderGUI(bmp);
                            }

                            if(tMat.width() > 0) {
                                Bitmap bmp = Bitmap.createBitmap(tMat.width(), tMat.height(), Bitmap.Config.ARGB_8888);
                                Utils.matToBitmap(tMat, bmp);
                                myBmpListener.getBitmapFromVideoRenderGUI(bmp);
                            }
//                            myBmpListener.onGetRemoteMatData(grayMat, (float)rt.x,
//                                    (float)rt.y,
//                                    (float)rt.width,
//                                    (float)rt.height, ptArray,
//                                    drawColor);
                        }
                        mViewMode = VIEW_MODE_CMT;
                    }
                    break;
                    case VIEW_MODE_CMT:{
                        Mat tMat = new Mat(frameHeight, frameWidth, CvType.CV_8UC1);
                        tMat.put(0, 0, yuv0);
                        if(tMat.width() > 0) {
                            Bitmap bmp = Bitmap.createBitmap(tMat.width(), tMat.height(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(tMat, bmp);
                            myBmpListener.getBitmapFromVideoRenderGUI(bmp);
                        }
//                        Mat markMat = new Mat();
//                        NativeFunc.ProcessCMT(markMat.getNativeObjAddr(), frameWidth, frameHeight,
//                                getMeasuredWidth(), getMeasuredHeight(),
//                                yuv0,
//                                strides0);
//                        if(markMat.width() > 0){
//                            Bitmap bmp = Bitmap.createBitmap(markMat.width(), markMat.height(), Bitmap.Config.ARGB_8888);
//                            Utils.matToBitmap(markMat, bmp);
//                            myBmpListener.getBitmapFromVideoRenderGUI(bmp);
//                        }else{
//                            myBmpListener.getBitmapFromVideoRenderGUI(null);
//                        }
                    }
                    break;
                }
            }

        }

    }

    public MyRemoteRTC_View(Context context) {
        super(context);
        this.scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;
        this.statisticsLock = new Object();
        this.renderFrameRunnable = new Runnable() {
            public void run() {
                MyRemoteRTC_View.this.renderFrameOnRenderThread();
            }
        };
        this.makeBlackRunnable = new Runnable() {
            public void run() {
                MyRemoteRTC_View.this.makeBlack();
            }
        };
        this.getHolder().addCallback(this);
    }

    public MyRemoteRTC_View(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;
        this.statisticsLock = new Object();
        this.renderFrameRunnable = new Runnable() {
            public void run() {
                MyRemoteRTC_View.this.renderFrameOnRenderThread();
            }
        };
        this.makeBlackRunnable = new Runnable() {
            public void run() {
                MyRemoteRTC_View.this.makeBlack();
            }
        };
        this.getHolder().addCallback(this);
    }

    public void init(org.webrtc.EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
        this.init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, new GlRectDrawer());
    }

    public void init(org.webrtc.EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents, int[] configAttributes, RendererCommon.GlDrawer drawer) {
        Object var5 = this.handlerLock;
        synchronized(this.handlerLock) {
            if(this.renderThreadHandler != null) {
                throw new IllegalStateException(this.getResourceName() + "Already initialized");
            }

            Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Initializing.");
            this.rendererEvents = rendererEvents;
            this.drawer = drawer;
            this.renderThread = new HandlerThread("MySurfaceViewRenderer");
            this.renderThread.start();
            this.eglBase = EglBase.create(sharedContext, configAttributes);
            this.renderThreadHandler = new Handler(this.renderThread.getLooper());
        }

        this.tryCreateEglSurface();
    }

    public void tryCreateEglSurface() {
        this.runOnRenderThread(new Runnable() {
            public void run() {
                synchronized(MyRemoteRTC_View.this.layoutLock) {
                    if(MyRemoteRTC_View.this.isSurfaceCreated && !MyRemoteRTC_View.this.eglBase.hasSurface()) {
                        MyRemoteRTC_View.this.eglBase.createSurface(MyRemoteRTC_View.this.getHolder().getSurface());
                        MyRemoteRTC_View.this.eglBase.makeCurrent();
                        GLES20.glPixelStorei(3317, 1);
                    }

                }
            }
        });
    }

    public void release() {
        final CountDownLatch eglCleanupBarrier = new CountDownLatch(1);
        Object var2 = this.handlerLock;
        synchronized(this.handlerLock) {
            if(this.renderThreadHandler == null) {
                Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Already released");
                return;
            }

            this.renderThreadHandler.postAtFrontOfQueue(new Runnable() {
                public void run() {
                    MyRemoteRTC_View.this.drawer.release();
                    MyRemoteRTC_View.this.drawer = null;
                    if(MyRemoteRTC_View.this.yuvTextures != null) {
                        GLES20.glDeleteTextures(3, MyRemoteRTC_View.this.yuvTextures, 0);
                        MyRemoteRTC_View.this.yuvTextures = null;
                    }

                    MyRemoteRTC_View.this.makeBlack();
                    MyRemoteRTC_View.this.eglBase.release();
                    MyRemoteRTC_View.this.eglBase = null;
                    eglCleanupBarrier.countDown();
                }
            });
            this.renderThreadHandler = null;
        }

        ThreadUtils.awaitUninterruptibly(eglCleanupBarrier);
        this.renderThread.quit();
        var2 = this.frameLock;
        synchronized(this.frameLock) {
            if(this.pendingFrame != null) {
                VideoRenderer.renderFrameDone(this.pendingFrame);
                this.pendingFrame = null;
            }
        }

        ThreadUtils.joinUninterruptibly(this.renderThread);
        this.renderThread = null;
        var2 = this.layoutLock;
        synchronized(this.layoutLock) {
            this.frameWidth = 0;
            this.frameHeight = 0;
            this.frameRotation = 0;
            this.rendererEvents = null;
        }

        this.resetStatistics();
    }

    public void resetStatistics() {
        Object var1 = this.statisticsLock;
        synchronized(this.statisticsLock) {
            this.framesReceived = 0;
            this.framesDropped = 0;
            this.framesRendered = 0;
            this.firstFrameTimeNs = 0L;
            this.renderTimeNs = 0L;
        }
    }

    public void setMirror(boolean mirror) {
        Object var2 = this.layoutLock;
        synchronized(this.layoutLock) {
            this.mirror = mirror;
        }
    }

    public void setScalingType(RendererCommon.ScalingType scalingType) {
        Object var2 = this.layoutLock;
        synchronized(this.layoutLock) {
            this.scalingType = scalingType;
        }
    }

    public void renderFrame(VideoRenderer.I420Frame frame) {
        Object var2 = this.statisticsLock;
        synchronized(this.statisticsLock) {
            ++this.framesReceived;
        }

        var2 = this.handlerLock;
        synchronized(this.handlerLock) {
            if(this.renderThreadHandler == null) {
                Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Dropping frame - Not initialized or already released.");
                VideoRenderer.renderFrameDone(frame);
            } else {
                Object var3 = this.frameLock;
                synchronized(this.frameLock) {
                    if(this.pendingFrame != null) {
                        Object var4 = this.statisticsLock;
                        synchronized(this.statisticsLock) {
                            ++this.framesDropped;
                        }

                        VideoRenderer.renderFrameDone(this.pendingFrame);
                    }

                    this.pendingFrame = frame;
                    this.updateFrameDimensionsAndReportEvents(frame);
                    this.renderThreadHandler.post(this.renderFrameRunnable);
                }

            }
        }
    }

    private Point getDesiredLayoutSize(int widthSpec, int heightSpec) {
        Object var3 = this.layoutLock;
        synchronized(this.layoutLock) {
            int maxWidth = getDefaultSize(2147483647, widthSpec);
            int maxHeight = getDefaultSize(2147483647, heightSpec);
            Point size = RendererCommon.getDisplaySize(this.scalingType, this.frameAspectRatio(), maxWidth, maxHeight);
            if(MeasureSpec.getMode(widthSpec) == 1073741824) {
                size.x = maxWidth;
            }

            if(MeasureSpec.getMode(heightSpec) == 1073741824) {
                size.y = maxHeight;
            }

            return size;
        }
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        Object var3 = this.layoutLock;
        synchronized(this.layoutLock) {
            if(this.frameWidth != 0 && this.frameHeight != 0) {
                this.desiredLayoutSize = this.getDesiredLayoutSize(widthSpec, heightSpec);
                if(this.desiredLayoutSize.x != this.getMeasuredWidth() || this.desiredLayoutSize.y != this.getMeasuredHeight()) {
                    Object var4 = this.handlerLock;
                    synchronized(this.handlerLock) {
                        if(this.renderThreadHandler != null) {
                            this.renderThreadHandler.postAtFrontOfQueue(this.makeBlackRunnable);
                        }
                    }
                }

                this.setMeasuredDimension(this.desiredLayoutSize.x, this.desiredLayoutSize.y);
            } else {
                super.onMeasure(widthSpec, heightSpec);
            }
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Object var6 = this.layoutLock;
        synchronized(this.layoutLock) {
            this.layoutSize.x = right - left;
            this.layoutSize.y = bottom - top;
        }

        this.runOnRenderThread(this.renderFrameRunnable);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Surface created.");
        Object var2 = this.layoutLock;
        synchronized(this.layoutLock) {
            this.isSurfaceCreated = true;
        }

        this.tryCreateEglSurface();
        this.init();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Surface destroyed.");
        Object var2 = this.layoutLock;
        synchronized(this.layoutLock) {
            this.isSurfaceCreated = false;
            this.surfaceSize.x = 0;
            this.surfaceSize.y = 0;
        }

        this.runOnRenderThread(new Runnable() {
            public void run() {
                MyRemoteRTC_View.this.eglBase.releaseSurface();
            }
        });
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Surface changed: " + width + "x" + height);
        Object var5 = this.layoutLock;
        synchronized(this.layoutLock) {
            this.surfaceSize.x = width;
            this.surfaceSize.y = height;
        }

        this.runOnRenderThread(this.renderFrameRunnable);
    }

    private void runOnRenderThread(Runnable runnable) {
        Object var2 = this.handlerLock;
        synchronized(this.handlerLock) {
            if(this.renderThreadHandler != null) {
                this.renderThreadHandler.post(runnable);
            }

        }
    }

    private String getResourceName() {
        try {
            return this.getResources().getResourceEntryName(this.getId()) + ": ";
        } catch (Resources.NotFoundException var2) {
            return "";
        }
    }

    private void makeBlack() {
        if(Thread.currentThread() != this.renderThread) {
            throw new IllegalStateException(this.getResourceName() + "Wrong thread.");
        } else {
            if(this.eglBase != null && this.eglBase.hasSurface()) {
                GLES20.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                GLES20.glClear(16384);
                this.eglBase.swapBuffers();
            }

        }
    }

    private boolean checkConsistentLayout() {
        if(Thread.currentThread() != this.renderThread) {
            throw new IllegalStateException(this.getResourceName() + "Wrong thread.");
        } else {
            Object var1 = this.layoutLock;
            synchronized(this.layoutLock) {
                return this.layoutSize.equals(this.desiredLayoutSize) && this.surfaceSize.equals(this.layoutSize);
            }
        }
    }

    private void renderFrameOnRenderThread() {
        if(Thread.currentThread() != this.renderThread) {
            throw new IllegalStateException(this.getResourceName() + "Wrong thread.");
        } else {
            Object startTimeNs = this.frameLock;
            VideoRenderer.I420Frame frame;
            synchronized(this.frameLock) {
                if(this.pendingFrame == null) {
                    return;
                }

                frame = this.pendingFrame;
                this.pendingFrame = null;
            }

            if(this.eglBase != null && this.eglBase.hasSurface()) {
                if(!this.checkConsistentLayout()) {
                    this.makeBlack();
                    VideoRenderer.renderFrameDone(frame);
                } else {
                    startTimeNs = this.layoutLock;
                    synchronized(this.layoutLock) {
                        if(this.eglBase.surfaceWidth() != this.surfaceSize.x || this.eglBase.surfaceHeight() != this.surfaceSize.y) {
                            this.makeBlack();
                        }
                    }

                    long var16 = System.nanoTime();
                    Object i = this.layoutLock;
                    float[] texMatrix;
                    synchronized(this.layoutLock) {
                        float[] rotatedSamplingMatrix = RendererCommon.rotateTextureMatrix(frame.samplingMatrix, (float)frame.rotationDegree);
                        float[] layoutMatrix = RendererCommon.getLayoutMatrix(this.mirror, this.frameAspectRatio(), (float)this.layoutSize.x / (float)this.layoutSize.y);
                        texMatrix = RendererCommon.multiplyMatrices(rotatedSamplingMatrix, layoutMatrix);
                    }

                    GLES20.glClear(16384);
                    if(frame.yuvFrame) {
                        if(this.yuvTextures == null) {
                            this.yuvTextures = new int[3];

                            for(int var17 = 0; var17 < 3; ++var17) {
                                this.yuvTextures[var17] = GlUtil.generateTexture(3553);
                            }
                        }

                        this.yuvUploader.uploadYuvData(this.yuvTextures, frame.width, frame.height, frame.yuvStrides, frame.yuvPlanes);
                        Log.e("ERRRRRR", "YUVPlanes is not null");
                        this.drawer.drawYuv(this.yuvTextures, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(), 0, 0, this.surfaceSize.x, this.surfaceSize.y);
                    } else {
                        this.drawer.drawOes(frame.textureId, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(), 0, 0, this.surfaceSize.x, this.surfaceSize.y);
                    }

                    this.eglBase.swapBuffers();
                    VideoRenderer.renderFrameDone(frame);
                    i = this.statisticsLock;
                    synchronized(this.statisticsLock) {
                        if(this.framesRendered == 0) {
                            this.firstFrameTimeNs = var16;
                            Object var18 = this.layoutLock;
                            synchronized(this.layoutLock) {
                                Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Reporting first rendered frame.");
                                if(this.rendererEvents != null) {
                                    this.rendererEvents.onFirstFrameRendered();
                                }
                            }
                        }

                        ++this.framesRendered;
                        this.renderTimeNs += System.nanoTime() - var16;
                        if(this.framesRendered % 300 == 0) {
                            this.logStatistics();
                        }

                    }
                }
            } else {
                Logging.d("MySurfaceViewRenderer", this.getResourceName() + "No surface to draw on");
                VideoRenderer.renderFrameDone(frame);
            }
        }
    }

    private float frameAspectRatio() {
        Object var1 = this.layoutLock;
        synchronized(this.layoutLock) {
            return this.frameWidth != 0 && this.frameHeight != 0?(this.frameRotation % 180 == 0?(float)this.frameWidth / (float)this.frameHeight:(float)this.frameHeight / (float)this.frameWidth):0.0F;
        }
    }

    private void updateFrameDimensionsAndReportEvents(VideoRenderer.I420Frame frame) {
        Object var2 = this.layoutLock;
        synchronized(this.layoutLock) {
            if(this.frameWidth != frame.width || this.frameHeight != frame.height || this.frameRotation != frame.rotationDegree) {
                Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Reporting frame resolution changed to " + frame.width + "x" + frame.height + " with rotation " + frame.rotationDegree);
                if(this.rendererEvents != null) {
                    this.rendererEvents.onFrameResolutionChanged(frame.width, frame.height, frame.rotationDegree);
                }

                this.frameWidth = frame.width;
                this.frameHeight = frame.height;
                this.frameRotation = frame.rotationDegree;
                this.post(new Runnable() {
                    public void run() {
                        MyRemoteRTC_View.this.requestLayout();
                    }
                });
            }

        }
    }

    private void logStatistics() {
        Object var1 = this.statisticsLock;
        synchronized(this.statisticsLock) {
            Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Frames received: " + this.framesReceived + ". Dropped: " + this.framesDropped + ". Rendered: " + this.framesRendered);
            if(this.framesReceived > 0 && this.framesRendered > 0) {
                long timeSinceFirstFrameNs = System.nanoTime() - this.firstFrameTimeNs;
                Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Duration: " + (int)((double)timeSinceFirstFrameNs / 1000000.0D) + " ms. FPS: " + (double)this.framesRendered * 1.0E9D / (double)timeSinceFirstFrameNs);
                Logging.d("MySurfaceViewRenderer", this.getResourceName() + "Average render time: " + (int)(this.renderTimeNs / (long)(1000 * this.framesRendered)) + " us.");
            }

        }
    }
}
