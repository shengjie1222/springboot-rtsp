package com.ethereal.rtmp.util;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Properties;

/**
 * 学习（记忆）识别
 * @author Administrator
 * @Description
 * @create 2021-06-18 13:52
 */
public class OpencvMemoryRecognition {


    static{
        Properties properties=System.getProperties();
        String propertiesValue = properties.getProperty("os.arch");
        try {
            String dllPath = new ClassPathResource(String.format("static/opencv_java340/opencv_java340-x%s.dll", (propertiesValue.equals("x86") ? "32" : "64"))).getFile().getAbsolutePath();
            System.load(dllPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        Mat image = Imgcodecs.imread("G:\\workspace\\opencv\\example\\16.png");
        System.out.println(getNum(image));
    }

    public static int getNum(Mat sourceImage){
        int num=0;
        int min=30000;

        for(int i=11;i<=17;i++){
            Mat targetImage = Imgcodecs.imread("G:\\workspace\\opencv\\example\\" + i + ".png");
            Mat targetGreyImage = new Mat();
            Mat sourceGreyImage = new Mat();
            Mat target = new Mat();
            Mat source = new Mat();
            //灰度处理
            Imgproc.cvtColor(targetImage, targetGreyImage, Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(sourceImage, sourceGreyImage, Imgproc.COLOR_BGR2GRAY);
            //二值化
            Imgproc.threshold(targetGreyImage, target, 180, 255, Imgproc.THRESH_BINARY);
            Imgproc.threshold(sourceGreyImage, source, 180, 255, Imgproc.THRESH_BINARY);
            //两个矩阵的差的绝对值
            Mat sub = new Mat();
            Core.absdiff(source, target, sub);
            int now = getSum(sub);
            System.out.println(i+":"+now);
            if(now<min){
                min = now;
                num=i;
            }
        }

        if(num==10) {
            num=1;
        }
        return num;
    }

    private static int getSum(Mat res){
        int sum = 0;
        for (int y = 0; y < res.rows(); y++) {
            for (int x = 0; x < res.cols(); x++) {
                sum += res.get(y, x)[0];
            }
        }
       return sum;
    }
}
