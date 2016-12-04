package com.divesh.facerecognition;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kairos.Kairos;
import com.kairos.KairosListener;
import org.json.JSONException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Divesh on 11/13/2016.
 */

public class FaceReco extends Service {

    public static String recoResponse = "";
    public static boolean faceRegistered = false;

    private static String app_id = "ab20ddcb",
            api_key = "969e2426d2b31c4491e73b0fe9ff8cdb";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

         /* * * instantiate a new kairos instance & set authentication * * */
        Kairos faceRecog = new Kairos();
        faceRecog.setAuthentication(this, app_id, api_key);

        recognizeFace(faceRecog);

        return START_REDELIVER_INTENT;
    }

    private void recognizeFace(Kairos faceReco) {
        // listener
        KairosListener recoListener = new KairosListener() {
            @Override
            public void onSuccess(String response) {
                sendRecoResult(response);
            }
            @Override
            public void onFail(String response) {
                sendRecoResult(response);
            }
        };

        try {
            Log.d("Kairos reco count", Integer.toString(MainActivity.comparedImageCount));
            faceReco.recognize(MainActivity.candidateBitmap, MainActivity.galleryID, null, null, null, null, recoListener);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void sendRecoResult(String response) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.recognizeResult);
        broadcastIntent.putExtra(MainActivity.recognizeResult, response);
        sendBroadcast(broadcastIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}
