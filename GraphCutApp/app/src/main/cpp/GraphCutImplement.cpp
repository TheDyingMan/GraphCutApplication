#include <jni.h>
#include <android/log.h>

#include <opencv2/opencv.hpp>
using namespace cv;
#include <queue>
using std::queue;
#include <vector>
using std::vector;
#include <iostream>
using std::cin;
using std::cout;
using std::endl;
#include <utility>
using std::pair;

#define NODENUMBER 6
#define INTMAXIMUM 2147483647

struct Edge
{
	Edge(int f, int c) :contain(c), flow(f) {};
	double contain;
	double flow;
};

struct flag
{
	double l = -1.0;
	int from;
	bool positive;

	bool isVisit = false;
};

struct GNode
{
	int index;

	//0为source，1234分别问上下左右，5为target
	vector<int> forwardPoint; //存储了该节点所联通的节点的index
	vector<Edge> forwoardEgde; //存储了该节点到所联通节点的边

	vector<int> backwordPoint; //存储了该节点所联通的反向边节点
	vector<Edge> backwardEdge; //存储了该节点到所联通反向边节点的边

	flag flag;

};

vector<GNode>Graph; //图
vector<pair<int,int>> ObjectPoint; //用户标记的前景点
vector<pair<int,int>> BackgroundPoint; //用户标记的背景点
int ObjectPointGreyHistogram[256]{0};
int BackgroundPointGreyHistogram[256]{0};


int SOURCE;
int TARGET;

extern "C"
{
    JNIEXPORT void JNICALL Java_com_example_jaychen_graphcutapp_MainActivity_AddObjectPoint(JNIEnv* env,jobject obj ,int x, int y);

    JNIEXPORT void JNICALL Java_com_example_jaychen_graphcutapp_MainActivity_AddObjectPoint(JNIEnv* env,jobject obj ,int x, int y)
    {
        ObjectPoint.push_back(pair<int,int>(x,y));
//        __android_log_print(ANDROID_LOG_INFO, "object add ", "size:%d   %d---%d",ObjectPoint.size(), x,y);
    }

    JNIEXPORT void JNICALL Java_com_example_jaychen_graphcutapp_MainActivity_AddBackgroundPoint(JNIEnv* env,jobject obj ,int x, int y);

    JNIEXPORT void JNICALL Java_com_example_jaychen_graphcutapp_MainActivity_AddBackgroundPoint(JNIEnv* env,jobject obj ,int x, int y)
    {
        BackgroundPoint.push_back(pair<int,int>(x,y));
//        __android_log_print(ANDROID_LOG_INFO, "background add ", "size:%d   %d---%d",BackgroundPoint.size(),x,y);
    }

}

void BuildGraph(Mat* image, int width,int height)
{

//检查原图数据读取是否有问题
//    __android_log_print(ANDROID_LOG_INFO, "SourceImageData", "image rows:%d,image clos:%d ", image->rows,image->cols);
//    __android_log_print(ANDROID_LOG_INFO, "SourceImageData", "data type:%d", image->type());
//    __android_log_print(ANDROID_LOG_INFO, "SourceImageData", "%d", image->channels());
//    __android_log_print(ANDROID_LOG_INFO, "SourceImageData", "R:%d G:%d B:%d Gray:%d", image->at<Vec4b>(0,0)[2],image->at<Vec4b>(0,0)[1],image->at<Vec4b>(0,0)[0],int(image->at<Vec4b>(0,0)[2]*0.299+ image->at<Vec4b>(0,0)[1]*0.587+ image->at<Vec4b>(0,0)[0]*0.114));
//    __android_log_print(ANDROID_LOG_INFO, "SourceImageData", "R:%d G:%d B:%d Gray:%d", image->at<Vec4b>(0,1)[2],image->at<Vec4b>(0,1)[1],image->at<Vec4b>(0,1)[0],int(image->at<Vec4b>(0,1)[2]*0.299+ image->at<Vec4b>(0,1)[1]*0.587+ image->at<Vec4b>(0,1)[0]*0.114));

    //获取原图片的灰度图
    Mat GreyImage;
    cvtColor(*image,GreyImage,COLOR_BGRA2GRAY);

//检查转化成的灰度图的读取是否有问题
//    __android_log_print(ANDROID_LOG_INFO, "GreyImageData", "image rows:%d,image clos:%d ", GreyImage.rows,GreyImage.cols);
//    __android_log_print(ANDROID_LOG_INFO, "GreyImageData", "data type:%d", GreyImage.type());
//    __android_log_print(ANDROID_LOG_INFO, "GreyImageData", "%d", GreyImage.channels());
//    __android_log_print(ANDROID_LOG_INFO, "GreyImageData", "R:%d G:%d B:%d", GreyImage.at<schar>(0,0),GreyImage.at<schar>(0,1),GreyImage.at<schar>(0,2));

    Graph.resize(width*height);

    //源点
    GNode s;
    s.flag.l=INTMAXIMUM;
    s.flag.positive=false;

    //汇点
    GNode t;
    t.flag.l=-1;

    Graph.push_back(s);
    Graph.push_back(t);
    SOURCE=height*width;
    TARGET=height*width+1;

//检查源点，汇点是否有问题
//    __android_log_print(ANDROID_LOG_INFO, "GraphData", "source:%f", Graph[SOURCE].flag.l);
//    __android_log_print(ANDROID_LOG_INFO, "GraphData", "target:%f", Graph[TARGET].flag.l);

//    __android_log_print(ANDROID_LOG_INFO, "SignedPointsData", "object:%d",ObjectPoint.size());
//    __android_log_print(ANDROID_LOG_INFO, "SignedPointsData", "bkg:%d", BackgroundPoint.size());

    //开始收集用户收集的背景点和前景点的灰度直方图
    for(int i=0;i<ObjectPoint.size();i++)
    {
        int x=ObjectPoint[i].first;
        int y=ObjectPoint[i].second;
        int pixel=GreyImage.at<uchar>(x,y);
        ObjectPointGreyHistogram[pixel]++;
    }

    for(int i=0;i<BackgroundPoint.size();i++)
    {
        int x=BackgroundPoint[i].first;
        int y=BackgroundPoint[i].second;
        int pixel=GreyImage.at<uchar>(x,y);
        BackgroundPointGreyHistogram[pixel]++;
    }

//    for(int i=0;i<256;i++)
//    {
//         __android_log_print(ANDROID_LOG_INFO, "ObjectHistogramData", " ");
//         __android_log_print(ANDROID_LOG_INFO, "ObjectHistogramData", "%d", ObjectPointGreyHistogram[i]);
//    }

//    for(int i=0;i<256;i++)
//    {
//         __android_log_print(ANDROID_LOG_INFO, "BackgroundHistogramData", " ");
//         __android_log_print(ANDROID_LOG_INFO, "BackgroundHistogramData", "%d", BackgroundPointGreyHistogram[i]);
//    }

    //开始遍历灰度图，
    for(int row=0;row<GreyImage.rows;row++)
    {
        schar* rowNow= GreyImage.ptr<schar>(row);
        for(int col=0;col<GreyImage.cols;col++)
        {
//            __android_log_print(ANDROID_LOG_INFO, "GreyImageData", "%d", rowNow[col]);

        }
    }
}

