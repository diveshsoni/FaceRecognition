package com.divesh.facerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class GetImages extends AppCompatActivity {

    private static final int SelectedTargetImage = 1400, serverPort = 49153;
    public static Bitmap targetImageFile;
    private static  byte targetImageArray [];
    private Handler handler = new Handler();
    TextView serverStatusTV;
    boolean isServerOn = false;
    int imagesSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_images);
        serverStatusTV = (TextView) findViewById(R.id.serverStatus);

        ImageButton sendFileButton = (ImageButton) findViewById(R.id.startServer);
        sendFileButton.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isServerOn){
                    if (targetImageFile != null){
                        new Thread(new ReceiveClients()).start();
                        isServerOn = true;
                        imagesSent = 0;
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Please Select An Image To Be Sent!", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Server Running Already!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton faceRecButton = (ImageButton) findViewById(R.id.faceRecoAcivity);
        faceRecButton.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        ImageView targetImage = (ImageView)findViewById(R.id.targetImage);
        targetImage.setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, SelectedTargetImage);
            }
        });

        ImageButton stopServerButton = (ImageButton) findViewById(R.id.stopServer);
        stopServerButton.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                isServerOn = false;
                serverStatusTV.setText("Server Stopped");
            }
        });
    }

    class ReceiveClients implements Runnable{
        ServerSocket serverSocket = null;

        @Override
        public void run() {
            try {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        serverStatusTV.setText("Server listening");
                    }
                });
                serverSocket = new ServerSocket(serverPort);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        serverStatusTV.setText("Server started");
                    }
                });
                Socket clientSocket = null;
//                TextView clientCountTV = (TextView)findViewById(R.id.clientCount);
                int clientCount = 0;
                while(true){
                    clientSocket = serverSocket.accept();
                    clientCount++;
                    final int finalClientCount = clientCount;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TextView clientCountTV = (TextView)findViewById(R.id.clientCount);
                    clientCountTV.setText(finalClientCount + " Client(s) connected");
                        }
                    });
                    Log.d("Client count:", Integer.toString(clientCount));
                    new Thread(new FileTransfer(clientSocket)).start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (serverSocket != null){
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class FileTransfer implements Runnable{

        protected Socket clientSocket = null;
        public FileTransfer(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                InputStream clientInput = clientSocket.getInputStream();
                OutputStream clientOutput = clientSocket.getOutputStream();
//                TextView sentImageCount = (TextView)findViewById(R.id.ImageSentCount);

                DataOutputStream clientOutStream = new DataOutputStream(clientOutput);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        TextView sentImageCount = (TextView)findViewById(R.id.ImageSentCount);
                        sentImageCount.setText(imagesSent+1 + " images sent");
                    }
                });

                if (targetImageFile != null){
                    int imageByteSize = targetImageArray.length;
                    clientOutStream.writeInt(imageByteSize);
                    if (imageByteSize > 0){
                        clientOutStream.write(targetImageArray, 0, imageByteSize);
//                        sentImageCount.setText(++imagesSent+" Image(s) Sent");
                        Log.d("Image Count:", Integer.toString(++imagesSent));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == SelectedTargetImage){
            Uri targetUri = data.getData();

            try {
                targetImageFile = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
                ImageView targetImage = (ImageView)findViewById(R.id.targetImage);
                targetImage.setImageBitmap(targetImageFile);

                targetImageArray = null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                targetImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                targetImageArray = baos.toByteArray();


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
