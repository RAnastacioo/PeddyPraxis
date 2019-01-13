// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package pt.ipleiria.pp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import pt.ipleiria.pp.common.CameraSource;
import pt.ipleiria.pp.common.CameraSourcePreview;
import pt.ipleiria.pp.common.GraphicOverlay;
import pt.ipleiria.pp.facedetection.FaceDetectionProcessor;
import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.model.Task;


/**
 * Demo app showing the various features of ML Kit for Firebase. This class is used to
 * set up continuous frame processing on frames from a camera source.
 */
@KeepName
public final class OpatioActivity extends AppCompatActivity
        implements OnRequestPermissionsResultCallback {


    private static final String TAG = "OpatioActivity";
    private static final int PERMISSION_REQUESTS = 1;
    private static int progress;
    public static boolean blinkEye,smilinG =false;
    public static boolean  walking,localA = true;

    private static final int min = 5;

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;

    public static Handler mHandler;
    private ProgressBar gameTimer;
    private CountDownTimer gameCountDownTimer, timer;

    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private PendingIntent myPendingIntentOpatio;
    private OpatioActivity.FenceReceiver fenceReceiver;

    private Task task;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.opatio_activity_live);

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntentOpatio = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new OpatioActivity.FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));


        Intent i = getIntent();
        task = (Task) i.getSerializableExtra("task");
        task = SingletonPPB.getInstance().containsIDTask(task.getId());


        preview = (CameraSourcePreview) findViewById(R.id.firePreview);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = (GraphicOverlay) findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }


        if (allPermissionsGranted()) {
            createCameraSource();
        } else {
            getRuntimePermissions();
            // ====
            createCameraSource();
        }

        // TODO: Communicate with the UI thread
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Boolean blink = msg.getData().getBoolean("blinkEye");
                Log.d(TAG, "Mensagem  " + blink);
                if (blink) {
                    blinkEye = true;
                }
                Boolean smiling = msg.getData().getBoolean("Smiling");
                if (smiling) {
                    smilinG = true;
                }
            }
        };

        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT); // cameraFrontal
        preview.stop();
        startCameraSource();

        gameTimer = findViewById(R.id.progressBar);
        setTimer(60 * min); // tempo em segundos


    }


    private void setTimer(int time) {
        progress = 100;
        final int timeDebug = time;
        final int actualTime = time * 1000;
        gameTimer.setProgress(progress);
        gameTimer.setScaleY(5);

        gameCountDownTimer = new CountDownTimer(actualTime, 5000) {
            int totalTime = actualTime;

            @Override
            public void onTick(long millisUntilFinished) {
                progress = (int) ((totalTime - millisUntilFinished) / (double) totalTime * 100);
                gameTimer.setProgress(progress);

                // ver se picou o olho
                if (gameTimer.getProgress() != 0) {

                    if (blinkEye & smilinG & walking & localA) {

                        Snackbar.make(findViewById(android.R.id.content), " god job " + blinkEye, Snackbar.LENGTH_SHORT).show();

                        blinkEye = false;
                        smilinG = false;

                    } else {

                        if(!blinkEye) {
                            Snackbar.make(findViewById(android.R.id.content), "É necessario piscar o olho!", Snackbar.LENGTH_LONG).show();
                        }
                        if(!smilinG) {
                           Snackbar.make(findViewById(android.R.id.content), "É necessario rir!", Snackbar.LENGTH_LONG).show();
                        }
                        if(!walking) {
                            Snackbar.make(findViewById(android.R.id.content), "É necessario andar", Snackbar.LENGTH_LONG).show();
                        }
                        if(!localA) {
                            Snackbar.make(findViewById(android.R.id.content), "É necessario estar no pátio do A", Snackbar.LENGTH_LONG).show();
                        }

                        smilinG = false;
                        blinkEye = false;

                        gameCountDownTimer.cancel();
                        gameCountDownTimer = null;
                        timer.cancel();
                        timer = null;
                        setTimer(timeDebug);

                    }
                }
            }

            @Override
            public void onFinish() {
                progress = 100;
                gameTimer.setProgress(progress);
                // finish activity
                Snackbar.make(findViewById(android.R.id.content), "Finish!", Snackbar.LENGTH_SHORT).show();
                removeFences("walkFence");
                removeFences("localFenceA");

                task.setTaskComplete(true);
                Intent intent = new Intent(OpatioActivity.this, GameActivity.class);
                intent.putExtra("FinishTask",task.getIdGame());
                startActivity(intent);
                finish();

            }
        }.start();

        timer = new CountDownTimer(actualTime, 1000) {
            TextView progressbartime = findViewById(R.id.progressbartime);

            @Override
            public void onTick(long millisUntilFinished) {
                long millis = millisUntilFinished;
                String hms = String.format("Time: %02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
                progressbartime.setText(hms);
            }

            @Override
            public void onFinish() {
                progressbartime.setText("Time: 00:00");
            }
        }.start();
    }

    private void createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor());  // inicio do face detectitico

    }

    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startCameraSource();

        removeFences("WalkFence");
        removeFences("localFenceA");

        AwarenessFence walkingFrence = AwarenessFence.not(AwarenessFence.or(DetectedActivityFence.during(DetectedActivity.IN_VEHICLE),DetectedActivityFence.during(DetectedActivity.ON_BICYCLE)));
        addFence("walkFence", walkingFrence);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        AwarenessFence inLocationFence = LocationFence.in(39.73557424,-8.82109344, 30, 0L);
        addFence("localFenceA", inLocationFence);

    }

    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntentOpatio)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
        removeFences("walkFence");
        removeFences("localFenceA");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    protected void queryFences() {

        Awareness.getFenceClient(this).queryFences(FenceQueryRequest.all())
                .addOnSuccessListener(new OnSuccessListener<FenceQueryResponse>() {
                    @Override
                    public void onSuccess(FenceQueryResponse fenceQueryResponse) {
                        String fenceInfo = "";
                        FenceStateMap fenceStateMap = fenceQueryResponse.getFenceStateMap();
                        for (String fenceKey : fenceStateMap.getFenceKeys()) {
                            int state = fenceStateMap.getFenceState(fenceKey).getCurrentState();
                            fenceInfo += fenceKey + ": "
                                    + (state == FenceState.TRUE ? "TRUE" :
                                    state == FenceState.FALSE ? "FALSE" : "UNKNOWN") + "\n";
                        }
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "> Fences' states:\n" + (fenceInfo.equals("") ?
                                "No registered fences." : fenceInfo);
                        Log.d("queryFence", text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be queried: " + e.getMessage();
                        Log.d("queryFence", text);
                    }
                });
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    public void dsfsf(View view) {
        queryFences();

    }

    protected void removeFences(String text) {

        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(text)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private class FenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            FenceState fenceState = FenceState.extract(intent);
            String fenceInfo = null;
            switch (fenceState.getFenceKey()) {

                case "walkFence":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceInfo = "TRUE | walkFence.";
                            Log.d("debug", "walkFence TRUE");
                            walking=true;

                            break;
                        case FenceState.FALSE:
                            fenceInfo = "FALSE | walkFence.";
                            Log.d("debug", "walkFence False");
                            walking=false;

                            break;
                        case FenceState.UNKNOWN:
                            fenceInfo = "Error | walkFence.";
                            walking=true;
                            break;
                    }
                    break;

                case "localFenceA":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceInfo = "TRUE | Location.";
                            Log.d("debug", "Location TRUE");
                            localA=true;
                            break;
                        case FenceState.FALSE:
                            fenceInfo = "FALSE | Location.";
                            Log.d("debug", "Location False");
                            localA=false;

                            break;
                        case FenceState.UNKNOWN:
                            fenceInfo = "Error | Location.";
                            localA=true;
                            break;
                    }
                    break;
                default:
                    fenceInfo = "Error: unknown fence: " + fenceState.getFenceKey();
                    break;
            }

          //  Snackbar.make(findViewById(android.R.id.content), fenceInfo, Snackbar.LENGTH_LONG).show();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.upbar, menu);
        return true;
    }

    public void onClick_action_return(MenuItem item) {
        onBackPressed();
    }
}
