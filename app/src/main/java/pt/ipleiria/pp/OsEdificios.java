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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


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
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.automl.v1beta1.AnnotationPayload;
import com.google.cloud.automl.v1beta1.ExamplePayload;
import com.google.cloud.automl.v1beta1.Image;
import com.google.cloud.automl.v1beta1.ModelName;
import com.google.cloud.automl.v1beta1.PredictResponse;
import com.google.cloud.automl.v1beta1.PredictionServiceClient;
import com.google.cloud.automl.v1beta1.PredictionServiceSettings;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.grpc.internal.IoUtils;
import pt.ipleiria.pp.help.PermissionUtils;
import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.model.Task;

import static com.google.protobuf.TextFormat.print;


public class OsEdificios extends AppCompatActivity {
    // TODO: insert project info
    private static final String COMPUTE_REGION = "us-central1";
    private static final String PROJECT_ID = "peddypraxis-224610";
    private static final String MODEL_ID = "ICN3501432650872849900";
    private static final String SCORE_THRESHOLD = "0.95";

    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final String TAG = OsEdificios.class.getSimpleName();

    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private TextView mImageDetails;
    private ImageView mMainImage;

    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private PendingIntent myPendingIntentOsEdificios;
    private OsEdificios.FenceReceiver fenceReceiver;

    private boolean local = true;
    private boolean A,B,C,D,E,library=false;
    private Task task;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.osedificios);


        Intent i = getIntent();
        task = (Task) i.getSerializableExtra("task");
        task = SingletonPPB.getInstance().containsIDTask(task.getId());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (local) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(OsEdificios.this, android.R.style.Theme_Material_Dialog_Alert);
                    builder.setMessage("Choose a picture")
                            .setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startGalleryChooser();
                                }
                            })
                            .setNegativeButton("Camera", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startCamera();
                                }
                            });
                    builder.create().show();

                } else {
                    Snackbar.make(findViewById(android.R.id.content), "É necessario esta especificado de cada edificio!", Snackbar.LENGTH_LONG).show();
                }

            }
        });

        mImageDetails = findViewById(R.id.image_details);
        mMainImage = findViewById(R.id.main_image);

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntentOsEdificios = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new OsEdificios.FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public void dsfsf(View view) {
        queryFences();
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

        AwarenessFence A = LocationFence.in(39.73478631,-8.82090569, 30, 0L);
        AwarenessFence B = LocationFence.in(39.73439853, -8.82157087, 30, 0L);
        AwarenessFence C = LocationFence.in(39.73389524, -8.82194638, 30, 0L);
        AwarenessFence D = LocationFence.in(39.73436965, -8.82115781, 30, 0L);
        AwarenessFence E = LocationFence.in(39.73301241,-8.82151723, 30, 0L);
        AwarenessFence bibli = LocationFence.in(39.73325993, -8.82081985, 30, 0L);
        AwarenessFence local = AwarenessFence.or(A, B, C, D, E, bibli);
        addFence("localOsEdificios", local);

    }

    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntentOsEdificios)
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

                case "localOsEdificios":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceInfo = "TRUE | localOsEdificios.";
                            Log.d("debug", "localOsEdificios TRUE");
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

    public void uploadImage(Uri uri) {

        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap = scaleBitmapDown(
                        MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                        1200);

                mMainImage.setImageBitmap(bitmap);

                PredictTask predictTask = new PredictTask();
                predictTask.execute(PROJECT_ID, COMPUTE_REGION, MODEL_ID, SCORE_THRESHOLD, uri.toString());
            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, "Something is wrong with that image. Pick a different one please.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, "Something is wrong with that image. Pick a different one please.", Toast.LENGTH_LONG).show();
        }
    }

    private class PredictTask extends AsyncTask<String, Integer, String> {
        String res = "";

        @Override
        protected void onPreExecute() {
            res = "Please wait...";
            mImageDetails.setText(res);

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected String doInBackground(String... predictParams) {

            try {
                String projectId = predictParams[0];
                String computeRegion = predictParams[1];
                String modelId = predictParams[2];
                String scoreThreshold = predictParams[3];
                Uri uri = Uri.parse(predictParams[4]);

                // MORE INFO: https://stackoverflow.com/a/47799002
                GoogleCredential credential;

                // TODO: insert correct json here
                InputStream inputStream = getResources().openRawResource(R.raw.peddypraxis);

                // MORE INFO: https://github.com/auth0/java-jwt/issues/131
                credential = GoogleCredential.fromStream(inputStream);
                Collection<String> scopes = Collections.singleton("https://www.googleapis.com/auth/cloud-platform");

                if (credential.createScopedRequired()) {
                    credential = credential.createScoped(scopes);
                }

                // copy over key values, note the additional "s", set some expiry
                // com.google.auth.oauth2.GoogleCredentials
                GoogleCredentials sac = ServiceAccountCredentials.newBuilder()
                        .setPrivateKey(credential.getServiceAccountPrivateKey())
                        .setPrivateKeyId(credential.getServiceAccountPrivateKeyId())
                        .setClientEmail(credential.getServiceAccountId())
                        .setScopes(scopes)
                        // .setAccessToken(new AccessToken(credential.getAccessToken(), new LocalDate().plusYears(1).toDate()))
                        .build();

                // Latest generation Google libs, GoogleCredentials extends Credentials
                CredentialsProvider cp = FixedCredentialsProvider.create(sac);


                PredictionServiceSettings settings = PredictionServiceSettings.newBuilder().setCredentialsProvider(cp).build();

                // Instantiate client for prediction service.
                PredictionServiceClient predictionClient = PredictionServiceClient.create(settings);


                // Get the full path of the model.
                ModelName name = ModelName.of(projectId, computeRegion, modelId);

                InputStream inputStreamImage = getContentResolver().openInputStream(uri);
                byte[] bytes = IoUtils.toByteArray(inputStreamImage);
                ByteString content = ByteString.copyFrom(bytes);
                Image image = Image.newBuilder().setImageBytes(content).build();
                ExamplePayload examplePayload = ExamplePayload.newBuilder().setImage(image).build();

                // Additional parameters that can be provided for prediction e.g. Score Threshold
                Map<String, String> params = new HashMap<>();
                if (scoreThreshold != null) {
                    params.put("score_threshold", scoreThreshold);
                }
                // Perform the AutoML Prediction request
                PredictResponse response = predictionClient.predict(name, examplePayload, params);


                String edf = "";
                String res = "";
                res += "Prediction results:";
                for (AnnotationPayload annotationPayload : response.getPayloadList()) {
                    res += "\nPredicted class name: " + annotationPayload.getDisplayName() + "\n";
                    res += "Predicted class score: " + annotationPayload.getClassification().getScore();
                    if(annotationPayload.getDisplayName()!=null) {
                        edf = annotationPayload.getDisplayName();
                    }
                }
                

                Log.d("automl1", edf);

                CheckBox edfA = findViewById(R.id.edfA);
                CheckBox edfB = findViewById(R.id.edfB);
                CheckBox edfC = findViewById(R.id.edfC);
                CheckBox edfD = findViewById(R.id.edfD);
                CheckBox edfE = findViewById(R.id.edfE);
                CheckBox bibli = findViewById(R.id.edfBibli);

                if((edf.equalsIgnoreCase("A") && A) || (edf.equalsIgnoreCase("B") && B) ||
                        (edf.equalsIgnoreCase("C") && C) || (edf.equalsIgnoreCase("D") && D)
                        || (edf.equalsIgnoreCase("E") && E) || (edf.equalsIgnoreCase("Biblioteca") && library) ){

                    Snackbar.make(findViewById(android.R.id.content), "Este edifício já foi reconhecido!", Snackbar.LENGTH_LONG).show();

                }

                if(edf.equalsIgnoreCase("A")){
                    A=true;
                    edfA.setChecked(true);
                } else if(edf.equalsIgnoreCase("B")){
                    B=true;
                    edfB.setChecked(true);
                }else if(edf.equalsIgnoreCase("C")){
                    C=true;
                    edfC.setChecked(true);
                } else if(edf.equalsIgnoreCase("D")){
                    D=true;
                    edfD.setChecked(true);
                }else if(edf.equalsIgnoreCase("E")){
                    E=true;
                    edfE.setChecked(true);
                }else if(edf.equalsIgnoreCase("Biblioteca")){
                    library=true;
                    bibli.setChecked(true);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "A foto deve ser retirada apenas aos edifícios solicitados "+"\n"+"Tente outra vez!", Snackbar.LENGTH_LONG).show();
                }

                if(A && B && C && D && E && library){
                    Snackbar.make(findViewById(android.R.id.content), "Finish!", Snackbar.LENGTH_LONG).show();

                    task.setTaskComplete(true);
                    Intent intent = new Intent(OsEdificios.this, GameActivity.class);
                    intent.putExtra("FinishTask",task.getIdGame());
                    startActivity(intent);
                    finish();

                }

                return res;
            } catch (IOException e) {
                e.printStackTrace();
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mImageDetails.setText(result);
        }
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeFences("localOsEdificios");
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

