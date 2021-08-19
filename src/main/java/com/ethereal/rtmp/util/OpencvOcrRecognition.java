package com.ethereal.rtmp.util;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * OCR标准库识别
 * @author Administrator
 * @Description
 * @create 2021-06-18 10:57
 */
public class OpencvOcrRecognition{


    public static void main(String[] args) throws IOException {
        try {
            File directory = new File("G:\\workspace\\opencv\\code4");
            File[] files = directory.listFiles();
            for(File f: files){
                String code =  handleImage(f.getAbsolutePath());
                System.out.println(code);
            }
        } catch (InterruptedException | TesseractException e) {
            e.printStackTrace();
        }
    }

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

    public static String handleImage(String imageFile) throws IOException, InterruptedException, TesseractException {
        Mat image = Imgcodecs.imread(imageFile);
        File fileObj = new File(imageFile);
        Mat grayImage = new Mat();
        Mat result = new Mat();

        //灰度化
        Imgproc.cvtColor(image, grayImage,Imgproc.COLOR_BGR2GRAY);
        //二值化
        Imgproc.threshold(grayImage, result, 180, 255, Imgproc.THRESH_BINARY);
        String binaryFileName = fileObj.getParentFile().getAbsolutePath()+File.separator+fileObj.getName()+"_temp.png";
        System.out.println(binaryFileName);
        Imgcodecs.imwrite(binaryFileName, result);

        //ocr 识别
        Tesseract tesseract = new InsideTesseract("eng");
        String code = tesseract.doOCR(new File(binaryFileName));

        if(StringUtils.isEmpty(code)){
            //去噪
            BufferedImage images = ImageIO.read(new File(binaryFileName));
            BufferedImage changedImages = removeLine(images,3);
            File removeLineImage = new File(fileObj.getParentFile().getAbsolutePath()+File.separator+fileObj.getName()+"_removeLine.png");
            System.out.println(removeLineImage);
            ImageIO.write(changedImages,"png", removeLineImage);
            code = tesseract.doOCR(removeLineImage);
        }
        return code;
    }

    /**
     * 裁剪
     * @param image
     * @param padding 边距
     */
    public static Mat imageCut(Mat image,int padding) {

        Rectangle rect = getEffectiveRect(image, padding);
        //截图
        Mat sub = image.submat(new Rect((int)rect.getX(),(int)rect.getY(),rect.width,rect.height));
        // 也可以写成 Mat sub = new Mat(image,rect);
        return sub;
    }

    /**
     * 获取有效范围内
     * @param image
     * @param padding
     * @return
     */
    private static Rectangle getEffectiveRect(Mat image, int padding) {
        for (int i = 0; i < image.cols(); i++) {
            for (int i1 = 0; i1 < image.rows(); i1++) {
                image.get(i1,i);
            }
        }
        int posY = 0,posX = 0,lastX = 0,lastY = 0;
        Double pointLast = null;
        for(int y = 0; y< image.rows(); y++){
            for(int x = 0; x< image.cols(); x++){
                double[] clone= image.get(y, x).clone();
                double cb = clone[0];
                if(pointLast == null) {
                    pointLast = cb;
                    continue;
                } else if(pointLast != cb){
                    //像素值发生变化,取最大
                    lastX = lastX > x ? lastX : x;
                    lastY = lastY > y ? lastY : y;
                    if(posX == 0||posX > x){
                        //首次
                        posX = x;
                    }
                    if(posY == 0||posY > y){
                        //首次
                        posY = y;
                    }
                }
                pointLast = cb;
            }
        }
        int width = lastX - posX;
        int height = lastY - posY;
        // 截取的区域：参数,坐标X,坐标Y,截图宽度,截图长度
        Rectangle rect = new Rectangle(posX - padding, posY - padding, width+ padding *2, height+ padding *2);
        return rect;
    }

    private static BufferedImage removeLine(BufferedImage img, int px) {
        if (img != null) {
            int width = img.getWidth();
            int height = img.getHeight();

            for (int x = 0; x < width; x++) {
                List<Integer> list = new ArrayList<Integer>();
                for (int y = 0; y < height; y++) {
                    int count = 0;
                    while (y < height - 1 && isBlack(img.getRGB(x, y))) {
                        count++;
                        y++;
                    }
                    if (count <= px && count > 0) {
                        for (int i = 0; i <= count; i++) {
                            list.add(y - i);
                        }
                    }
                }
                if (list.size() != 0) {
                    for (int i = 0; i < list.size(); i++) {
                        img.setRGB(x, list.get(i), Color.white.getRGB());
                    }
                }
            }
        }
        return img;

    }

    public static boolean isBlack(int rgb){
        Color c = new Color(rgb);
        int b = c.getBlue();
        int r = c.getRed();
        int g = c.getGreen();
        int sum = r+g+b;
        if(sum<10){
            return true;
        }
        return false;
        //sum的值越小（最小为零，黑色）颜色越重，
        //sum的值越大（最大值是225*3）颜色越浅，
        //sum的值小于10就算是黑色了.
    }




    static class InsideTesseract extends Tesseract{

        public InsideTesseract() {
            try {
                String dataPath = new ClassPathResource("tesseract.data").getFile().getAbsolutePath();
                super.setDatapath(dataPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public InsideTesseract(String language) {
            try {
                String dataPath = new ClassPathResource("tesseract.data").getFile().getAbsolutePath();
                super.setDatapath(dataPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.setLanguage(language);
        }


    }
}