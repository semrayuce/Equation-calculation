package com.example.iu.equcalc;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/* Android dokümanlarından uyarlanmış bir Kamera önizleme sınıfı. */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private String TAG = "CameraPreview";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        initHolder();
    }

    // Install a SurfaceHolder.Callback
    // böylece altta yatan yüzey oluşturulduğunda ve yok edildiğinde haberdar oluruz.
    public void initHolder() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        //Kullanımdan kaldırıldı, ancak 3.0'dan önceki Android sürümlerinde gerekiyor
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        //Surface oluşturuldu, şimdi cameraya önizlemeyi nerede çizeceğini söyleriz.
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Kamera önizlemesi ayarlanırken hata oluştu: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Boş. Activity'nizde Kamera önizlemesini bırakmaya özen gösterin.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Önizleme değişebilir veya döndürülebilirse, burada bu etkinliklerle ilgilen.
        // Yeniden boyutlandırmadan veya yeniden biçimlendirmeden önce önizlemeyi durdurduğunuzdan emin olun.

        if (mHolder.getSurface() == null){
            // Önizleme yüzeyi mevcut değil
            return;
        }

        // Değişiklik yapmadan önce önizlemeyi durdur
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // Yoksay: mevcut olmayan bir önizlemeyi durdurmaya çalıştı
        }

        // Önizleme boyutunu ayarlayın ve herhangi bir yeniden boyutlandırma yapın,
        // döndürün veya yeniden biçimlendirme değişiklikleri yapın

        // Yeni ayarları kullanarak önizlemeyi başlat
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Kamera önizlemesi başlatılırken hata oluştu: " + e.getMessage());
        }
    }
}