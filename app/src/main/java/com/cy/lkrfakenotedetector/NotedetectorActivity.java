package com.cy.lkrfakenotedetector;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cy.lkrfakenotedetector.ml.ModelFakenote;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class NotedetectorActivity extends AppCompatActivity {

    private ImageView mImg1;
    private TextView mTitale;
    private TextView mDis;

    int imageSize = 224;
    private Bitmap img;


    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PICK_IMAGE = 2;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notedetector);

        mImg1 = (ImageView) findViewById(R.id.img1);
        mTitale = (TextView) findViewById(R.id.titale);
        mDis = (TextView) findViewById(R.id.Dis);

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // startActivityForResult(cameraIntent, 1);

            Intent Cpik = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(Cpik, 200);

        } else {
            Intent Cpik = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(Cpik, 2);
        }

    }

    public void classifyImage(Bitmap image){

        try {
            ModelFakenote model = ModelFakenote.newInstance(NotedetectorActivity.this);

            //loads the image into a ByteBuffer and processes it using the model
            //creates a TensorBuffer called inputFeature0 with a specific size (1, 224, 224, 3) and data type (FLOAT32).
            //it input for model
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            // get 1D array of 224 * 224 pixels in image
            int [] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            //Load the Input Data
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            //ML model classifies the input image
            ModelFakenote.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            //extracts the confidence scores for each class
            float[] confidences = outputFeature0.getFloatArray();

            //classification results are displayed using a bar chart


            String[] classes = {"20", "50", "100", "500", "1000", "5000", "other"};


            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for(int i = 0; i < confidences.length; i++){
                if(confidences[i] > maxConfidence){
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            //result.setText(classes[maxPos]);

           // mTitale.setText(classes[maxPos]+"");

            //Control UI Elements Based on Classification
           if (classes[maxPos].equals("20")){
               mTitale.setText("\uD83C\uDDF1\uD83C\uDDF0 Rs. 20/=");


            } else if (classes[maxPos].equals("50")){
               mTitale.setText("\uD83C\uDDF1\uD83C\uDDF0 Rs. 50/=");

            }else if (classes[maxPos].equals("100")){
               mTitale.setText("\uD83C\uDDF1\uD83C\uDDF0 Rs. 100/=");

            }else if (classes[maxPos].equals("500")) {
               mTitale.setText("\uD83C\uDDF1\uD83C\uDDF0 Rs. 500/=");

            }else if (classes[maxPos].equals("1000")) {
               mTitale.setText("\uD83C\uDDF1\uD83C\uDDF0 Rs. 1000/=");

            }else if (classes[maxPos].equals("5000")) {
               mTitale.setText("\uD83C\uDDF1\uD83C\uDDF0 Rs. 5000/=");

            }else if (classes[maxPos].equals("other")) {
               mTitale.setText("This is not a currency used in Sri Lanka");

           }


            //contains detailed information about each class's confidence percentage
            String s = "";
            for(int i = 0; i < classes.length; i++){
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
                // Toast.makeText(this, s+"", Toast.LENGTH_SHORT).show();
                mDis.setText(s+""); //displayed percentage in text view
            }

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    // handle the results of image capture/selection, and then process the selected/captured image using the classifyImage method
    //automatically called by the Android framework when an activity that started for a result returns a result
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //200 - image capture action has returned a result
        if (requestCode == 200) {
            //extracts the captured image as a Bitmap from the data
            Bitmap image = (Bitmap) data.getExtras().get("data");

            try {
                int dimension = Math.min(image.getWidth(), image.getHeight());

                //resizes the image to a square shape by finding the minimum dimension
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                //imageView.setImageBitmap(image);

                //again resized 224*224
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);

                //call classifyImage() method with processed image
                classifyImage(image);

                mImg1.setImageBitmap(image);

            } catch (Exception e) {

                Toast.makeText(this, "The photo could not be found", Toast.LENGTH_SHORT).show();
                //mSearchbar.setText("");
            }

        } else if (requestCode == 105 && resultCode == RESULT_OK) {
            Toast.makeText(this, "sdf", Toast.LENGTH_SHORT).show();


        } else if (requestCode == 100) {
            //get image from the gallery

            //mImg1.setImageURI(data.getData());

            //save the image in image view

            // selected image is obtained as a Uri
            Uri uri = data.getData();
            try {

                // reads the image using MediaStore.Images.Media.getBitmap
                img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                // image resizing and classification
                int dimension = Math.min(img.getWidth(), img.getHeight());
                img = ThumbnailUtils.extractThumbnail(img, dimension, dimension);
                //imageView.setImageBitmap(image);
                img = Bitmap.createScaledBitmap(img, imageSize, imageSize, false);

                classifyImage(img);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }}
}