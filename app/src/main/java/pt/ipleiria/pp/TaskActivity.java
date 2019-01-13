package pt.ipleiria.pp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.model.Task;

public class TaskActivity extends AppCompatActivity {

    public static final String ID_VIEW_TASK = "id_viewTask";

    private SingletonPPB PPB;
    private Task task;
    Switch switchAB;
    private int mId = 1;


    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private PendingIntent myPendingIntentTask;
    private TaskActivity.FenceReceiver fenceReceiver;

    String[] appPermission = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CALL_PHONE
    };
    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher_icon);

        PPB = SingletonPPB.getInstance();

        Intent i = getIntent();

        EditText etTitle = findViewById(R.id.task_Title);
        EditText etDescription = findViewById(R.id.task_Description);
        EditText etValue = findViewById(R.id.task_value);
        TextView etId = findViewById(R.id.task_id);
        TextView etOrder = findViewById(R.id.task_order);


        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntentTask = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new TaskActivity.FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));


        if (i.getStringExtra(ID_VIEW_TASK) != null) {
            String idgame = i.getStringExtra(ID_VIEW_TASK);
            task = PPB.containsIDTask(idgame);

            etTitle.setHint("Title: " + task.getTitle());
            etTitle.setEnabled(false);
            etDescription.setHint("Description: " + task.getDescription());
            etDescription.setEnabled(false);
            etValue.setHint("Value: " + task.getValue());
            etValue.setEnabled(false);
            etId.setText("" + task.getId());
            etOrder.setText("" + task.getOrder());

            TextView locationText = findViewById(R.id.locationText);
            ImageView locationImage = findViewById(R.id.imageLocation);

            if (task.getTitle().equals("O pátio")) {

                locationText.setVisibility(View.VISIBLE);
                locationImage.setVisibility(View.VISIBLE);
                InputStream is = null;
                try {
                    is = getAssets().open("patio.png");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                locationImage.setImageBitmap(bitmap);

            } else if (task.getTitle().equals("Os Edifícios")) {
                locationText.setVisibility(View.VISIBLE);
                locationImage.setVisibility(View.VISIBLE);
                InputStream is = null;
                try {
                    is = getAssets().open("edificios.jpg");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                locationImage.setImageBitmap(bitmap);

            } else if (task.getTitle().equals("A Biblioteca")) {
                locationText.setVisibility(View.VISIBLE);
                locationImage.setVisibility(View.VISIBLE);
                InputStream is = null;
                try {
                    is = getAssets().open("biblioteca.png");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                locationImage.setImageBitmap(bitmap);

            } else if (task.getTitle().equals("Descompressao")) {
                locationText.setVisibility(View.VISIBLE);
                locationImage.setVisibility(View.VISIBLE);
                InputStream is = null;
                try {
                    is = getAssets().open("patio.png");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Bitmap bitmap = BitmapFactory.decodeStream(is);
                locationImage.setImageBitmap(bitmap);

            } else {
                locationText.setVisibility(View.INVISIBLE);
                locationImage.setVisibility(View.INVISIBLE);

            }

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
                        //textView_main.setText(text + textView_main.getText());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be queried: " + e.getMessage();
                        Log.d("queryFence", text);
                        // textView_main.setText(text + textView_main.getText());
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

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

    public void onClick_btn_start_task(View view) {

        removeFences("localfence");
        if (checkAndRequestPermissions()) {

            if (task.getTitle().equals("O pátio")) {

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

                AwarenessFence inLocationFence = LocationFence.in(39.735729, -8.820847, 200, 0L);
                addFence("localFence", inLocationFence);


            } else if (task.getTitle().equals("Os Edifícios")) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
                    AwarenessFence A = LocationFence.in(39.735729, -8.820847, 200, 0L);
                    AwarenessFence B = LocationFence.in(39.734468, -8.821494, 200, 0L);
                    AwarenessFence C = LocationFence.in(39.733865, -8.821885, 200, 0L);
                    AwarenessFence D = LocationFence.in(39.734490, -8.820937, 200, 0L);
                    AwarenessFence E = LocationFence.in(39.732870, -8.822050, 200, 0L);
                    AwarenessFence bibli = LocationFence.in(39.733465, -8.820731, 200, 0L);
                    AwarenessFence local = AwarenessFence.or(A, B, C, D, E, bibli);
                    addFence("localFence", local);

                } else {
                    Snackbar.make(findViewById(android.R.id.content), "É necessário Android 9 (API level 28)! ", Snackbar.LENGTH_LONG).setActionTextColor(Color.RED).show();
                }

            } else if (task.getTitle().equals("A Biblioteca")) {
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
                AwarenessFence inLocationFence = LocationFence.in(39.733465, -8.820731, 200, 0L);
                addFence("localFence", inLocationFence);


            } else if (task.getTitle().equals("Descompressao")) {

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

                AwarenessFence inLocationFence = LocationFence.in(39.735729, -8.820847, 200, 0L);
                addFence("localFence", inLocationFence);


            } else if (task.getTitle().equals("Sala de Aula")) {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(TaskActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Rules")
                        .setMessage("CRIAR PREAMBULO DESTA ATIVIDADE")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                Intent intent = new Intent(TaskActivity.this, SalaAula.class);
                                intent.putExtra("task",task);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show().setCanceledOnTouchOutside(false);

            } else if (task.getTitle().equals("Melhor Curso")) {

                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(TaskActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Rules")
                        .setMessage("CRIAR PREAMBULO DESTA ATIVIDADE")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                Intent intent = new Intent(TaskActivity.this, MelhorCurso.class);
                                intent.putExtra("task",task);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show().setCanceledOnTouchOutside(false);

            } else {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(TaskActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Information!")
                        .setMessage("Game under construction....")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show().setCanceledOnTouchOutside(false);
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "É obrigatorio aceitar permissões", Snackbar.LENGTH_LONG).setActionTextColor(Color.RED).show();

        }
    }

    private boolean checkAndRequestPermissions() {

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermission) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {

            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntentTask)
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

    public void onclick(View view) {
        queryFences();
    }

    private class FenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            FenceState fenceState = FenceState.extract(intent);
            String fenceInfo = null;
            switch (fenceState.getFenceKey()) {

                case "localFence":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceInfo = "TRUE | location";
                            clearnotify();
                            if (task.getTitle().equals("O pátio")) {
                                removeFences("localFence");
                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(TaskActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                                builder.setTitle("Rules")
                                        .setMessage("O calorio tem de estar no pátio do Edifício A!\n" + "É necessatio andar as voltas no pátio durante 5 minutos a piscar um olho e a sorrir apontando a camera frontal para si!")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                                Intent intent = new Intent(TaskActivity.this, OpatioActivity.class);
                                                intent.putExtra("task",task);
                                                startActivity(intent);
                                                finish();
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_info)
                                        .show().setCanceledOnTouchOutside(false);

                            } else if (task.getTitle().equals("Os Edifícios")) {
                                removeFences("localFence");
                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(TaskActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                                builder.setTitle("Rules")
                                        .setMessage("O calorio deve!\n" + "Retirar uma foto a cada edifício de forma a conhecer os mesmos")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                                Intent intent = new Intent(TaskActivity.this, OsEdificios.class);
                                                intent.putExtra("task",task);
                                                startActivity(intent);
                                                finish();
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_info)
                                        .show().setCanceledOnTouchOutside(false);

                            } else if (task.getTitle().equals("A Biblioteca")) {
                                removeFences("localFence");
                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(TaskActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                                builder.setTitle("Rules")
                                        .setMessage("O calorio tem de completar a seguinte questão!\n" + "A Biblioteca jośe Saramago é um espaço de Cultura,Conhecimento e ...??")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                                Intent intent = new Intent(TaskActivity.this, BibliotecaActivity.class);
                                                intent.putExtra("task",task);
                                                startActivity(intent);
                                                finish();
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_info)
                                        .show().setCanceledOnTouchOutside(false);

                            } else if (task.getTitle().equals("Descompressao")) {

                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(TaskActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                                builder.setTitle("Rules")
                                        .setMessage("POR PREENCHER!")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                                Intent intent = new Intent(TaskActivity.this, Descompressao.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_info)
                                        .show().setCanceledOnTouchOutside(false);

                            }
                            break;
                        case FenceState.FALSE:
                            fenceInfo = "FALSE | location";
                            removeFences("localFence");
                            notifyItem();
                            break;
                    }
                    break;

                default:
                    fenceInfo = "Error: unknown fence: " + fenceState.getFenceKey();
                    notifyItem();
                    removeFences("localFence");
                    break;
            }

          //  Snackbar.make(findViewById(android.R.id.content), fenceInfo, Snackbar.LENGTH_LONG).show();
        }
    }

    private void notifyItem() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_icon)
                .setTicker("PeddyPraxis")
                //     .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("PeddyPraxis")
                .setContentText("You should not leave the task location!")
                .setContentInfo("Info");

        notificationManager.notify(/*notification id*/mId, notificationBuilder.build());

    }

    private void clearnotify() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mId);
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


}
