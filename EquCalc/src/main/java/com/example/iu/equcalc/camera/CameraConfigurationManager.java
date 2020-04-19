/*
 * Copyright (C) 2010 ZXing authors
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.iu.equcalc.camera;


import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Bu sınıf Kamerayı yapılandırmak için kullanılan kamera parametrelerinin okunması, ayrıştırılması ve ayarlanması
 * ile ilgilidir.
 * <p>
 * Bu sınıfın kodunu ZXing projesinden uyarladık: http://code.google.com/p/zxing
 */
public final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";
    // Halen desteklenen küçük bir ekranın boyutundan daha büyük
    // Aşağıdaki rutin bunlar için varsayılan (muhtemelen 320x240) boyutu seçecektir.
    // Bu şekilde bazı cihazlarda yanlışlıkla çok düşük çözünürlük seçilmesini önlüyoruz

    private static final int MIN_PREVIEW_PIXELS = 470 * 320; // normal ekran
    private static final int MAX_PREVIEW_PIXELS = 800 * 600; // Daha büyük / HD ekran.

    private Context context;
    private Point screenResolution;
    private Point cameraResolution;

    public CameraConfigurationManager(Context context) {
        this.context = context;
    }

    //Kamera nesnesinin instance'sini almanın güvenli bir yolu
    public Camera getCameraInstance(String focusMode, int previewFormat) {
        Camera c = null;
        try {
            c = Camera.open(); //Camera instance'sini almaya çalış

            initFromCameraParameters(c);
            setDesiredCameraParameters(c, focusMode, previewFormat);

            // Kamera parametrelerini ayarla...
            Camera.Parameters params = c.getParameters();
            params.setFocusMode(focusMode);
            params.setPreviewFormat(previewFormat);

            c.setParameters(params);
        } catch (Exception e) {
            // Kamera kullanılamıyor (kullanımdayken veya yoksa)
        }
        return c; // Kamera kullanılamıyorsa null döndürür
    }

    //Uygulama tarafından ihtiyaç duyulan değerleri kamera bir kereliğine okuyacaktır
    public void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        screenResolution = new Point(width, height);
        Log.i(TAG, "Ekran çözünürlüğü: " + screenResolution);
        cameraResolution = findBestPreviewSizeValue(parameters, screenResolution);
        Log.i(TAG, "Kamera çözünürlüğü: " + cameraResolution);
    }

    private void setDesiredCameraParameters(Camera camera, String focusMode, int previewFormat) {
        Camera.Parameters parameters = camera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Cihaz hatası: Kamera parametresi yok. Yapılandırmadan devam et.");
            return;
        }

        parameters.setFocusMode(focusMode);
        parameters.setPreviewFormat(previewFormat);
        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
        camera.setParameters(parameters);
    }

    public Point getCameraResolution() {
        return cameraResolution;
    }

    Point getScreenResolution() {
        return screenResolution;
    }

    private Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

        // Azalan boyuta göre sırala(sort by descending)
        List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes());
        //Desteklenen preview size'leri azalan sırada listeye atıyoruz
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
                previewSizesString.append(supportedPreviewSize.width).append('x')
                        .append(supportedPreviewSize.height).append(' ');
            }
            Log.i(TAG, "Desteklenen önizleme boyutları: " + previewSizesString);
        }

        Point bestSize = null;
        float screenAspectRatio = (float) screenResolution.x / (float) screenResolution.y;

        float diff = Float.POSITIVE_INFINITY;
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            int pixels = realWidth * realHeight;
            if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
                continue;
            }
            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.i(TAG, "Önizleme boyutunu tam olarak eşleşen ekran boyutunda buldu: " + exactPoint);
                return exactPoint;
            }
            float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
            float newDiff = Math.abs(aspectRatio - screenAspectRatio);
            if (newDiff < diff) {
                bestSize = new Point(realWidth, realHeight);
                diff = newDiff;
            }
        }

        if (bestSize == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            bestSize = new Point(defaultSize.width, defaultSize.height);
            Log.i(TAG, "Varsayılan kullanarak uygun önizleme boyutları bulunmamakta: " + bestSize);
        }
        Log.i(TAG, "En iyi yaklaşık önizleme boyutu bulundu: " + bestSize);
        return bestSize;
    }

}
