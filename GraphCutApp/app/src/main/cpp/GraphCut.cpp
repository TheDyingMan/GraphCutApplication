#include <jni.h>
#include <opencv2/opencv.hpp>
using namespace cv;

extern "C"
 {
    //灰度图转换函数声明
    JNIEXPORT jintArray JNICALL Java_com_example_jaychen_graphcutapp_MainActivity_GraphCut(JNIEnv* env,jobject obj,jintArray buf,int width, int height);

    //灰度图转换函数定义
    JNIEXPORT jintArray JNICALL Java_com_example_jaychen_graphcutapp_MainActivity_GraphCut(JNIEnv* env,jobject obj,jintArray buf,int width, int height)
    {

        jint* inputImageDatabuf;
        jboolean ptfalse = false;
        inputImageDatabuf = env->GetIntArrayElements(buf, &ptfalse); //让inputImageDatabuf指向传入的int数组类型（也就是传入的图片的数据）的地址
        if(inputImageDatabuf==NULL)
        {
            return 0;
        }

        Mat resultImg;//结果Mat

        //将int数组类型转化成Mat类型
        Mat myImg(height,width,CV_8UC4,(unsigned char*)inputImageDatabuf);




        //将处理完的图片输出
        int size=width*height;
        jintArray result=env->NewIntArray(size);
        env->SetIntArrayRegion(result,0,size,(jint*)myImg.data);
        env->ReleaseIntArrayElements(buf,inputImageDatabuf,0);

        return result;
    }
 }