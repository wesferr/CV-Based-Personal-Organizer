package me.wesferr.personalorganizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Collections;

public class CameraPreviewClass {

    private Size imageDimentions;
    private TextureView previewView;
    private Context context;
    CameraDevice cameraDevice;
    CameraManager cameraManager;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    String cameraId;

    void setImageDimentions(Size size){
        imageDimentions= size;
    }

    void initPreview(Context context, TextureView previewView, Size size) {
        this.context = context;
        this.previewView = previewView;
        this.imageDimentions = size;
        previewView.setSurfaceTextureListener(previewListener);
    }

    void resumePreview(){
        this.startBackgroundThread();
        if(previewView.isAvailable()){
            setupCamera();
            openCamera();
        } else {
            previewView.setSurfaceTextureListener(previewListener);
        }
    }

    void pausePreview(){
        if(cameraCaptureSession != null){
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if(cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
        this.stopBackgroundThread();
    }

    //Configura os callbacks do listener da textura, abrindo a camera quando a superficie estiver pronta
    private TextureView.SurfaceTextureListener previewListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera();
            openCamera();
            configureTransform(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    void setupCamera() {

        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    //carrega a camera
    @SuppressLint("MissingPermission")
    void openCamera() {

        try {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            try {
                createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    void createCameraPreview() throws CameraAccessException {

        SurfaceTexture texture = previewView.getSurfaceTexture();
        texture.setDefaultBufferSize(imageDimentions.getWidth(), imageDimentions.getHeight());
        Surface surface = new Surface(texture);
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 5.0f);
        captureRequestBuilder.addTarget(surface);
        cameraDevice.createCaptureSession(Collections.singletonList(surface), captureStateCallback, backgroundHandler);

    }

    private void configureTransform(int viewWidth, int viewHeight) {

        int rotation = ((AppCompatActivity)context).getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, imageDimentions.getHeight(), imageDimentions.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / imageDimentions.getHeight(),
                    (float) viewWidth / imageDimentions.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        previewView.setTransform(matrix);
    }

    private CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (cameraDevice == null) {
                return;
            }
            cameraCaptureSession = session;
            try {
                updatePreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(context.getApplicationContext(), "Configuração Alterada", Toast.LENGTH_LONG).show();
        }
    };

    private void updatePreview() throws CameraAccessException {
        if (cameraDevice == null) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
    }

    private void startBackgroundThread() {
        this.backgroundThread = new HandlerThread("camera background");
        this.backgroundThread.start();
        this.backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {

        this.backgroundThread.quitSafely();
        try {
            this.backgroundThread.join();
            this.backgroundThread = null;
            this.backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
