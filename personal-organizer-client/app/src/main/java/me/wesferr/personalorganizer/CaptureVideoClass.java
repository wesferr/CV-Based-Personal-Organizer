package me.wesferr.personalorganizer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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

import org.json.JSONException;
import org.json.JSONObject;

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

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.SENSOR_SERVICE;

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

    float azimuth = 0;
    SensorManager sensorManager = null;
    Sensor accelerometer, magnetometer;

    float[] accelerometerVector = new float[3];
    float[] magnetometerVector = new float[3];
    float[] rotation = new float[9];
    float[] inclination = new float[9];
    private WifiManager wifiManager;

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

        sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(mSensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(mSensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_GAME);

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

                    wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    ArrayList<ScanResult> redes = (ArrayList<ScanResult>) wifiManager.getScanResults();

                    ArrayList<String> string_redes = new ArrayList<String>();
                    for(ScanResult rede: redes){
                        string_redes.add("{\"SSID\": \"" + rede.SSID + "\"; \"BSSID\": \"" + rede.BSSID + "\"; \"LEVEL\": " + rede.level + "}");
                    }

                    JSONObject extra_data = new JSONObject();
                    extra_data.put("wireless", string_redes);
                    extra_data.put("bussola", azimuth);


                    Log.d(TAG, "run: teste");
                    File file = new File(filePath);
                    URL url = new URL("http://192.168.2.10:5000/");

                    MultipartBody.Builder form = new MultipartBody.Builder().setType(MultipartBody.FORM);
                    form.addFormDataPart("image","file.mp4", MultipartBody.create(file, MediaType.parse("video/mp4")));
                    form.addFormDataPart("extra_data", extra_data.toString());

                    Request request = new Request.Builder()
                            .header("Content-Type", "multipart/form-data")
                            .url(url).post(form.build()).build();

                    OkHttpClient client = new OkHttpClient();
                    Call call = client.newCall(request);
                    Response response = call.execute();

                } catch (IOException | JSONException e) {
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

    private SensorEventListener mSensorEventListener = new SensorEventListener() {

        float[] orientation = new float[3];
        float[] rMat = new float[9];

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float alpha = 0.97f;
            synchronized (this) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                    accelerometerVector[0] = alpha * accelerometerVector[0] + (1 - alpha) * event.values[0];
                    accelerometerVector[1] = alpha * accelerometerVector[1] + (1 - alpha) * event.values[1];
                    accelerometerVector[2] = alpha * accelerometerVector[2] + (1 - alpha) * event.values[2];

                }

                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                    magnetometerVector[0] = alpha * magnetometerVector[0] + (1 - alpha) * event.values[0];
                    magnetometerVector[1] = alpha * magnetometerVector[1] + (1 - alpha) * event.values[1];
                    magnetometerVector[2] = alpha * magnetometerVector[2] + (1 - alpha) * event.values[2];

                }
            }

            boolean success = SensorManager.getRotationMatrix(rotation, inclination, accelerometerVector, magnetometerVector);

            if(success) {

                float orientation[] = new float[3];
                SensorManager.getOrientation(rotation, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                azimuth = (azimuth + 360) % 360;

            }
        }

    };
}