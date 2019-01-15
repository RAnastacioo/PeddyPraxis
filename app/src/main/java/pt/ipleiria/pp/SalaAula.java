package pt.ipleiria.pp;



import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;


import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.model.Task;


public class SalaAula extends AppCompatActivity {

    private ImageView imgView = null;
    private Bitmap imageBitmap = null;
    private TextView textRec;
    private Task task;

    private  FirebaseVisionCloudLabelDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sala_aula);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher_icon);

        Intent i = getIntent();
        task = (Task) i.getSerializableExtra("task");
        task = SingletonPPB.getInstance().containsIDTask(task.getId());

        imgView = findViewById(R.id.image_view);
        textRec = findViewById(R.id.textRecog);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    imageBitmap = (Bitmap) extras.get("data");
                    imgView.setImageBitmap(imageBitmap);
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
                    runCloudLabel(imageBitmap);
                    imageBitmap = null;
                }
                break;
        }
    }

    private void runCloudLabel(Bitmap mSelectedImage ){

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mSelectedImage);
        FirebaseVisionCloudDetectorOptions   options = new FirebaseVisionCloudDetectorOptions.Builder()
                .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                .setMaxResults(15)
                .build();

         detector = FirebaseVision.getInstance().getVisionCloudLabelDetector(options);
        detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionCloudLabel>>() {
            @Override
            public void onSuccess(List<FirebaseVisionCloudLabel> labels) {
                Log.d("cloudlabel", "cloud label size: " + labels.size());
                List<String> labelsStr = new ArrayList<>();
                for (int i = 0; i < labels.size(); ++i) {
                    FirebaseVisionCloudLabel label = labels.get(i);

                    if (label.getLabel() != null) {
                        labelsStr.add((label.getLabel()));
                    }
                }
                textRec.setVisibility(View.VISIBLE);
                textRec.setText(labelsStr.toString());
                Log.d("cloudlabel", "cloud label: " + labelsStr.toString());

                if(labelsStr.toString().toUpperCase().contains("CLASSROOM")){

                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.finish), Snackbar.LENGTH_LONG).show();

                    task.setTaskComplete(true);
                    Intent intent = new Intent(SalaAula.this, GameActivity.class);
                    intent.putExtra("FinishTask",task.getIdGame());
                    startActivity(intent);
                    finish();

                } else {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.answer_not_detected_try_again), Snackbar.LENGTH_LONG).show();
                }
            }
        });

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

    public void OnCLickDetectBuilding(View view) {
        if (imageBitmap == null) {
            //dispatchTakePictureIntent();
            AlertDialog.Builder builder = new AlertDialog.Builder(SalaAula.this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setMessage(getString(R.string.choose_picture))
                    .setPositiveButton(getString(R.string.gallery), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, 1);//one can be replaced with any action code
                        }
                    })
                    .setNegativeButton(getString(R.string.camera), new DialogInterface.OnClickListener() {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.upbar, menu);
        return true;
    }

    public void onClick_action_return(MenuItem item) {
        onBackPressed();
    }
}


