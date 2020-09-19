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
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class CaptureVideoClass {

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "Camera2VideoFragment";

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private final TextureView previewView;

    Context context;
    MediaRecorder mMediaRecorder;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CaptureRequest mCaptureRequest;
    private Size size;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraCaptureSession cameraCaptureSession;
    private Integer mSensorOrientation;
    private String filePath;

    private String getVideoFilePath() {
        File pasta = new File(Environment.getExternalStorageDirectory(), "/PersonalOrganizer");
        if (!pasta.exists())
            pasta.mkdirs();

        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        currentDateTimeString = currentDateTimeString.toLowerCase().replace(' ', '-');
        currentDateTimeString = currentDateTimeString.replace(",", "");
        filePath = Environment.getExternalStorageDirectory() + "/PersonalOrganizer/picture-" + currentDateTimeString + ".mp4";
        return filePath;
    }

    private StateCallback cameraStateCallback = new StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {

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
            mMediaRecorder.setOrientationHint(SENSOR_ORIENTATION_DEFAULT_DEGREES);

            int rotation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
            switch (mSensorOrientation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }

            try {

                mMediaRecorder.setOutputFile(getVideoFilePath());
                mMediaRecorder.prepare();
                Surface recorderSurface = mMediaRecorder.getSurface();
                List<Surface> list = new ArrayList<>();
                list.add(recorderSurface);
                list.add(textureSurface);
                final CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
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

    public CaptureVideoClass(Context context, TextureView previewView, Size size){
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
        mCameraManager = (CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameras = mCameraManager.getCameraIdList();
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameras[0]);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
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
        sendPostFile();
    }

    public void sendPostFile() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: teste");
                    String fileBytes = getBase64FromPath(filePath);
                    URL url = new URL("http://192.168.2.10:5000/");
                    HttpURLConnection conn = null;
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Content-Type", "video/mp4");
                    conn.setRequestProperty("Content-Length", Integer.toString(fileBytes.length()));
                    conn.setDoOutput(true); //fala que voce vai enviar algo
                    conn.setDoInput(true);
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.writeBytes(fileBytes);
                    wr.flush();
                    wr.close();
                    conn.connect();
                    String jsonDeResposta = new Scanner(conn.getInputStream()).next();
                    Log.d(TAG, "run: " + jsonDeResposta);
                    conn.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public static String getBase64FromPath(String path) {
        String base64 = "";
        try {
            File file = new File(path);
            byte[] buffer = new byte[(int) file.length() + 100];
            int length = new FileInputStream(file).read(buffer);
            base64 = Base64.encodeToString(buffer, 0, length, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return base64;
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