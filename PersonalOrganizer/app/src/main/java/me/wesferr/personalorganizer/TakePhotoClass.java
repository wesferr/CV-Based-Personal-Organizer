package me.wesferr.personalorganizer;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static androidx.core.content.ContextCompat.getSystemService;

public class TakePhotoClass {

    final String TAG = "CAMERALOG";
    Context context;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    CaptureRequest.Builder captureBuilder;
    CameraPreviewClass cameraPreview;
    File file;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    public void setupCallback(Context context) {
        this.context = context;
    }

    protected void takePicture(CameraPreviewClass cameraPreview) {
        this.cameraPreview = cameraPreview;
        if(null == cameraPreview.cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraPreview.cameraDevice.getId());
            int rotation = ((AppCompatActivity)context).getWindowManager().getDefaultDisplay().getRotation();
            int width = 640;
            int height = 480;


            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }

            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());

            captureBuilder = cameraPreview.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            File pasta = new File(Environment.getExternalStorageDirectory(), "/PersonalOrganizer");
            if (!pasta.exists())
                pasta.mkdirs();

            file = new File(Environment.getExternalStorageDirectory() + "/PersonalOrganizer/picture-" + UUID.randomUUID().toString() + ".jpg");
            reader.setOnImageAvailableListener(readerListener, backgroundHandler);

            cameraPreview.cameraDevice.createCaptureSession(outputSurfaces, captureStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    final CameraCaptureSession.StateCallback captureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                session.capture(captureBuilder.build(), captureListener, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    };

    final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(context.getApplicationContext(), "Saved:" + file, Toast.LENGTH_SHORT).show();
            try {
                cameraPreview.createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    };

    ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                save(bytes);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
        private void save(byte[] bytes) throws IOException {
            OutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
            } finally {
                if (null != output) {
                    output.close();
                }
            }
        }
    };

}
