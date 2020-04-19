/*
 * Copyright 2009 ZXing authors
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
package com.example.iu.equcalc.imageProc;

import android.graphics.Bitmap;

/**
 * This object extends LuminanceSource around an array of YUV data returned from the camera driver,
 * with the option to crop to a rectangle within the full data. This can be used to exclude
 * superfluous pixels around the perimeter and speed up decoding.
 * <p>
 * Y kanalının düzlemsel olduğu ve ilk göründüğü YCbCr_420_SP ve YCbCr_422_SP de dahil olmak üzere
 * herhangi bir piksel formatı için çalışır.
 * <p>
 * Bu sınıfın kodunu ZXing projesinden uyarladık: http://code.google.com/p/zxing
 */
public final class PlanarYUVLuminanceSource {

    private final byte[] yuvData;
    private final int dataWidth;
    private final int dataHeight;
    private final int left;
    private final int top;
    private final int width;
    private final int height;

    public PlanarYUVLuminanceSource(byte[] yuvData,
                                    int dataWidth,
                                    int dataHeight,
                                    int left,
                                    int top,
                                    int width,
                                    int height,
                                    boolean reverseHorizontal) {


        if (left + width > dataWidth || top + height > dataHeight) {
            throw new IllegalArgumentException("Kırpma dikdörtgeni resim verilerine uymuyor.");
        }

        this.yuvData = yuvData;
        this.dataWidth = dataWidth;
        this.dataHeight = dataHeight;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        if (reverseHorizontal) {
            reverseHorizontal(width, height);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


    public Bitmap renderCroppedGreyscaleBitmap() {//Kırpılmış Gri Tonlama Bitmapi oluştur
        int width = getWidth();
        int height = getHeight();
        int[] pixels = new int[width * height];
        byte[] yuv = yuvData;
        int inputOffset = top * dataWidth + left;

        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            for (int x = 0; x < width; x++) {
                int grey = yuv[inputOffset + x] & 0xff;
                pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += dataWidth;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private void reverseHorizontal(int width, int height) {
        byte[] yuvData = this.yuvData;
        for (int y = 0, rowStart = top * dataWidth + left; y < height; y++, rowStart += dataWidth) {
            int middle = rowStart + width / 2;
            for (int x1 = rowStart, x2 = rowStart + width - 1; x1 < middle; x1++, x2--) {
                byte temp = yuvData[x1];
                yuvData[x1] = yuvData[x2];
                yuvData[x2] = temp;
            }
        }
    }
}
