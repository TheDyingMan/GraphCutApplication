package com.example.jaychen.graphcutapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.FileUriExposedException;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity
{

     //Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("convertToGrey");
    }
    public native int[] Cvt2Grey(int[] buf, int w, int h);
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
//    }
//
//    /**
//     * A native method that is implemented by the 'native-lib' native library,
//     * which is packaged with this application.
//     */
//    public native String stringFromJNI();

    private final static int CHOSSE_IMAGE_REQUEST = 6;
    private final static int RESULT = 0;

    ImageView imgView;
    Button ChosseImage;
    Button Source_button, CV_button;


    private Bitmap ImageInScreen;//经过屏幕适应的缩放后的图片
    private Bitmap HandleImage;//图片副本
    private Canvas canvas;//画布
    private Paint paint;//画笔

    private float downx = 0; //开始按下的点的x
    private float downy = 0; //开始按下的点的y
    private float x = 0; //当前划过的点的x
    private float y = 0; //当前划过的点的y

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        this.setTitle("使用NDK转换灰度图");
        ChosseImage = (Button) this.findViewById(R.id.LoadImage);
        CV_button = (Button) this.findViewById(R.id.CV_button);
        Source_button = (Button) this.findViewById(R.id.Source_button);
        imgView = (ImageView) this.findViewById(R.id.ImageArea);

        ChosseImage.setOnClickListener(new ClickEvent());
    }

    class ClickEvent implements View.OnClickListener
    {
        public void onClick(View button)
        {
            if (button == ChosseImage)
            {
                Toast.makeText(getApplicationContext(), "点击加载图片按钮", Toast.LENGTH_SHORT).show();
                choose(ChosseImage);
            }
        }
    }

    // 选择图片
    public void choose(View view)
    {
        // 进入图库
//        Intent intent = new Intent(Intent.ACTION_PICK,
//                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");

        startActivityForResult(intent, CHOSSE_IMAGE_REQUEST);
    }

    //用户在画布上绘画
    public class MyTouchListener implements View.OnTouchListener
    {
        AppCompatActivity activity; //获得外部activity的引用）
        float ImageHeight; //图片的高
        float ImageWidth; //图片的宽

        //构造函数
        MyTouchListener(AppCompatActivity activity, float ImageHeight, float ImageWidth)
        {
            this.activity = activity;
            this.ImageHeight = ImageHeight;
            this.ImageWidth = ImageWidth;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            int action = event.getAction();
            if (event.getX() < 0 || event.getY() < 0 || event.getX() > ImageWidth || event.getY() > ImageHeight)
            {
                return true;
            }

            switch (action)
            {
                // 按下
                case MotionEvent.ACTION_DOWN:

                    downx = event.getX();
                    downy = event.getY();
//                    String logText;
//                    logText=String.format("X:%f,Y:%f",downx,downy);
//                    Toast.makeText(activity.getApplicationContext(), logText, Toast.LENGTH_SHORT).show();
//                    Log.e("x-y", event.getX() + "" + event.getY());
                    break;
                // 移动
                case MotionEvent.ACTION_MOVE:
                    // 路径画板
                    x = event.getX();
                    y = event.getY();
                    // 画线
                    canvas.drawLine(downx, downy, x, y, paint);
                    // 刷新image
                    imgView.invalidate();
                    downx = x;
                    downy = y;
//                    Log.e("x-y", downx + "-" + downy);
                    break;
                case MotionEvent.ACTION_UP:
                    break;

                default:
                    break;
            }
            // true：告诉系统，这个触摸事件由我来处理
            // false：告诉系统，这个触摸事件我不处理，这时系统会把触摸事件传递给imageview的父节点
            return true;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CHOSSE_IMAGE_REQUEST)
        {
            Toast.makeText(this.getApplicationContext(), "读取图片完毕，进入Activity", Toast.LENGTH_SHORT).show();

            // TODO Auto-generated method stub
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK)
            {
                Toast.makeText(this.getApplicationContext(), "读取成功，开始显示", Toast.LENGTH_LONG).show();
                // 获取选中的图片的Uri
                Uri imageFileUri = data.getData();
                // 获取屏幕大小
                Display display = getWindowManager().getDefaultDisplay();
                float displayWidth = display.getWidth();
                float displayHeight = display.getHeight();

                try
                {
                    // 解析图片时需要使用到的参数都封装在这个对象里了
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    // 不为像素申请内存，只获取图片宽高
                    options.inJustDecodeBounds = true;
                    ImageInScreen = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageFileUri), null, options);
                    // 设置缩放比例
                    int heightRatio = (int) Math.ceil(options.outHeight / displayHeight);
                    int widthRatio = (int) Math.ceil(options.outWidth / displayWidth);
                    if (heightRatio > 1 && widthRatio > 1)
                    {
                        if (heightRatio > widthRatio)
                        {
                            options.inSampleSize = heightRatio;
                        } else
                        {
                            options.inSampleSize = widthRatio;
                        }
                    }

                    // 为像素申请内存
                    options.inJustDecodeBounds = false;
                    // 获取缩放后的图片
                    ImageInScreen = BitmapFactory.decodeStream(getContentResolver()
                            .openInputStream(imageFileUri), null, options);
                    // 创建缩放后的图片副本
                    HandleImage = Bitmap.createBitmap(ImageInScreen.getWidth(),
                            ImageInScreen.getHeight(), ImageInScreen.getConfig());
//                    // 创建画布
//                    canvas = new Canvas(HandleImage);
//                    // 创建画笔
//                    paint = new Paint();
//                    // 设置画笔颜色
//                    paint.setColor(Color.GREEN);
//                    // 设置画笔宽度
//                    paint.setStrokeWidth(20);
//                    // 开始作画，把原图的内容绘制在白纸上
//                    canvas.drawBitmap(ImageInScreen, new Matrix(), paint);

                    int[] srcImageData=new int[ImageInScreen.getWidth()*ImageInScreen.getHeight()];
                    ImageInScreen.getPixels(srcImageData,0,ImageInScreen.getWidth(),0,0,ImageInScreen.getWidth(),ImageInScreen.getHeight());
                    int[] resultInt = Cvt2Grey(srcImageData, ImageInScreen.getWidth(),ImageInScreen.getHeight());
                    Bitmap resultImg = Bitmap.createBitmap(ImageInScreen.getWidth(),ImageInScreen.getHeight(), Bitmap.Config.ARGB_8888);
                    resultImg.setPixels(resultInt,0,ImageInScreen.getWidth(),0,0,ImageInScreen.getWidth(),ImageInScreen.getHeight());

                    // 将处理后的图片放入imageview中
                    imgView.setImageBitmap(resultImg);
                    // 设置imageview监听
//                    imgView.setOnTouchListener(new MyTouchListener(this, ImageInScreen.getHeight(), ImageInScreen.getWidth()));

                } catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }

//            try {
//                // 解析图片时需要使用到的参数都封装在这个对象里了
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                // 不为像素申请内存，只获取图片宽高
//                options.inJustDecodeBounds = true;
//                ImageInScreen = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageFileUri), null, options);
//                // 设置缩放比例
//                int heightRatio = (int) Math.ceil(options.outHeight / displayHeight);
//                int widthRatio = (int) Math.ceil(options.outWidth / displayWidth);
//                if (heightRatio > 1 && widthRatio > 1) {
//                    if (heightRatio > widthRatio) {
//                        options.inSampleSize = heightRatio;
//                    } else {
//                        options.inSampleSize = widthRatio;
//                    }
//                }
//                // 为像素申请内存
//                options.inJustDecodeBounds = false;
//                // 获取缩放后的图片
//                ImageInScreen = BitmapFactory.decodeStream(getContentResolver()
//                        .openInputStream(imageFileUri), null, options);
//                // 创建缩放后的图片副本
//                HandleImage = Bitmap.createBitmap(ImageInScreen.getWidth(),
//                        ImageInScreen.getHeight(), ImageInScreen.getConfig());
//                // 创建画布
//                canvas = new Canvas(HandleImage);
//                // 创建画笔
//                paint = new Paint();
//                // 设置画笔颜色
//                paint.setColor(Color.GREEN);
//                // 设置画笔宽度
//                paint.setStrokeWidth(10);
//                // 开始作画，把原图的内容绘制在白纸上
//                canvas.drawBitmap(ImageInScreen, new Matrix(), paint);
//                // 将处理后的图片放入imageview中
//                imgView.setImageBitmap(HandleImage);
//                // 设置imageview监听
//                imgView.setOnTouchListener(new MyTouchListener());
//            } catch (FileNotFoundException e) {
//
//                e.printStackTrace();
//            }
//
//        }
            }
        }
    }

}
