package me.wesferr.personalorganizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodec;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    String TAG = "CAMERALOG";
    TextureView previewView;
    String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
    };

    final int CAMERA_REQUEST_CODE = 0x1;
    final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 0x2;
    final int ACCESS_WIFI_STATE_REQUEST_CODE = 0x4;
    final int CHANGE_WIFI_STATE_REQUEST_CODE = 0x8;
    final int ACCESS_FINE_LOCATION_STATE_REQUEST_CODE = 0x10;
    final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 0x20;
    final int REQUEST_CODE_REQUEST_CODE = 0x40;
    final int INTERNET_REQUEST_CODE = 0x80;
    final int ACCESS_NETWORK_STATE_REQUEST_CODE = 0x100;

    // TODO: Request permission to microphone

    final int permissions_request_codes =
        CAMERA_REQUEST_CODE +
        WRITE_EXTERNAL_STORAGE_REQUEST_CODE +
        ACCESS_WIFI_STATE_REQUEST_CODE +
        CHANGE_WIFI_STATE_REQUEST_CODE +
        ACCESS_FINE_LOCATION_STATE_REQUEST_CODE +
        READ_EXTERNAL_STORAGE_REQUEST_CODE +
        REQUEST_CODE_REQUEST_CODE +
        INTERNET_REQUEST_CODE +
        ACCESS_NETWORK_STATE_REQUEST_CODE;


    int recording = 0;
    CameraPreviewClass previewClass;

    TakePhotoClass photoClass;

    CaptureVideoClass videoClass;
    Button capturebutton;
    Size[] output_sizes;
    Size size;
    String words;
    SensorsScanner sensorsScanner;

    public class RequestSender implements Runnable{

        Context context;
        File file;
        String mediatype;
        String endpoint;
        ResponseBody body = null;

        RequestSender(Context context, File file, String mediatype, String endpoint){
            this.context = context;
            this.file = file;
            this.mediatype = mediatype;
            this.endpoint = endpoint;
        }

        @Override
        public void run() {


            JSONObject extra_data = new JSONObject();

            while (!sensorsScanner.azimuth_ready()){
                Log.i(TAG, "run: waiting");
            }

            try {
                extra_data.put("wireless", sensorsScanner.get_wifi_list());
                extra_data.put("bussola", sensorsScanner.get_azimuth());
                extra_data.put("words", words);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            URL url = null;
            try {
                url = new URL("http://192.168.2.10:5000" + endpoint);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            assert url != null;

            MultipartBody.Builder form = new MultipartBody.Builder().setType(MultipartBody.FORM);
            form.addFormDataPart("file", file.getName(), MultipartBody.create(file, MediaType.parse(mediatype)));
            form.addFormDataPart("extra_data", extra_data.toString());

            Request request = new Request.Builder()
                    .header("Content-Type", "multipart/form-data")
                    .url(url).post(form.build()).build();

            OkHttpClient client = new OkHttpClient();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {

                    if(!response.isSuccessful()){
                        Toast.makeText(context, "Desculpe, problema com o envio", Toast.LENGTH_LONG).show();
                    } else {

                        body = response.body();
                        assert body != null;
                        if(body.contentType().toString().equals("image/jpeg")){
                            getResponseImage();
                        }

                        if(body.contentType().toString().equals("application/json")){

                        }

                    }


                }

                void getResponseImage(){
                    InputStream bytes = body.byteStream();
                    OutputStream image = null;
                    try {
                        image = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/PersonalOrganizer/present.jpg"));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    try {
                        byte[] data = new byte[1024];

                        long total = 0;
                        int count;
                        while ((count = bytes.read(data)) != -1) {
                            total += count;
                            image.write(data, 0, count);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        image.flush();
                        image.close();
                        bytes.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(context, FeedbackActivity.class);
                    startActivity(intent);
                }

            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //carrega a texture view para a previsualização
        previewView = findViewById(R.id.textureView);
        capturebutton = findViewById(R.id.take_photo2);


        Log.i(TAG, "0.0.028");
        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[1]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[2]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[3]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[4]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[5]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[6]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[7]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[8]) != PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this, permissions, permissions_request_codes);

        } else {

            permissionsSuccess();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: "+ requestCode);

        boolean permissions_granted =
                grantResults.length > 5 &&
                grantResults[0] == PERMISSION_GRANTED &&
                grantResults[1] == PERMISSION_GRANTED &&
                grantResults[2] == PERMISSION_GRANTED &&
                grantResults[3] == PERMISSION_GRANTED &&
                grantResults[4] == PERMISSION_GRANTED &&
                grantResults[5] == PERMISSION_GRANTED &&
                grantResults[6] == PERMISSION_GRANTED &&
                grantResults[7] == PERMISSION_GRANTED &&
                grantResults[8] == PERMISSION_GRANTED;

        if (requestCode == permissions_request_codes ) {
            if ( permissions_granted ) {
                permissionsSuccess();
            } else {
                permissionsFailed();
            }
        }
    }

    protected void setResolutionChoices() throws CameraAccessException {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String cameraId = cameraManager.getCameraIdList()[0];
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert configs != null;
        output_sizes = configs.getOutputSizes(MediaCodec.class);

        final Spinner spinner = findViewById(R.id.combobox);
        final List<Size> sizes = new ArrayList<>();
        Collections.addAll(sizes, output_sizes);
        ArrayAdapter<Size> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizes );
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        for (int i = 0; i < sizes.size(); i++) {
            if(sizes.get(i).getWidth() == 1920 && sizes.get(i).getHeight() == 1080){
                size = sizes.get(i);
                spinner.setSelection(i);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                size = sizes.get(position);
                previewClass.setImageDimentions(size);
                previewClass.pausePreview();
                previewClass.resumePreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    void createAlert(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Buscar");
        alert.setMessage("Digite as palavras separadas por ';'");

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                words = input.getText().toString();
                new Thread(new RequestSender(getApplicationContext(), photoClass.file, "image/jpg", "/search")).start();
            }
        });

        alert.show();
    }

    protected void permissionsSuccess() {

        try {
            setResolutionChoices();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        previewClass = new CameraPreviewClass();
        previewClass.initPreview(this, previewView, size);

        videoClass = new CaptureVideoClass(this, previewView, size);

        photoClass = new TakePhotoClass();
        photoClass.setupCallback(this);
        sensorsScanner = new SensorsScanner(getApplicationContext());

        Button buttonPhoto = findViewById(R.id.take_photo);
        buttonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAlert();
                photoClass.takePicture(previewClass, size);
            }
        });

        capturebutton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                if(recording == 0){
                    previewClass.pausePreview();
                    videoClass.startRecordingVideo();
                    capturebutton.setText("Parar");
                    recording = 1;
                } else {
                    videoClass.stopRecordingVideo();
                    words = "";
                    new Thread(new RequestSender(getApplicationContext(), videoClass.file, "image/mp4", "")).start();
                    capturebutton.setText("Gravar");
                    previewClass.resumePreview();
                    recording = 0;
                }
            }
        });

    }

    protected void permissionsFailed(){
        Toast.makeText(this, "Desculpe, todas as permissões são necessarias", Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onResume(){
        super.onResume();
        if (previewClass!=null){
            previewClass.resumePreview();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (previewClass!=null){
            previewClass.pausePreview();
        }
        if(recording == 1){
            videoClass.stopRecordingVideo();
        }
        super.onPause();
    }

}
