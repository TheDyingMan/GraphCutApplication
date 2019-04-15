#include <jni.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
using namespace cv;

 extern "C"
 {
    //灰度图转换函数声明
    JNIEXPORT jintArray JNICALL Java_com_example_jaychen_graphcutapp_MainActivity_Cvt2Grey(JNIEnv* env,jobject obj,jintArray buf,int width, int height);

    //灰度图转换函数定义
    JNIEXPORT jintArray JNICALL Java_com_example_jaychen_graphcutapp_MainActivity_Cvt2Grey(JNIEnv* env,jobject obj,jintArray buf,int width, int height)
    {

        jint* cbuf;
        jboolean ptfalse = false;
        cbuf = env->GetIntArrayElements(buf, &ptfalse);
        if(cbuf==NULL)
        {
            return 0;
        }

        Mat myImg(height,width,CV_8UC4,(unsigned char*)cbuf);

        Mat GreyImage;
        cvtColor(myImg,GreyImage,COLOR_BGRA2GRAY);
        cvtColor(GreyImage,GreyImage,COLOR_GRAY2BGRA);

        int size=width*height;
        jintArray result=env->NewIntArray(size);
        env->SetIntArrayRegion(result,0,size,(jint*)GreyImage.data);
        env->ReleaseIntArrayElements(buf,cbuf,0);

        return result;
    }
 }
