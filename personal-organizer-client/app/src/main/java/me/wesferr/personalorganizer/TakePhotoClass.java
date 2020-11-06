package me.wesferr.personalorganizer;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TakePhotoClass {

    final String TAG = "CAMERALOG";
    Context context;
    Handler backgroundHandler;
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

    protected void takePicture(CameraPreviewClass cameraPreview, Size size) {
        this.cameraPreview = cameraPreview;
        if(null == cameraPreview.cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            manager.getCameraCharacteristics(cameraPreview.cameraDevice.getId());
            int rotation = ((AppCompatActivity)context).getWindowManager().getDefaultDisplay().getRotation();
            ImageReader reader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());

            captureBuilder = cameraPreview.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

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

            file = new File(Environment.getExternalStorageDirectory() + "/PersonalOrganizer/picture-" + currentDateTimeString + ".jpg");
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
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    private final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
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
            try (Image image = reader.acquireLatestImage()) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                save(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void save(byte[] bytes) throws IOException {
            try (OutputStream output = new FileOutputStream(file)) {
                output.write(bytes);
            }
        }
    };

}
