package com.example.facerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.facerecognition.FaceDetection.Box;
import com.example.facerecognition.FaceDetection.MTCNN;
import com.example.facerecognition.FaceRecognition.FaceNet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private final String[] imageSourceDisplay = new String[]{"From Pictures", "From Camera"};

    private static final int PICK_IMAGEVIEW_CONTENT = 1;
    private static final int PICK_IMAGEVIEW2_CONTENT = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_IMAGE2_CAPTURE = 4;

    private Bitmap image1;
    private Bitmap image2;
    private ImageView imageView1;
    private ImageView imageView2;
    private Button processBtn;
    private TextView textView;
    private RelativeLayout loading;

    private Bitmap cropFace(Bitmap bitmap, MTCNN mtcnn){
        Bitmap croppedBitmap = null;
        try {
            Vector<Box> boxes = mtcnn.detectFaces(bitmap, 10);

            Log.i("MTCNN", "No. of faces detected: " + boxes.size());

            //Not Used
            // int left = boxes.get(0).left();
            // int top = boxes.get(0).top();

            int x = boxes.get(0).left();
            int y = boxes.get(0).top();
            int width = boxes.get(0).width();
            int height = boxes.get(0).height();


            if (y + height >= bitmap.getHeight())
                height -= (y + height) - (bitmap.getHeight() - 1);
            if (x + width >= bitmap.getWidth())
                width -= (x + width) - (bitmap.getWidth() - 1);

            Log.i("MTCNN", "Final x: " + (x + width));
            Log.i("MTCNN", "Width: " + bitmap.getWidth());
            Log.i("MTCNN", "Final y: " + (y + width));
            Log.i("MTCNN", "Height: " + bitmap.getWidth());

            croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height);
        }catch (Exception e){
            e.printStackTrace();
        }
        return croppedBitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView1 = findViewById(R.id.imageView1);
        imageView2 = findViewById(R.id.imageView2);
        processBtn = findViewById(R.id.processBtn);
        textView = findViewById(R.id.textView);
        loading = findViewById(R.id.loadingPanel);

        loading.setVisibility(View.GONE);

        imageView1.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select a source");
            builder.setItems(imageSourceDisplay, (dialogInterface, i) -> {
                if (i == 0){
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, PICK_IMAGEVIEW_CONTENT);
                } else {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            });
            builder.show();
        });

        imageView2.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select a source");
            builder.setItems(imageSourceDisplay, (dialogInterface, i) -> {
                if (i == 0){
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, PICK_IMAGEVIEW2_CONTENT);
                } else {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE2_CAPTURE);
                    }
                }
            });
            builder.show();
        });

        processBtn.setOnClickListener(view -> {
            processBtn.setEnabled(false);
            textView.setText("");
            loading.setVisibility(View.VISIBLE);

            if (image1 == null || image2 == null){
                Toast.makeText(getApplicationContext(), "One of the images haven't been set yet.", Toast.LENGTH_SHORT).show();
                processBtn.setEnabled(true);
                loading.setVisibility(View.GONE);
            }else {
                Runnable r = () -> {
                    MTCNN mtcnn = new MTCNN(getAssets());

                    Bitmap face1 = cropFace(image1, mtcnn);
                    Bitmap face2 = cropFace(image2, mtcnn);

                    mtcnn.close();

                    FaceNet facenet = null;
                    try {
                        facenet = new FaceNet(getAssets());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    MainActivity.this.runOnUiThread(() -> {
                        if (face1 != null)
                            imageView1.setImageBitmap(face1);
                        else
                            Log.i("detect", "Couldn't crop image 1.");

                        if (face2 != null)
                            imageView2.setImageBitmap(face2);
                        else
                            Log.i("detect", "Couldn't crop image 2.");
                    });

                    if (face1 != null && face2 != null) {
                        double score = 0;
                        if (facenet != null) {
                            score = facenet.getSimilarityScore(face1, face2);
                        }

                        Log.i("score", String.valueOf(score));
                        //Toast.makeText(MainActivity.this, "Similarity score: " + score, Toast.LENGTH_LONG).show();
                        String text = String.format(Locale.US, "Similarity score = %.2f", score);
                        MainActivity.this.runOnUiThread(() -> textView.setText(text));
                    }

                    if (facenet != null) {
                        facenet.close();
                    }
                    MainActivity.this.runOnUiThread(() -> {

                        processBtn.setEnabled(true);
                        loading.setVisibility(View.GONE);
                    });

                };
                Thread th = new Thread(r);
                th.start();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGEVIEW_CONTENT && resultCode == RESULT_OK) {
            try {
                Uri imageUri = null;
                if (data != null) {
                    imageUri = data.getData();
                }
                InputStream imageStream = null;
                if (imageUri != null) {
                    imageStream = getContentResolver().openInputStream(imageUri);
                }
                image1 = BitmapFactory.decodeStream(imageStream);
                float scale = (float) 1000 / image1.getWidth();
                image1 = Bitmap.createScaledBitmap(image1, (int)(image1.getWidth()*scale), (int)(image1.getHeight()*scale), true);
                imageView1.setImageBitmap(image1);
                textView.setText("");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error loading gallery image.", Toast.LENGTH_LONG).show();
            }
        }else if (requestCode == PICK_IMAGEVIEW2_CONTENT && resultCode == RESULT_OK) {
            try {
                Uri imageUri = null;
                if (data != null) {
                    imageUri = data.getData();
                }
                InputStream imageStream = null;
                if (imageUri != null) {
                    imageStream = getContentResolver().openInputStream(imageUri);
                }
                image2 = BitmapFactory.decodeStream(imageStream);
                float scale = (float) 1000 / image2.getWidth();
                image2 = Bitmap.createScaledBitmap(image2, (int)(image2.getWidth()*scale), (int)(image2.getHeight()*scale), true);
                imageView2.setImageBitmap(image2);
                textView.setText("");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error loading gallery image.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = null;
            if (data != null) {
                extras = data.getExtras();
            }
            Bitmap imageBitmap = null;
            if (extras != null) {
                imageBitmap = (Bitmap) extras.get("data");
            }
            float scale;
            if (imageBitmap != null) {
                scale = (float) 1000 / imageBitmap.getWidth();
                image1 = Bitmap.createScaledBitmap(imageBitmap, (int)(imageBitmap.getWidth()*scale), (int)(imageBitmap.getHeight()*scale), true);
                Matrix matrix = new Matrix();
                matrix.postRotate(270);
                image1 = Bitmap.createBitmap(image1, 0, 0, image1.getWidth(), image1.getHeight(), matrix, true);

            }
            imageView1.setImageBitmap(image1);
        } else if (requestCode == REQUEST_IMAGE2_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = null;
            if (data != null) {
                extras = data.getExtras();
            }
            Bitmap imageBitmap = null;
            if (extras != null) {
                imageBitmap = (Bitmap) extras.get("data");
            }
            float scale;
            if (imageBitmap != null) {
                scale = (float) 1000 / imageBitmap.getWidth();
                image2 = Bitmap.createScaledBitmap(imageBitmap, (int)(imageBitmap.getWidth()*scale), (int)(imageBitmap.getHeight()*scale), true);
                Matrix matrix = new Matrix();
                matrix.postRotate(270);
                image2 = Bitmap.createBitmap(image2, 0, 0, image2.getWidth(), image2.getHeight(), matrix, true);
            }
            imageView2.setImageBitmap(image2);
        }
    }

}
