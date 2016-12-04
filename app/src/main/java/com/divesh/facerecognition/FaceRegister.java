package com.divesh.facerecognition;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.kairos.Kairos;
import com.kairos.KairosListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class FaceRegister extends Service {
    public FaceRegister() {
    }

    private static String app_id = "ab20ddcb",
            api_key = "969e2426d2b31c4491e73b0fe9ff8cdb";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

         /* * * instantiate a new kairos instance & set authentication * * */
        Kairos faceReg = new Kairos();
        faceReg.setAuthentication(this, app_id, api_key);

        deleteGallery(faceReg);
        registerFace(faceReg);

        return START_REDELIVER_INTENT;
    }

    private void registerFace(Kairos registerAPI) {
        // listener
        KairosListener registerListener = new KairosListener() {
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

            registerAPI.enroll(MainActivity.targetBitmap, MainActivity.subjectID, MainActivity.galleryID, null, "true", null, registerListener);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }


    //    deletes the current gallery referred by galleryId, if it exists
    private void deleteGallery(final Kairos registerAPI) {
        try {
            registerAPI.listGalleries(new KairosListener() {
                @Override
                public void onSuccess(String s) {
                    if (s.contains(MainActivity.galleryID)){
                        Log.d("KAIROS list", s);
                        try {
                            registerAPI.deleteGallery(MainActivity.galleryID, new KairosListener() {
                                @Override
                                public void onSuccess(String s) {Log.d("KAIROS del", s);}
                                @Override
                                public void onFail(String s) {}
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFail(String s) {
                    Log.d("KAIROS list err", s);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private void sendRecoResult(String response) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.registerResult);
        broadcastIntent.putExtra(MainActivity.registerResult, response);
        sendBroadcast(broadcastIntent);
        Log.d("KAIROS reg", response);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
