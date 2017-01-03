#include "com_adnet_archat_MainActivity.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include "cmt.h"
#include <vector>
#include <GLES2/gl2.h>

#include <android/log.h>

#define LOG_TAG "FaceDetection/DetectionBasedTracker"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace std;
using namespace cv;
typedef unsigned char byte;

CMT arrayCMT[100];
float arrayFreeDrawingPoint[100][500];
float rectArray[100][4];
int pointCntArray[100];
int colorArray[100];
bool arrayCMTInitializeResult[100];
int cmtCNT = 0;
long rect[4];
cv::Ptr<cv::DescriptorExtractor> descriptorExtractor = cv::BRISK::create(40, 1, 1.0);
int COMPRESS_RATIO = 8;

JNIEXPORT void JNICALL Java_com_adnet_archat_NativeFunc_ResetCMT(JNIEnv* env, jobject instance)
{
    memset(rectArray, 0, sizeof(float) * 100 * 4);
    memset(pointCntArray, 0, sizeof(int) * 100);
    memset(colorArray, 0, sizeof(int) * 100);
    memset(arrayCMTInitializeResult, 0, sizeof(bool) * 100);
    memset(arrayFreeDrawingPoint, 0, sizeof(float) * 100 * 500);
    cmtCNT = 0;
}

JNIEXPORT void JNICALL Java_com_adnet_archat_NativeFunc_UndoCMT(JNIEnv* env, jobject instance)
{
    if(cmtCNT <= 0)
        return;
    cmtCNT--;
    memset(&rectArray[cmtCNT][0], 0, 4 * sizeof(int));
    pointCntArray[cmtCNT] = 0;
    memset(&arrayFreeDrawingPoint[cmtCNT][0], 0, 500 * sizeof(float));
    colorArray[cmtCNT] = 0;
    arrayCMTInitializeResult[cmtCNT] = false;
LOGD("Removing END");
}


Mat getMat(int imgWidth, int imgHeight, int displayWidth, int displayHeight, jbyte *yuvArray0, jsize size0, int yuvStrides0){
    byte* yuvBytes = new byte[imgWidth*imgHeight * 3 / 2];
    int i = 0;
    memset(yuvBytes, 0, imgWidth*imgHeight * 3 / 2);
    for (int row = 0; row<imgHeight; row++) {
        for (int col = 0; col<imgWidth; col++) {
            yuvBytes[i++] = yuvArray0[col + row*yuvStrides0];
        }
    }

    Mat gray(imgHeight, imgWidth, CV_8UC1, yuvBytes);
    Mat dst;
    float zoomWidth = 0, zoomHeight = 0;
    float cutWidth = 0, cutHeight = 0;
    Rect roiRect;
    cv::transpose(gray, dst);
    cv::flip(dst, dst, 1);
    float scaleX = (float)displayWidth / dst.size().width, scaleY = (float)displayHeight / dst.size().height;
    if(scaleX > scaleY){
        zoomWidth = dst.size().width * scaleX;
        zoomHeight = dst.size().height * scaleX;
        Size sz(zoomWidth, zoomHeight);
        resize(dst, gray, sz);
        cutHeight = (zoomHeight - displayHeight) / 2;
        roiRect = Rect(0, cutHeight, zoomWidth, displayHeight);
    }else{
        zoomWidth = dst.size().width * scaleY;
        zoomHeight = dst.size().height * scaleY;
        Size sz(zoomWidth, zoomHeight);
        resize(dst, gray, sz);
        cutWidth = (zoomWidth - displayWidth) / 2;
        roiRect = Rect(cutWidth, 0, displayWidth, zoomHeight);
    }
    dst = gray(roiRect);

    resize(dst, gray, dst.size() / COMPRESS_RATIO);
    if (yuvBytes) delete yuvBytes;
    return gray;
}

JNIEXPORT void JNICALL Java_com_adnet_archat_NativeFunc_getFeatureMat(JNIEnv* env, jobject,
    jlong addrGray, jint imgWidth, jint imgHeight, jint displayWidth, jint displayHeight,
    jbyteArray yuvPlanes0, jint yuvStrides0)
{

    byte* yuvBytes = new byte[imgWidth*imgHeight * 3 / 2];
    jsize size0 = env->GetArrayLength(yuvPlanes0);
    jbyte* yuvArray0 = new jbyte[size0];

    env->GetByteArrayRegion(yuvPlanes0,0,size0, yuvArray0);
    Mat gray = getMat(imgWidth, imgHeight, displayWidth, displayHeight, yuvArray0, size0, yuvStrides0);
    if(yuvArray0) delete yuvArray0;
    Mat& im_gray  = *(Mat*)addrGray;
    gray.copyTo(im_gray);
}

JNIEXPORT void JNICALL Java_com_adnet_archat_NativeFunc_OpenCMT(JNIEnv* env, jobject,
        jlong addrGray,jfloat x, jfloat y, jfloat width, jfloat height, jfloatArray pointArray, jint color)
{
    Mat& im_gray  = *(Mat*)addrGray;
    Point p1(x / COMPRESS_RATIO,y / COMPRESS_RATIO);
    Point p2((x+width) / COMPRESS_RATIO,(y+height) / COMPRESS_RATIO);

    cmtCNT++;
    if(cmtCNT >= 100)
    {
    cmtCNT = 100;
    return;
    }
    jsize size = env->GetArrayLength(pointArray);
    if(env->ExceptionCheck()) {
    #ifndef NDEBUG
    env->ExceptionDescribe();
    #endif
    env->ExceptionClear();
    return;
    }
    if(size > 500)
        size = 500;
    env->GetFloatArrayRegion(pointArray,0,size, arrayFreeDrawingPoint[cmtCNT - 1]);
    pointCntArray[cmtCNT - 1] = size;
    rectArray[cmtCNT - 1][0] = (float)x / (float) COMPRESS_RATIO;
    rectArray[cmtCNT - 1][1] = (float)y / (float) COMPRESS_RATIO;
    rectArray[cmtCNT - 1][2] = (float)(x + width ) / COMPRESS_RATIO;
    rectArray[cmtCNT - 1][3] = (float)(y + height) / COMPRESS_RATIO;
    arrayCMTInitializeResult[cmtCNT - 1]=false;
    arrayCMT[cmtCNT - 1].descriptorExtractor = descriptorExtractor;
    arrayCMT[cmtCNT - 1].detector = descriptorExtractor;
    colorArray[cmtCNT - 1] = color;
    if(arrayCMT[cmtCNT - 1].initialise(im_gray, p1, p2)){
        arrayCMTInitializeResult[cmtCNT - 1]=true;
    }
}


void getTransformedPoint(Mat *img, int index, float scaleX, float scaleY)
{
    std::vector<Point2f> obj;
    std::vector<Point2f> scene;
    obj.push_back(Point2f(rectArray[index][0], rectArray[index][1]));
    obj.push_back(Point2f(rectArray[index][2], rectArray[index][1]));
    obj.push_back(Point2f(rectArray[index][2], rectArray[index][3]));
    obj.push_back(Point2f(rectArray[index][0], rectArray[index][3]));

    scene.push_back(arrayCMT[index].topLeft);
    scene.push_back(arrayCMT[index].topRight);
    scene.push_back(arrayCMT[index].bottomRight);
    scene.push_back(arrayCMT[index].bottomLeft);
    float lenWidth = (rectArray[index][0] - rectArray[index][2]) * (rectArray[index][0] - rectArray[index][2]);
    float lenHeight = (rectArray[index][1] - rectArray[index][3]) * (rectArray[index][1] - rectArray[index][3]);

    float transformWidth = (arrayCMT[index].topLeft.x - arrayCMT[index].topRight.x) * (arrayCMT[index].topLeft.x - arrayCMT[index].topRight.x)
                    + (arrayCMT[index].topLeft.y - arrayCMT[index].topRight.y) * (arrayCMT[index].topLeft.y - arrayCMT[index].topRight.y);
    float transformHeight = (arrayCMT[index].topLeft.x - arrayCMT[index].bottomLeft.x) * (arrayCMT[index].topLeft.x - arrayCMT[index].bottomLeft.x)
                      + (arrayCMT[index].topLeft.y - arrayCMT[index].bottomLeft.y) * (arrayCMT[index].topLeft.y - arrayCMT[index].bottomLeft.y);

    if(lenWidth * COMPRESS_RATIO <= transformWidth || lenWidth / COMPRESS_RATIO >= transformWidth)
        return;
    if(lenHeight * COMPRESS_RATIO <= transformHeight || lenHeight / COMPRESS_RATIO >= transformHeight)
        return;

    Mat H = findHomography( obj, scene, CV_RANSAC );

    vector<Point2f> pin;
    vector<Point2f> pout;
    for(int i = 0; i < pointCntArray[index]; i++)
    {
        Point2f pos;
        pos.x = (float) arrayFreeDrawingPoint[index][i] / COMPRESS_RATIO;
        pos.y = (float) arrayFreeDrawingPoint[index][i + 1] / COMPRESS_RATIO;
        pin.push_back(pos);
        i++;
    }

    byte r = ((colorArray[index] >> 16) & 0xFF);  // Extract the RR byte
    byte g = ((colorArray[index] >> 8) & 0xFF);   // Extract the GG byte
    byte b = ((colorArray[index]) & 0xFF);
    perspectiveTransform( pin, pout, H);
    for(int i = 1; i < pointCntArray[index] / 2; i++)
    {
        line(*img, Point(pout[i - 1].x * COMPRESS_RATIO * scaleX, pout[i - 1].y * COMPRESS_RATIO * scaleY),
             Point(pout[i].x * COMPRESS_RATIO * scaleX, pout[i].y * COMPRESS_RATIO * scaleY), cv::Scalar(r, g, b, 255), 5,CV_AA,0);
//        line(*img, Point(pout[i - 1].x * COMPRESS_RATIO, pout[i - 1].y * COMPRESS_RATIO),
//             Point(pout[i].x * COMPRESS_RATIO, pout[i].y * COMPRESS_RATIO), cv::Scalar(r, g, b, 255), 5,CV_AA,0);
    }

//    line(*img, arrayCMT[index]->topLeft * 4, arrayCMT[index]->topRight * 4, cv::Scalar(r, g, b, 255), 5,CV_AA,0);
//    line(*img, arrayCMT[index]->topRight * 4, arrayCMT[index]->bottomRight * 4, cv::Scalar(r, g, b, 255), 5,CV_AA,0);
//    line(*img, arrayCMT[index]->bottomRight * 4, arrayCMT[index]->bottomLeft * 4, cv::Scalar(r, g, b, 255), 5,CV_AA,0);
//    line(*img, arrayCMT[index]->bottomLeft * 4, arrayCMT[index]->topLeft * 4, cv::Scalar(r, g, b, 255), 5,CV_AA,0);
}

JNIEXPORT void JNICALL Java_com_adnet_archat_NativeFunc_ProcessCMT(JNIEnv* env, jobject,
        jlong markMatAddr, jint imgWidth, jint imgHeight, jint displayWidth, jint displayHeight,
        jbyteArray yuvPlanes0, jint yuvStrides0)
{
    if(cmtCNT <= 0)
        return;
    Mat& img_mark  = *(Mat*)markMatAddr;
    byte* yuvBytes = new byte[imgWidth*imgHeight * 3 / 2];
    jsize size0 = env->GetArrayLength(yuvPlanes0);
    jbyte* yuvArray0 = new jbyte[size0];

    env->GetByteArrayRegion(yuvPlanes0,0,size0, yuvArray0);
    Mat gray = getMat(imgWidth, imgHeight, displayWidth, displayHeight, yuvArray0, size0, yuvStrides0);
    Mat transparentMark(displayHeight, displayWidth, CV_8UC4);
    transparentMark=Scalar(0,0,0,0);
//    resize(gray, transparentMark, Size(displayWidth, displayHeight));
//    Mat tmpMat;
//    transparentMark.copyTo(tmpMat);
//    cvtColor(tmpMat, transparentMark, CV_GRAY2RGB);

    jint fill[8];
    memset(fill, 0, 8 * sizeof(jint));
    std::vector<cv::KeyPoint> keypoints;
    cv::Mat features;
//    descriptorExtractor->detect(gray, keypoints);
//    descriptorExtractor->compute(gray, keypoints, features);
    for(int i = 0; i < cmtCNT; i++){
        if(arrayCMTInitializeResult[i]){
            float scaleX = (float)gray.size().width / arrayCMT[i].im_prev.size().width;
            float scaleY = (float)gray.size().height / arrayCMT[i].im_prev.size().height;
            Mat tmp;
            resize(gray, tmp, arrayCMT[i].im_prev.size());
            descriptorExtractor->detect(tmp, keypoints);
            descriptorExtractor->compute(tmp, keypoints, features);
            arrayCMT[i].processFrame(tmp, keypoints, features);
            getTransformedPoint(&transparentMark, i, scaleX, scaleY);
        }
    }
    transparentMark.copyTo(img_mark);
    if (yuvBytes) delete yuvBytes;
}