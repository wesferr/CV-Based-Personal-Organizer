package me.wesferr.personalorganizer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CaptureVideoClass {

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private final TextureView previewView;

    Context context;
    MediaRecorder mMediaRecorder;

    private CameraDevice mCameraDevice;
    private CaptureRequest mCaptureRequest;
    private Size size;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraCaptureSession cameraCaptureSession;
    private String filePath;
    File file;
    SensorsScanner sensorsScanner;

    private String getVideoFilePath() {
        File pasta = new File(Environment.getExternalStorageDirectory(), "/PersonalOrganizer");
        if (!pasta.exists()){
            boolean resultado = pasta.mkdirs();
            if(!resultado){
                try {
                    throw new Exception(("Sem resultado"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        currentDateTimeString = currentDateTimeString.toLowerCase().replace(' ', '-');
        currentDateTimeString = currentDateTimeString.replace(",", "");
        filePath = Environment.getExternalStorageDirectory() + "/PersonalOrganizer/picture-" + currentDateTimeString + ".mp4";
        return filePath;
    }

    private StateCallback cameraStateCallback = new StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            SurfaceTexture texture = previewView.getSurfaceTexture();
            texture.setDefaultBufferSize(size.getWidth(), size.getHeight());
            Surface textureSurface = new Surface(texture);

            mCameraDevice = camera;
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());

            int rotation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
            mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));

            try {

                mMediaRecorder.setOutputFile(getVideoFilePath());
                mMediaRecorder.prepare();
                Surface recorderSurface = mMediaRecorder.getSurface();
                List<Surface> list = new ArrayList<>();
                list.add(recorderSurface);
                list.add(textureSurface);
                final CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                captureRequest.set(CaptureRequest.LENS_FOCUS_DISTANCE, 5.0f);
                captureRequest.addTarget(recorderSurface);
                captureRequest.addTarget(textureSurface);
                mCaptureRequest = captureRequest.build();
                mCameraDevice.createCaptureSession(list, captureStateCallback, backgroundHandler);

            } catch (CameraAccessException | IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            closeCameraDevice();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            closeCameraDevice();
        }
    };

    CaptureVideoClass(Context context, TextureView previewView, Size size){
        sensorsScanner = new SensorsScanner(context);
        this.context = context;
        this.previewView = previewView;
        this.size = size;
    }


    private CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mMediaRecorder.start();
            if (mCameraDevice == null) {
                return;
            }
            cameraCaptureSession = session;
            try {
                cameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(context.getApplicationContext(), "Configuração Alterada", Toast.LENGTH_LONG).show();
        }
    };

    @SuppressLint("MissingPermission")
    void startRecordingVideo() {

        this.startBackgroundThread();
        Activity activity = (Activity) context;
        CameraManager mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameras = mCameraManager.getCameraIdList();
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameras[0]);
            mCameraManager.openCamera(cameras[0], cameraStateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @MainThread private
    void closeCameraDevice() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        this.stopBackgroundThread();
    }

    void stopRecordingVideo() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        try {
            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        file = new File(filePath);
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