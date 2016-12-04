package com.divesh.facerecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int SelectImage = 1234, candidateImageCount = 13;
    public static final String recognizeResult = "recoResult", registerResult = "regResult";
    public static Bitmap targetBitmap = null, candidateBitmap = null;
    private IntentFilter mIntentFilter;
    public static String galleryID, subjectID;
    private Handler handler = new Handler();
    private static String[] paths;
    public static ArrayList<Bitmap> matchedImages;
    public static int matchedImageCount = 0, comparedImageCount = 0;
    TextView showResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(recognizeResult);
        mIntentFilter.addAction(registerResult);
        paths = getCandidateImagePaths();

        showResult = (TextView) findViewById(R.id.resultTextView);
        showResult.setText("Select images from gallery");

        ImageButton getImagesActivity = (ImageButton) findViewById(R.id.backButton);
        getImagesActivity.setOnClickListener(new ImageButton.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), GetImages.class);
                startActivity(intent);
            }
        });

        ImageButton loadImage = (ImageButton) findViewById(R.id.loadButton);
        loadImage.setOnClickListener(new ImageButton.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, SelectImage);
            }
        });

        final ImageButton resetButton = (ImageButton) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new ImageButton.OnClickListener(){

            @Override
            public void onClick(View v) {
                ImageView targetImage = (ImageView) findViewById(R.id.targetImage);
                ImageView candidateImage = (ImageView) findViewById(R.id.candidateImage);
                targetImage.setImageResource(0);
                candidateImage.setImageResource(0);
                TextView showResult = (TextView) findViewById(R.id.resultTextView);
                showResult.setText("Select images from gallery");
                comparedImageCount = 0;
                matchedImageCount = 0;
                targetBitmap = null;
                candidateBitmap = null;
            }
        });

        final ImageButton recogButton = (ImageButton) findViewById(R.id.recognizeButton);
        recogButton.setOnClickListener(new ImageButton.OnClickListener(){

            @Override
            public void onClick(View v) {
                TextView showResult = (TextView) findViewById(R.id.resultTextView);
                showResult.setText("Please Wait...");

                new Thread(new ClientReceive()).start();
            }
        });
    }

    class ClientReceive implements Runnable{

        private static final int SERVERPORT = 49153;
        private static final String SERVER_IP = "localhost";

        @Override
        public void run() {

            InetAddress serverAddr = null;
            try {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showResult.setText("b4 conn");
                    }
                });
                serverAddr = InetAddress.getByName(SERVER_IP);
                Socket socket = new Socket(serverAddr, SERVERPORT);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showResult.setText("af conn");
                    }
                });

                InputStream in = socket.getInputStream();
                DataInputStream serverInputStream = new DataInputStream(in);

                int imageSize = serverInputStream.readInt();
                byte[] imageData = new byte[imageSize];
                if (imageSize > 0){
                    serverInputStream.readFully(imageData);
                }
                socket.close();
                targetBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ImageView targetImage = (ImageView) findViewById(R.id.targetImage);
                        targetImage.setImageBitmap(targetBitmap);
                    }
                });

                startFaceRecognition();

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void startFaceRecognition() {
        if (targetBitmap != null){
            galleryID = Long.toString(System.currentTimeMillis()/1000);
            subjectID = galleryID + (new Random().nextInt(10000)+100);

            comparedImageCount = 0;
            matchedImageCount = 0;
            Intent regIntent = new Intent(this, FaceRegister.class);
            startService(regIntent);
        }
        else {
            Toast.makeText(getApplicationContext(), "No Target Image Set!", Toast.LENGTH_SHORT).show();
        }
    }

    private String[] getCandidateImagePaths() {
        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID, MediaStore.Images.ImageColumns.DATE_TAKEN };
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC LIMIT "+Integer.toString(candidateImageCount);
        //Stores all the images from the gallery in Cursor
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,   //images from external storage
                columns,                                        //attributes returned
                null,
                null,
                orderBy);                                       //order by date taken property

        //Total number of images
        int count = cursor.getCount();

        //Create an array to store path to all the images
        String[] imagePaths = new String[count];

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            //Store the path of the image
            imagePaths[i]= cursor.getString(dataColumnIndex);

        }

        return imagePaths;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(recognizeResult)) {
                String response = intent.getStringExtra(recognizeResult);
                Log.d("Kairos_main response", response);
                TextView showResult = (TextView) findViewById(R.id.resultTextView);

                if(response.contains("success")){
                    matchedImageCount++;
                    matchedImages.add(candidateBitmap);
                }
                showResult.setText(comparedImageCount+" done. "+matchedImageCount+" matched so far.");
                if(comparedImageCount < candidateImageCount){
                    matchImages();
                }
            }
            else if (intent.getAction().equals(registerResult)){
                String response = intent.getStringExtra(registerResult);
                if (!response.contains("Errors")){
                    matchImages();
                }
                else{
                    TextView showResult = (TextView) findViewById(R.id.resultTextView);
                    showResult.setText("Error registering image!!");
                }
            }
        }
    };

    private void matchImages() {

        candidateBitmap = BitmapFactory.decodeFile(paths[comparedImageCount]);
        comparedImageCount++;
        ImageView candidateImage = (ImageView) findViewById(R.id.candidateImage);
        candidateImage.setImageBitmap(candidateBitmap);
        Intent serviceIntent = new Intent(this, FaceReco.class);
        startService(serviceIntent);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == SelectImage){
            Uri targetUri = data.getData();
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));

                ImageView targetImage = (ImageView) findViewById(R.id.targetImage);
                targetImage.setImageBitmap(bitmap);
                targetBitmap = bitmap;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
