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
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.awareness.snapshot.PlacesResponse;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResult;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import pt.ipleiria.pp.MainActivity;
import pt.ipleiria.pp.R;
import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.model.Task;

public class Descompressao extends AppCompatActivity implements SensorEventListener {

    public static final String TAG_DESCOMPRESSAO = "descompressao";

    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private static final int REQUEST_CODE_FLPERMISSION = 42;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    public boolean screenDown;

    public static final String FENCEKEY = "localfence";
    private MyFenceReceiver fenceReceiver;

    public boolean check = true;
    private int mId = 1;

    private static int progress;
    private ProgressBar progressBar;
    private TextView progressTime;
    private CountDownTimer gameCountDownTimer, timer;
    private static final int min = 10;

    private TextView textViewScreenDown;
    private TextView textViewPlaces;
    public boolean finishTask = false;


    public static final String TAG_FENCE_DES = "descompressao_FENCE";
    private boolean location;
    private PendingIntent myPendingIntentDescompressao;
    private boolean checkPrintPlaces;
    private boolean goodWeather;

    private Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.descompressao);
        //textView_main = findViewById(R.id.textViewScreenDown);
        progressBar = findViewById(R.id.progressBar_Des);
        textViewScreenDown = findViewById(R.id.textViewDescompressao);
        textViewPlaces = findViewById(R.id.textViewDescompressaoPlaces);
        progressTime = findViewById(R.id.progressTime_Des);

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntentDescompressao = PendingIntent.getBroadcast(this, 0, intent, 0);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        fenceReceiver = new MyFenceReceiver();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        Intent i = getIntent();
        task = (Task) i.getSerializableExtra("task");
        task = SingletonPPB.getInstance().containsIDTask(task.getId());

    }

    private void setTimer(int time) {
        progress = 100;
        final int timeDebug = time;
        final int actualTime = time * 1000;
        progressBar.setProgress(progress);

        queryFences();

        gameCountDownTimer = new CountDownTimer(actualTime, 5000) {
            int totalTime = actualTime;

            @Override
            public void onTick(long millisUntilFinished) {
                progress = (int) ((totalTime - millisUntilFinished) / (double) totalTime * 100);
                progressBar.setProgress(progress);

                // ver se esta virado para baixo

                if (progressBar.getProgress() != 0) {
                    Log.d(TAG_DESCOMPRESSAO, "VAR: weather:" + goodWeather + " / screenDown:"
                            + screenDown + " / location:" + location);
                    if (goodWeather & screenDown & location) {

                        Snackbar.make(findViewById(android.R.id.content),
                                "Screen down: " + screenDown, Snackbar.LENGTH_SHORT).show();

                    } else {

                        if (!goodWeather & !location) {
                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(Descompressao.this, android.R.style.Theme_Material_Dialog_Alert);
                            builder.setTitle("Atention!")
                                    .setMessage("Need to go to other place, the weather it's not good!" +
                                            "\nPlease go to the nearest place, choose one of this 3!!")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            checkPrintPlaces = true;
                                            nearbyPlaces();
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .show().setCanceledOnTouchOutside(false);
                            Log.d(TAG_DESCOMPRESSAO, "Need to go to other place, the weather it's not good!");

                            goodWeather = true;
                            Snackbar.make(findViewById(android.R.id.content),
                                    "GO to one of theese 3 places!", Snackbar.LENGTH_LONG).show();
                        }else{
                            if (!location) {
                                //goodWeather = true;
                                Snackbar.make(findViewById(android.R.id.content),
                                        "GO to one of theese 3 places!", Snackbar.LENGTH_LONG).show();
                            }else{
                                if (!screenDown) {
                                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                                    progressBar.setVisibility(View.VISIBLE);
                                    progressTime.setVisibility(View.VISIBLE);
                                    textViewPlaces.setVisibility(View.INVISIBLE);
                                    textViewScreenDown.setText("Put the SCREEN DOWN!");

                                    Snackbar.make(findViewById(android.R.id.content),
                                            "You need to put de screen down!", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        }

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
                progressBar.setProgress(progress);
                // finish activity
                Snackbar.make(findViewById(android.R.id.content), "finish", Snackbar.LENGTH_LONG).show();
                String title = "\tDescompressao: time out!";
                String text = "\tTarefa completa com sucesso!";
                notifyItem(title, text);
                task.setTaskComplete(true);
                Intent intent = new Intent(Descompressao.this, GameActivity.class);
                intent.putExtra("FinishTask",task.getIdGame());
                startActivity(intent);
                finish();

            }
        }.start();

        timer = new CountDownTimer(actualTime, 1000) {


            @Override
            public void onTick(long millisUntilFinished) {
                long millis = millisUntilFinished;
                String hms = String.format("Time: %02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
                progressTime.setText(hms);
            }

            @Override
            public void onFinish() {
                progressTime.setText("Time: FINISH");
                finishTask = true;
            }
        }.start();
    }

    private void checkFineLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                Descompressao.this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    Descompressao.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_FLPERMISSION // todo: declare this constant
            );
        }
        try {
            int locationMode = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCATION_MODE);
            if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                Toast.makeText(this,
                        "Error: high accuracy location mode must be enabled in the device.",
                        Toast.LENGTH_LONG).show();
                return;

            }
        } catch (Settings.SettingNotFoundException e) {
            Toast.makeText(this, "Error: could not access location mode.",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }
    }

    private void weatherState() {
        checkFineLocationPermission();
        Awareness.getSnapshotClient(this).getWeather()
                .addOnSuccessListener(new OnSuccessListener<WeatherResponse>() {
                    @Override
                    public void onSuccess(WeatherResponse weatherResponse) {
                        Weather weather = weatherResponse.getWeather();

                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String weatherCond = "";

                        for (int condition : weather.getConditions()) {
                            switch (condition) {
                                case Weather.CONDITION_CLEAR:
                                    weatherCond = " Clear weather condition.";
                                    goodWeather = false;
                                    break;
                                case Weather.CONDITION_CLOUDY:
                                    weatherCond = " Cloudy weather condition.";
                                    goodWeather = true;
                                    break;
                                case Weather.CONDITION_FOGGY:
                                    weatherCond = " Foggy weather condition.";
                                    goodWeather = true;
                                    break;
                                case Weather.CONDITION_HAZY:
                                    weatherCond = " Hazy weather condition.";
                                    goodWeather = true;
                                    break;
                                case Weather.CONDITION_ICY:
                                    weatherCond = " Icy weather condition.";
                                    goodWeather = false;
                                    break;
                                case Weather.CONDITION_RAINY:
                                    weatherCond = " Rainy weather condition.";
                                    goodWeather = false;
                                    break;
                                case Weather.CONDITION_SNOWY:
                                    weatherCond = " Snowy weather condition.";
                                    goodWeather = false;
                                    break;
                                case Weather.CONDITION_STORMY:
                                    weatherCond = " Stormy weather condition.";
                                    goodWeather = false;
                                    break;
                                case Weather.CONDITION_UNKNOWN:
                                    weatherCond = " Unknown weather condition.";
                                    goodWeather = true;
                                    break;
                                case Weather.CONDITION_WINDY:
                                    weatherCond = " Windy weather condition.";
                                    goodWeather = true;
                                    break;
                            }
                        }

                        location = goodWeather;
                        Log.d(TAG_DESCOMPRESSAO, "VAR: goodWaether: " + goodWeather + " - location: " +location);
                        Log.d(TAG_DESCOMPRESSAO, "Weather condition: " + weatherCond);
                        Toast.makeText(Descompressao.this, "Weather condition:" + weatherCond,
                                Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG_DESCOMPRESSAO, "Could not get Weather: " + e);
                        Toast.makeText(Descompressao.this, "Could not get Weather: " + e,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void nearbyPlaces() {
        checkFineLocationPermission();

        Awareness.getSnapshotClient(this).getPlaces()
                .addOnSuccessListener(new OnSuccessListener<PlacesResponse>() {
                    @Override
                    public void onSuccess(PlacesResponse placesResponse) {
                        List<PlaceLikelihood> pll = placesResponse.getPlaceLikelihoods();

                        int numPlaces = 3;// Show the top 3 possible location results.
                        String plText = "";

                        for (int i = 0; i < (pll.size() < numPlaces ? pll.size() : 3); i++) {
                            PlaceLikelihood pl = pll.get(i);

                            plText += "\t#" + (i + 1) + ": " + pl.getPlace().getName().toString().toUpperCase()
                                    + "\n\tAddress: " + pl.getPlace().getAddress()
                                    + "\n\tLocation: " + pl.getPlace().getLatLng()
                                    + "\n\tPlaceTypes: "
                                    + "\t" + printPlaceTypes(pl.getPlace().getPlaceTypes());
                            if(i==(numPlaces-1)){
                                plText+= "\n";
                            }else{
                                plText+= "\n\t--or--\n";
                            }

                        }
                        checkFineLocationPermission();

                        AwarenessFence nearbyPlace1 =  LocationFence.in(pll.get(0).getPlace().getLatLng().latitude, pll.get(0).getPlace().getLatLng().longitude,50, 0L);
                        AwarenessFence nearbyPlace2 =  LocationFence.in(pll.get(1).getPlace().getLatLng().latitude, pll.get(1).getPlace().getLatLng().longitude,50, 0L);
                        AwarenessFence nearbyPlace3 =  LocationFence.in(pll.get(2).getPlace().getLatLng().latitude, pll.get(2).getPlace().getLatLng().longitude,50, 0L);
                        AwarenessFence local = AwarenessFence.or(nearbyPlace1,nearbyPlace2,nearbyPlace3);
                        addFence(FENCEKEY, local);

                        if(checkPrintPlaces){
                            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                            progressTime.setVisibility(View.INVISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                            textViewPlaces.setVisibility(View.VISIBLE);
                            textViewPlaces.setText(plText);
                            textViewScreenDown.setText("Go to the nearest place");
                        }else{
                            Log.d(TAG_DESCOMPRESSAO, "Create fences:  just creating fences!");
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG_DESCOMPRESSAO, "Could not get Places: " + e);
                        Toast.makeText(Descompressao.this, "Could not get Places: " + e,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String printPlaceTypes(List<Integer> placeTypes) {
        String res = "";

        for (int placeType :
                placeTypes) {

            switch (placeType) {
                case Place.TYPE_ACCOUNTING:
                    res += "TYPE_ACCOUNTING";
                    break;
                case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_1:
                    res += "TYPE_ADMINISTRATIVE_AREA_LEVEL_1";
                    break;
                case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_2:
                    res += "TYPE_ADMINISTRATIVE_AREA_LEVEL_2";
                    break;
                case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_3:
                    res += "TYPE_ADMINISTRATIVE_AREA_LEVEL_3";
                    break;
                case Place.TYPE_AIRPORT:
                    res += "TYPE_AIRPORT";
                    break;
                case Place.TYPE_AMUSEMENT_PARK:
                    res += "TYPE_AMUSEMENT_PARK";
                    break;
                case Place.TYPE_AQUARIUM:
                    res += "TYPE_AQUARIUM";
                    break;
                case Place.TYPE_ART_GALLERY:
                    res += "TYPE_ART_GALLERY";
                    break;
                case Place.TYPE_ATM:
                    res += "TYPE_ATM";
                    break;
                case Place.TYPE_BAKERY:
                    res += "TYPE_BAKERY";
                    break;
                case Place.TYPE_BANK:
                    res += "TYPE_BANK";
                    break;
                case Place.TYPE_BAR:
                    res += "TYPE_BAR";
                    break;
                case Place.TYPE_BEAUTY_SALON:
                    res += "TYPE_BEAUTY_SALON";
                    break;
                case Place.TYPE_BICYCLE_STORE:
                    res += "TYPE_BICYCLE_STORE";
                    break;
                case Place.TYPE_BOOK_STORE:
                    res += "TYPE_BOOK_STORE";
                    break;
                case Place.TYPE_BOWLING_ALLEY:
                    res += "TYPE_BOWLING_ALLEY";
                    break;
                case Place.TYPE_BUS_STATION:
                    res += "TYPE_BUS_STATION";
                    break;
                case Place.TYPE_CAFE:
                    res += "TYPE_CAFE";
                    break;
                case Place.TYPE_CAMPGROUND:
                    res += "TYPE_CAMPGROUND";
                    break;
                case Place.TYPE_CAR_DEALER:
                    res += "TYPE_CAR_DEALER";
                    break;
                case Place.TYPE_CAR_RENTAL:
                    res += "TYPE_CAR_RENTAL";
                    break;
                case Place.TYPE_CAR_REPAIR:
                    res += "TYPE_CAR_REPAIR";
                    break;
                case Place.TYPE_CAR_WASH:
                    res += "TYPE_CAR_WASH";
                    break;
                case Place.TYPE_CASINO:
                    res += "TYPE_CASINO";
                    break;
                case Place.TYPE_CEMETERY:
                    res += "TYPE_CEMETERY";
                    break;
                case Place.TYPE_CHURCH:
                    res += "TYPE_CHURCH";
                    break;
                case Place.TYPE_CITY_HALL:
                    res += "TYPE_CITY_HALL";
                    break;
                case Place.TYPE_CLOTHING_STORE:
                    res += "TYPE_CLOTHING_STORE";
                    break;
                case Place.TYPE_COLLOQUIAL_AREA:
                    res += "TYPE_COLLOQUIAL_AREA";
                    break;
                case Place.TYPE_CONVENIENCE_STORE:
                    res += "TYPE_CONVENIENCE_STORE";
                    break;
                case Place.TYPE_COUNTRY:
                    res += "TYPE_COUNTRY";
                    break;
                case Place.TYPE_COURTHOUSE:
                    res += "TYPE_COURTHOUSE";
                    break;
                case Place.TYPE_DENTIST:
                    res += "TYPE_DENTIST";
                    break;
                case Place.TYPE_DEPARTMENT_STORE:
                    res += "TYPE_DEPARTMENT_STORE";
                    break;
                case Place.TYPE_DOCTOR:
                    res += "TYPE_DOCTOR";
                    break;
                case Place.TYPE_ELECTRICIAN:
                    res += "TYPE_ELECTRICIAN";
                    break;
                case Place.TYPE_ELECTRONICS_STORE:
                    res += "TYPE_ELECTRONICS_STORE";
                    break;
                case Place.TYPE_EMBASSY:
                    res += "TYPE_EMBASSY";
                    break;
                case Place.TYPE_ESTABLISHMENT:
                    res += "TYPE_ESTABLISHMENT";
                    break;
                case Place.TYPE_FINANCE:
                    res += "TYPE_FINANCE";
                    break;
                case Place.TYPE_FIRE_STATION:
                    res += "TYPE_FIRE_STATION";
                    break;
                case Place.TYPE_FLOOR:
                    res += "TYPE_FLOOR";
                    break;
                case Place.TYPE_FLORIST:
                    res += "TYPE_FLORIST";
                    break;
                case Place.TYPE_FOOD:
                    res += "TYPE_FOOD";
                    break;
                case Place.TYPE_FUNERAL_HOME:
                    res += "TYPE_FUNERAL_HOME";
                    break;
                case Place.TYPE_FURNITURE_STORE:
                    res += "TYPE_FURNITURE_STORE";
                    break;
                case Place.TYPE_GAS_STATION:
                    res += "TYPE_GAS_STATION";
                    break;
                case Place.TYPE_GENERAL_CONTRACTOR:
                    res += "TYPE_GENERAL_CONTRACTOR";
                    break;
                case Place.TYPE_GEOCODE:
                    res += "TYPE_GEOCODE";
                    break;
                case Place.TYPE_GROCERY_OR_SUPERMARKET:
                    res += "TYPE_GROCERY_OR_SUPERMARKET";
                    break;
                case Place.TYPE_GYM:
                    res += "TYPE_GYM";
                    break;
                case Place.TYPE_HAIR_CARE:
                    res += "TYPE_HAIR_CARE";
                    break;
                case Place.TYPE_HARDWARE_STORE:
                    res += "TYPE_HARDWARE_STORE";
                    break;
                case Place.TYPE_HEALTH:
                    res += "TYPE_HEALTH";
                    break;
                case Place.TYPE_HINDU_TEMPLE:
                    res += "TYPE_HINDU_TEMPLE";
                    break;
                case Place.TYPE_HOME_GOODS_STORE:
                    res += "TYPE_HOME_GOODS_STORE";
                    break;
                case Place.TYPE_HOSPITAL:
                    res += "TYPE_HOSPITAL";
                    break;
                case Place.TYPE_INSURANCE_AGENCY:
                    res += "TYPE_INSURANCE_AGENCY";
                    break;
                case Place.TYPE_INTERSECTION:
                    res += "TYPE_INTERSECTION";
                    break;
                case Place.TYPE_JEWELRY_STORE:
                    res += "TYPE_JEWELRY_STORE";
                    break;
                case Place.TYPE_LAUNDRY:
                    res += "TYPE_LAUNDRY";
                    break;
                case Place.TYPE_LAWYER:
                    res += "TYPE_LAWYER";
                    break;
                case Place.TYPE_LIBRARY:
                    res += "TYPE_LIBRARY";
                    break;
                case Place.TYPE_LIQUOR_STORE:
                    res += "TYPE_LIQUOR_STORE";
                    break;
                case Place.TYPE_LOCALITY:
                    res += "TYPE_LOCALITY";
                    break;
                case Place.TYPE_LOCAL_GOVERNMENT_OFFICE:
                    res += "TYPE_LOCAL_GOVERNMENT_OFFICE";
                    break;
                case Place.TYPE_LOCKSMITH:
                    res += "TYPE_LOCKSMITH";
                    break;
                case Place.TYPE_LODGING:
                    res += "TYPE_LODGING";
                    break;
                case Place.TYPE_MEAL_DELIVERY:
                    res += "TYPE_MEAL_DELIVERY";
                    break;
                case Place.TYPE_MEAL_TAKEAWAY:
                    res += "TYPE_MEAL_TAKEAWAY";
                    break;
                case Place.TYPE_MOSQUE:
                    res += "TYPE_MOSQUE";
                    break;
                case Place.TYPE_MOVIE_RENTAL:
                    res += "TYPE_MOVIE_RENTAL";
                    break;
                case Place.TYPE_MOVIE_THEATER:
                    res += "TYPE_MOVIE_THEATER";
                    break;
                case Place.TYPE_MOVING_COMPANY:
                    res += "TYPE_MOVING_COMPANY";
                    break;
                case Place.TYPE_MUSEUM:
                    res += "TYPE_MUSEUM";
                    break;
                case Place.TYPE_NATURAL_FEATURE:
                    res += "TYPE_NATURAL_FEATURE";
                    break;
                case Place.TYPE_NEIGHBORHOOD:
                    res += "TYPE_NEIGHBORHOOD";
                    break;
                case Place.TYPE_NIGHT_CLUB:
                    res += "TYPE_NIGHT_CLUB";
                    break;
                case Place.TYPE_OTHER:
                    res += "TYPE_OTHER";
                    break;
                case Place.TYPE_PAINTER:
                    res += "TYPE_PAINTER";
                    break;
                case Place.TYPE_PARK:
                    res += "TYPE_PARK";
                    break;
                case Place.TYPE_PARKING:
                    res += "TYPE_PARKING";
                    break;
                case Place.TYPE_PET_STORE:
                    res += "TYPE_PET_STORE";
                    break;
                case Place.TYPE_PHARMACY:
                    res += "TYPE_PHARMACY";
                    break;
                case Place.TYPE_PHYSIOTHERAPIST:
                    res += "TYPE_PHYSIOTHERAPIST";
                    break;
                case Place.TYPE_PLACE_OF_WORSHIP:
                    res += "TYPE_PLACE_OF_WORSHIP";
                    break;
                case Place.TYPE_PLUMBER:
                    res += "TYPE_PLUMBER";
                    break;
                case Place.TYPE_POINT_OF_INTEREST:
                    res += "TYPE_POINT_OF_INTEREST";
                    break;
                case Place.TYPE_POLICE:
                    res += "TYPE_POLICE";
                    break;
                case Place.TYPE_POLITICAL:
                    res += "TYPE_POLITICAL";
                    break;
                case Place.TYPE_POSTAL_CODE:
                    res += "TYPE_POSTAL_CODE";
                    break;
                case Place.TYPE_POSTAL_CODE_PREFIX:
                    res += "TYPE_POSTAL_CODE_PREFIX";
                    break;
                case Place.TYPE_POSTAL_TOWN:
                    res += "TYPE_POSTAL_TOWN";
                    break;
                case Place.TYPE_POST_BOX:
                    res += "TYPE_POST_BOX";
                    break;
                case Place.TYPE_POST_OFFICE:
                    res += "TYPE_POST_OFFICE";
                    break;
                case Place.TYPE_PREMISE:
                    res += "TYPE_PREMISE";
                    break;
                case Place.TYPE_REAL_ESTATE_AGENCY:
                    res += "TYPE_REAL_ESTATE_AGENCY";
                    break;
                case Place.TYPE_RESTAURANT:
                    res += "TYPE_RESTAURANT";
                    break;
                case Place.TYPE_ROOFING_CONTRACTOR:
                    res += "TYPE_ROOFING_CONTRACTOR";
                    break;
                case Place.TYPE_ROOM:
                    res += "TYPE_ROOM";
                    break;
                case Place.TYPE_ROUTE:
                    res += "TYPE_ROUTE";
                    break;
                case Place.TYPE_RV_PARK:
                    res += "TYPE_RV_PARK";
                    break;
                case Place.TYPE_SCHOOL:
                    res += "TYPE_SCHOOL";
                    break;
                case Place.TYPE_SHOE_STORE:
                    res += "TYPE_SHOE_STORE";
                    break;
                case Place.TYPE_SHOPPING_MALL:
                    res += "TYPE_SHOPPING_MALL";
                    break;
                case Place.TYPE_SPA:
                    res += "TYPE_SPA";
                    break;
                case Place.TYPE_STADIUM:
                    res += "TYPE_STADIUM";
                    break;
                case Place.TYPE_STORAGE:
                    res += "TYPE_STORAGE";
                    break;
                case Place.TYPE_STORE:
                    res += "TYPE_STORE";
                    break;
                case Place.TYPE_STREET_ADDRESS:
                    res += "TYPE_STREET_ADDRESS";
                    break;
                case Place.TYPE_SUBLOCALITY:
                    res += "TYPE_SUBLOCALITY";
                    break;
                case Place.TYPE_SUBLOCALITY_LEVEL_1:
                    res += "TYPE_SUBLOCALITY_LEVEL_1";
                    break;
                case Place.TYPE_SUBLOCALITY_LEVEL_2:
                    res += "TYPE_SUBLOCALITY_LEVEL_2";
                    break;
                case Place.TYPE_SUBLOCALITY_LEVEL_3:
                    res += "TYPE_SUBLOCALITY_LEVEL_3";
                    break;
                case Place.TYPE_SUBLOCALITY_LEVEL_4:
                    res += "TYPE_SUBLOCALITY_LEVEL_4";
                    break;
                case Place.TYPE_SUBLOCALITY_LEVEL_5:
                    res += "TYPE_SUBLOCALITY_LEVEL_5";
                    break;
                case Place.TYPE_SUBPREMISE:
                    res += "TYPE_SUBPREMISE";
                    break;
                case Place.TYPE_SUBWAY_STATION:
                    res += "TYPE_SUBWAY_STATION";
                    break;
                case Place.TYPE_SYNAGOGUE:
                    res += "TYPE_SYNAGOGUE";
                    break;
                case Place.TYPE_SYNTHETIC_GEOCODE:
                    res += "TYPE_SYNTHETIC_GEOCODE";
                    break;
                case Place.TYPE_TAXI_STAND:
                    res += "TYPE_TAXI_STAND";
                    break;
                case Place.TYPE_TRAIN_STATION:
                    res += "TYPE_TRAIN_STATION";
                    break;
                case Place.TYPE_TRANSIT_STATION:
                    res += "TYPE_TRANSIT_STATION";
                    break;
                case Place.TYPE_TRAVEL_AGENCY:
                    res += "TYPE_TRAVEL_AGENCY";
                    break;
                case Place.TYPE_UNIVERSITY:
                    res += "TYPE_UNIVERSITY";
                    break;
                case Place.TYPE_VETERINARY_CARE:
                    res += "TYPE_VETERINARY_CARE";
                    break;
                case Place.TYPE_ZOO:
                    res += "TYPE_ZOO";
                    break;
            }

            res += "\t";
        }

        return res;
    }

    //FENCES
    private void addFence(final String fenceKey, final AwarenessFence fence) {
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(fenceKey, fence, myPendingIntentDescompressao)
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

    public class MyFenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            FenceState fenceState = FenceState.extract(intent);
            if (TextUtils.equals(fenceState.getFenceKey(), FENCEKEY)) {
                Log.d(TAG_DESCOMPRESSAO, "VAR: weather:" + goodWeather + " / screenDown:"
                        + screenDown + " / location:" + location);
                switch(fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        location=true;
                        goodWeather = true;
                        Log.i(TAG_FENCE_DES, "Fence state: Chegou a um dos 3 places.");
                        break;
                    case FenceState.FALSE:
                        location=false;
                        Log.i(TAG_FENCE_DES, "Fence state: Ainda não chegou a um dos 3 places.");
                        break;
                    case FenceState.UNKNOWN:
                        //goodWeather=true;
                        Log.i(TAG_FENCE_DES, "Fence state: Unknown state.");
                        break;
                }
                Log.d(TAG_DESCOMPRESSAO, "VAR: weather:" + goodWeather + " / screenDown:"
                        + screenDown + " / location:" + location);
            }
        }
    }

    protected void queryFences() {

        //final TextView textView_main = findViewById(R.id.textView2);
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
                        Log.d(TAG_DESCOMPRESSAO,"QueryFence: " + text);
                        //textView_main.setText(text + textView_main.getText());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String text = "\n\n[Fences @ " + timestamp + "]\n"
                                + "Fences could not be queried: " + e.getMessage();
                        Log.d(TAG_DESCOMPRESSAO,"QueryFence" + text);
                        //textView_main.setText(text + textView_main.getText());
                    }
                });
    }

    //SENSOR
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        screenDown = sensorEvent.values[2] < 0 ? true : false;
        Log.d(TAG_DESCOMPRESSAO, "ScreenDown: " + screenDown);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG_DESCOMPRESSAO, sensor.getName() + " accuracy changed to " + accuracy);
//        Toast.makeText(Descompressao.this, sensor.getName() + "accuracy changed to " + accuracy,
//                Toast.LENGTH_SHORT).show();
    }

    //NOTIFICAÇÃO
    private void notifyItem(String title, String text) {
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

    //---------
    @Override
    protected void onResume() {
        super.onResume();
        removeFences(FENCEKEY);
        if(!finishTask){
            checkPrintPlaces = false;
            nearbyPlaces();
        }else {
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(Descompressao.this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle("Task alreay completed!")
                    .setMessage("Task is already completed with sucess!" +
                            "\nPlease choose another one!!")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ///mandar para a task activity
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show().setCanceledOnTouchOutside(false);
        }
        weatherState();
        setTimer(60 * min);
    }

    @Override
    protected void onPause() {
        super.onPause();
        clearnotify();
        removeFences(FENCEKEY);
        gameCountDownTimer.cancel();
        gameCountDownTimer = null;
        timer.cancel();
        timer = null;
    }


}