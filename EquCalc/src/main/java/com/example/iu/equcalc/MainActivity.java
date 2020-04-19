package com.example.iu.equcalc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.hardware.camera2.CaptureResult;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect;

import java.io.File;
import java.util.List;

import com.example.iu.equcalc.camera.CameraConfigurationManager;

import static com.example.iu.equcalc.imageProc.ImageProccessingService.NV21BytesToGrayScaleBitmap;
import static com.example.iu.equcalc.imageProc.ImageProccessingService.locallyAdaptiveThreshold;

public class MainActivity extends Activity {

    private String TAG = "MainActivity";

    // Kameranın önizleme yapıp yapmadığını kontrol ediyor
    boolean isCameraPreviewing = true;

    // Kameradan önizleme çerçevesi almak için
    private CameraPreviewCallback cameraPreviewCB;

    private class CameraPreviewCallback implements Camera.PreviewCallback {

        public CameraPreviewCallback() {
        }

        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            handlePreviewFrame(bytes, camera);
        }
    }

    private static Camera mCamera;
    private CameraConfigurationManager configManager;
    private final String focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

    // Önizleme için kullanacağımız resim formatını belirtiyoruz
    private int previewFormat = ImageFormat.NV21; // SetPreviewFormat (int) ile başka türlü ayarlanmadığında,
    // Kamera önizleme görüntülerinin varsayılan biçimi budur.

    // Görüntü Bileşenleri
    private FrameLayout previewFrame;
    private CameraPreview mPreview;
    private Button captureButton;
    private ImageView pictureView;

    // En son çekilen resim frame'i ile ilgili bilgi
    private Bitmap currentFrame;
    private Bitmap currentFrameRaw; //işlenmemiş frame
    private Bitmap currentFrameBW; //siyah ve beyaz hali (black and white)
    int currentFrameWidth;
    int currentFrameHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Uygulamayı açık tutar
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // OpenCv'nin yüklenip yüklenmediğinin kontrolü
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mOvenCVLoaderCallback)) {
            Toast toast = Toast.makeText(getApplicationContext(), "Failed to load OpenCV! Check that you have OpenCV Manager.", Toast.LENGTH_LONG);
            toast.show();
        }
        //Burda OpenCv Manager yüklü değilse toast mesajı gösteriyoruz,OpenCV kendisi onu indirmeye yönlendiriyor
        setupCameraPreview();
    }

    //OpenCV Yöneticisini kullanarak OpenCV'yi yüklemek için çağrı yapıyoruz
    private BaseLoaderCallback mOvenCVLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                default:
                    // Handle error after callback
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void setupCameraPreview() {
        // Kamera örneğini oluşturma
        configManager = new CameraConfigurationManager(this);
        mCamera = configManager.getCameraInstance(focusMode, previewFormat);
        cameraPreviewCB = new CameraPreviewCallback();

        //Önizleme görünümümüzü oluştuyoruz ve activity'imizin içeriği olarak ayarlıyoruz.
        mPreview = new CameraPreview(this, mCamera);
        previewFrame = (FrameLayout) findViewById(R.id.camera_preview);
        previewFrame.addView(mPreview);

        pictureView = (ImageView) findViewById(R.id.picture_view);

        // Kamera butonuna bir listener ekliyoruz.
        captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // kameradan bir resim al
                        // mCamera.takePicture(null, null, mPictureCB);

                        // Kameradan bir frame isteği
                        mCamera.setOneShotPreviewCallback(cameraPreviewCB);
                    }
                }
        );
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_CAMERA:
                    mCamera.setOneShotPreviewCallback(cameraPreviewCB);
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    // Görüntüyü temsil eden NV21 formatında işlenmemiş bayt dizisinden başlayarak
    // tüm işlem adımlarını gerçekleştireceğiz

    public void handlePreviewFrame(byte[] data, Camera camera) {

        // Görüntüyü ekrana yerleştirebilmek için Kamera Arayüzünü gizliyoruz
        hideCameraPreview();

        Point cameraResolution = configManager.getCameraResolution();

        currentFrameWidth = cameraResolution.x;
        currentFrameHeight = cameraResolution.y;

        currentFrameRaw = NV21BytesToGrayScaleBitmap(data, currentFrameWidth, currentFrameHeight);
        //int[] pixels = convertYUV420_NV21toRGB8888(data, width, height);
        //currentFrameRaw = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        currentFrame = currentFrameRaw;
        currentFrameBW = locallyAdaptiveThreshold(currentFrame);

        // Yakalanan frame'i göster
        pictureView.setImageBitmap(currentFrameBW);
        pictureView.setVisibility(View.VISIBLE);
    }

    private void hideCameraPreview() {
        previewFrame.setVisibility(View.GONE);
        captureButton.setVisibility(View.GONE);
        isCameraPreviewing = false;
    }

    private void showCameraPreview() {
        previewFrame.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.VISIBLE);
        isCameraPreviewing = true;
    }

    @Override
    public void onBackPressed() {
        if (isCameraPreviewing) {
            super.onBackPressed();
        } else {
            // Return to the camera preview
            pictureView.setVisibility(View.GONE);
            mCamera.startPreview();
            showCameraPreview();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");

        // Diğer kamera tabanlı uygulamalarla çakışmamak için kamerayı kullanmayı bırakmalıyız
        // Kamerayı serbest bırakmadan önce  "method called after release()" hatasını önlemek için PreviewCallback 'i null yapıyoruz
        if (mCamera != null) {
            mCamera.stopPreview();

            previewFrame.removeView(mPreview);
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        if (mCamera == null) {
            Log.d(TAG, "re-initializing camera");
            setupCameraPreview();
        }
    }
}
