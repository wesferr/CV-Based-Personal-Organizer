package me.wesferr.personalorganizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FeedbackActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_response);

        Intent intent = getIntent();
        ImageView imageView = findViewById(R.id.image_feedback);

        Bitmap bmp = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/PersonalOrganizer/present.jpg");
        imageView.setImageBitmap(bmp);

    }
}
