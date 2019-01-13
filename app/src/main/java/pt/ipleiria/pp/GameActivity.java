package pt.ipleiria.pp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.awareness.snapshot.PlacesResponse;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pt.ipleiria.pp.model.Game;
import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.recyclerView.LineAdapter_task;

public class GameActivity extends AppCompatActivity {


    public static final String ID_VIEW_GAME = "id_viewGame";
    private static final int REQUEST_CODE_FLPERMISSION = 42;

    Switch switchAB;
    private int mId = 1;

    private SingletonPPB PPB;
    private RecyclerView recyclerView;
    private LineAdapter_task mAdapter;
    private Paint p = new Paint();
    private boolean editing;
    private Game game;

    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private PendingIntent myPendingIntent;
    private GameActivity.FenceReceiver fenceReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        PPB = SingletonPPB.getInstance();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher_icon);

        recyclerView = findViewById(R.id.recycler_view);
        setupRecycler();

        EditText etTitle = findViewById(R.id.game_Title);
        EditText etDescription = findViewById(R.id.game_Description);
        EditText etAuthor = findViewById(R.id.game_Author);
        EditText etDuration = findViewById(R.id.game_Duration);
        TextView etId = findViewById(R.id.game_Id);
        TextView etDate = findViewById(R.id.game_Update);
        etTitle.setEnabled(false);
        etDescription.setEnabled(false);
        etAuthor.setEnabled(false);
        etDuration.setEnabled(false);


        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        fenceReceiver = new GameActivity.FenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        Intent i = getIntent();

       if(i.getStringExtra("FinishTask") != null){
           Snackbar.make(findViewById(android.R.id.content), "Tarefa Concluida!", Snackbar.LENGTH_LONG).show();
       }


        if (i.getStringExtra(ID_VIEW_GAME) != null || i.getStringExtra("FinishTask") != null) {

            if (i.getStringExtra(ID_VIEW_GAME) != null) {
                final String id = i.getStringExtra(ID_VIEW_GAME);
                game = PPB.containsID(id);

            }
            if (i.getStringExtra("FinishTask") != null) {
                final String id = i.getStringExtra("FinishTask");
                game = PPB.containsID(id);

            }

            mAdapter.updateFullList(game);

            etTitle.setHint("Title: "+game.getTitle());
            etDescription.setHint("Description: "+game.getDescription());
            etAuthor.setHint("Author: "+game.getAuthor());
            etDuration.setHint("Duration: " + game.getDurationGame() +" minutes");
            etId.setText("" + game.getId());
            etDate.setText(game.getLastUpdate());

            long nowMillis = System.currentTimeMillis();
            AwarenessFence timeFence = TimeFence.inInterval(nowMillis,game.getDurationGame()*60000+nowMillis);
            addFence("timeFence", timeFence);

            long time50 = (long) (((game.getDurationGame())/(double)2)*60000+nowMillis);
            AwarenessFence timeFence50 = TimeFence.inInterval(nowMillis,time50);
            addFence("timeFence50", timeFence50);

            long  time10 = (long) (((game.getDurationGame())*60000)*(double)0.9);
            AwarenessFence timeFence10 = TimeFence.inInterval(nowMillis,time10+nowMillis);
            addFence("timeFence10", timeFence10);


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
                        Log.d("queryFence",text);
                        //textView_main.setText(text + textView_main.getText());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be queried: " + e.getMessage();
                        Log.d("queryFence",text);
                        // textView_main.setText(text + textView_main.getText());
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            FileOutputStream fileOutputStream =
                    openFileOutput("game.bin", Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream =
                    new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(SingletonPPB.getInstance().getGames());
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(GameActivity.this, "Error_write_Game_to_internal", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (game != null) {
            mAdapter.updateFullList(game);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.upbar, menu);
        return true;
    }


    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntent)
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
        printNearbyPlaces();
        printWeather();
    }

    private class FenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            FenceState fenceState = FenceState.extract(intent);
            String fenceInfo = null;
            switch (fenceState.getFenceKey()) {

                case "timeFence":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceInfo = "TRUE | timeFence";
                            break;
                        case FenceState.FALSE:
                            fenceInfo = "FALSE | timeFence";
                            Log.d("timefence","game tive over");
                            clearnotify();
                            notifyItem("Time to finish","the time is over");
                            removeFences();
                            game.setTimeOver(true);

                            if(game!=null){
                                boolean alltrue=true;
                                for(int j = 0; j<game.getTasks().size() ; j++){
                                    if(!game.getTasks().get(j).isTaskComplete()){
                                        alltrue=false;
                                    }
                                }
                                if(alltrue==true){
                                    game.setGamewin(true);
                                    game.setGameLost(false);
                                    Intent intentwin = new Intent(GameActivity.this, win.class);
                                    startActivity(intentwin);
                                    finish();
                                }
                                if(alltrue!=true && game.isTimeOver()){
                                    game.setGameLost(true);
                                    game.setGamewin(false);
                                    Intent intentlost = new Intent(GameActivity.this, GameOver.class);
                                    startActivity(intentlost);
                                    finish();
                                }
                            }
                            break;
                        case FenceState.UNKNOWN:
                            fenceInfo = "Error: unknown state.";
                            break;
                    }
                    break;
                case "timeFence50":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceInfo = "TRUE | timeFence50";
                            break;
                        case FenceState.FALSE:
                            fenceInfo = "FALSE | timeFence50";
                            Log.d("timefence","50");
                            clearnotify();
                            notifyItem("Time to finish","50% to end the game");
                            break;
                    }
                    break;
                case "timeFence10":
                    switch (fenceState.getCurrentState()) {
                        case FenceState.TRUE:
                            fenceInfo = "TRUE | timeFence10.";
                            break;
                        case FenceState.FALSE:
                            fenceInfo = "FALSE | Not timeFence10.";
                            Log.d("timefence","10");
                            notifyItem("Time to finish","10% to end the game");
                            break;
                        case FenceState.UNKNOWN:
                            fenceInfo = "Error:  timeFence10.";
                            break;
                    }
                    break;
                default:
                    fenceInfo = "Error: unknown fence: " + fenceState.getFenceKey();
                    break;
            }

           // Snackbar.make(findViewById(android.R.id.content), fenceInfo, Snackbar.LENGTH_LONG).show();
        }
    }

    private void notifyItem(String title,String text) {
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
                .setContentTitle(title)
                .setContentText(text)
                .setContentInfo("Info");

        notificationManager.notify(/*notification id*/mId, notificationBuilder.build());

    }

    private void clearnotify() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mId);
    }

    protected void removeFences() {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(myPendingIntent)
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

    public void onClick_action_return(MenuItem item) {
        onBackPressed();
    }

    private void setupRecycler() {

        // Configurando o gerenciador de layout para ser uma lista.
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Adiciona o adapter que irá anexar os objetos à lista.
        // Está sendo criado com lista vazia, pois será preenchida posteriormente.
        mAdapter = new LineAdapter_task(new ArrayList<>(0));
        recyclerView.setAdapter(mAdapter);

        // Configurando um dividr entre linhas, para uma melhor visualização.
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void checkFineLocationPermission() {
        if (ContextCompat.checkSelfPermission(GameActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GameActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_FLPERMISSION // todo: declare this constant
            );
        }
        try {
            int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                Toast.makeText(this, "Error: high accuracy location mode must be enabled in the device.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Settings.SettingNotFoundException e) {
            Toast.makeText(this, "Error: could not access location mode.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }
    }

    private void printWeather() {
        checkFineLocationPermission();
        Awareness.getSnapshotClient(this).getWeather()
                .addOnSuccessListener(new OnSuccessListener<WeatherResponse>() {
                    @Override
                    public void onSuccess(WeatherResponse weatherResponse) {
                        Weather weather = weatherResponse.getWeather();
                        String text = "";

                        for (int condition : weather.getConditions()) {
                            switch (condition) {
                                case Weather.CONDITION_RAINY:
                                    text += " Rainy weather condition.";
                                    break;

                                default :
                                    text += "It's not raining";
                            }
                        }

                        Log.d("weather", text);
                        //  textView_main2.setText("weather: "+text);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("weather", "Could not get Weather: " + e);
                        Toast.makeText(GameActivity.this, "Could not get Weather: " + e, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void printNearbyPlaces() {
        checkFineLocationPermission();
        Awareness.getSnapshotClient(this).getPlaces().addOnSuccessListener(new OnSuccessListener<PlacesResponse>() {
            @Override
            public void onSuccess(PlacesResponse placesResponse) {
                List<PlaceLikelihood> pll = placesResponse.getPlaceLikelihoods();

                Log.d("places", "1"+pll.get(0).getPlace().getName().toString());
                Log.d("places", "2"+pll.get(1).getPlace().getName().toString());
                Log.d("places", "3"+pll.get(2).getPlace().getName().toString());

//                textView_main.setText(pll.get(0).getPlace().getName()+"\n"+pll.get(0).getPlace().getLatLng()+"\n"
//                        +pll.get(1).getPlace().getName()+"\n"+pll.get(1).getPlace().getLatLng()+"\n"
//                        +pll.get(2).getPlace().getName()+"\n"+pll.get(2).getPlace().getLatLng());

            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("places", "Could not get Places: " + e);
                        Toast.makeText(GameActivity.this, "Could not get Places: " + e,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public Date getDate() {
        Date date = Calendar.getInstance().getTime();
        return date;
    }
}
