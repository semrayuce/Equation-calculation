package com.example.iu.equcalc.imageProc;

import android.graphics.Bitmap;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


public class ImageProccessingService {

    private final String TAG = "ImageProccessingService";

    private ImageProccessingService() {}

    public static Bitmap locallyAdaptiveThreshold(Bitmap gray) {
        Bitmap newBitmap = Bitmap.createBitmap(gray.getWidth(), gray.getHeight(), Bitmap.Config.ARGB_8888);

        Mat mat = new Mat(); //Mat.zeros(imgBitmap.getHeight(), imgBitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(gray, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

        Mat matBW = new Mat(); //Mat.zeros(imgBitmap.getHeight(), imgBitmap.getWidth(), CvType.CV_8UC1);

        // Thresholding için blok boyutu veriyoruz.
        // Bu tek sayı olmalı yoksa openCV exception fırlatacaktır!!!
        int blockSize = 55;
        Imgproc.adaptiveThreshold(mat, matBW, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, 15);
        Utils.matToBitmap(matBW, newBitmap);

        return newBitmap;
        //Thresholding yapabilmek için mat a çevirdik.Yeniden bitmap yapıp return ettik.
    }

    //NV21 formatındaki diziyi görüntünün grayscale'li bitmap'ine dönüştürüyoruz

    public static Bitmap NV21BytesToGrayScaleBitmap(byte[] data, int width, int height) {
        PlanarYUVLuminanceSource lum = new PlanarYUVLuminanceSource(data, width, height,
                0, 0, width, height, false);
        return lum.renderCroppedGreyscaleBitmap();
    }

}
