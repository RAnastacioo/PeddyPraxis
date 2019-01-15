package pt.ipleiria.pp;


import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;


import java.io.IOException;
import java.sql.Timestamp;

import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.model.Task;

public class BibliotecaActivity extends AppCompatActivity {

    private ImageView imgView = null;
    private Bitmap imageBitmap = null;
    private boolean local = true;
    private TextView textRec;
    //private GraphicOverlay mGraphicOverlay;
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private PendingIntent myPendingIntentAbiblioteca;
    private BibliotecaActivity.FenceReceiver fenceReceiver;
    private Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.abiblioteca);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher_icon);

        imgView = findViewById(R.id.image_view);
        textRec = findViewById(R.id.textRecog);

        Intent i = getIntent();
        task = (Task) i.getSerializableExtra("task");
        task = SingletonPPB.getInstance().containsIDTask(task.getId());

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntentAbiblioteca = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new BibliotecaActivity.FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    imageBitmap = (Bitmap) extras.get("data");
                    imgView.setImageBitmap(imageBitmap);
                    runCloudTextRecognition(imageBitmap);

                    imageBitmap = null;
                }

                break;
            case 1:
                if (resultCode == RESULT_OK) {

                    Uri selectedImage = data.getData();
                  //  Bitmap imageBitmap = null;
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    imgView.setImageBitmap(imageBitmap);
                    runCloudTextRecognition(imageBitmap);
                    imageBitmap = null;
                }
                break;
        }
    }

    private void runCloudTextRecognition(Bitmap mSelectedImage ){

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mSelectedImage);
        FirebaseVisionDocumentTextRecognizer recognizer = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer();
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionDocumentText>() {
                            @Override
                            public void onSuccess(FirebaseVisionDocumentText texts) {
                                processCloudTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });
    }

    private void processCloudTextRecognitionResult(FirebaseVisionDocumentText text) {
        // Task completed successfully
        if (text == null) {
            Snackbar.make(findViewById(android.R.id.content), R.string.not_found, Snackbar.LENGTH_LONG).show();
            textRec.setVisibility(View.INVISIBLE);
            return;
        }
       //mGraphicOverlay.clear();
        Log.d("Cloud", text.getText());
        textRec.setVisibility(View.VISIBLE);
        textRec.setText(text.getText());

        if(text.getText().toUpperCase().contains("CRIATIVIDADE")){

            Snackbar.make(findViewById(android.R.id.content), R.string.finish, Snackbar.LENGTH_LONG).show();
            task.setTaskComplete(true);
            Intent intent = new Intent(BibliotecaActivity.this, GameActivity.class);
            intent.putExtra("FinishTask",task.getIdGame());
            startActivity(intent);
            finish();

        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.answer_not_detected_try_again, Snackbar.LENGTH_LONG).show();
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

    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntentAbiblioteca)
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

                case "localAbiblioteca":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceInfo = "TRUE | localAbiblioteca.";
                            Log.d("debug", "localAbiblioteca TRUE");
                            local = true;
                            break;
                        case FenceState.FALSE:
                            fenceInfo = "FALSE | Location.";
                            Log.d("debug", "Location False");
                            local = false;
                            break;
                        case FenceState.UNKNOWN:
                            fenceInfo = "Error | Location.";
                            local = true;
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

    public void OnCLickDetectBuilding(View view) {
        if (imageBitmap == null) {
            //dispatchTakePictureIntent();
            AlertDialog.Builder builder = new AlertDialog.Builder(BibliotecaActivity.this,android.R.style.Theme_Material_Dialog_Alert);
            builder.setMessage(R.string.choose_picture)
                    .setPositiveButton(R.string.gallery, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto , 1);//one can be replaced with any action code
                        }
                    })
                    .setNegativeButton(R.string.camera, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(takePicture, 0);//zero can be replaced with any action code
                        }
                    });
            builder.create().show();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        removeFences("localAbiblioteca");
    }
    @Override
    protected void onResume() {
        super.onResume();

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


        AwarenessFence bibli = LocationFence.in(39.73325993, -8.82081985, 30, 0L);
        addFence("localAbiblioteca", bibli);

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