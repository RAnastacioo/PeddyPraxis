package pt.ipleiria.pp;


import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import pt.ipleiria.pp.model.Game;
import pt.ipleiria.pp.model.SingletonPPB;


public class SplashScreenActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        //Animation image
        // load the animation
        Animation animRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
        ImageView imagemSplash = findViewById(R.id.id_launcher_icon);
        imagemSplash.startAnimation(animRotate);

            Thread welcomeThread = new Thread() {
                @Override
                public void run() {
                    try {
                        super.run();
                        try {
                            FileInputStream fileInputStream = openFileInput("game.bin");
                            ObjectInputStream objectInputStream = new
                                    ObjectInputStream(fileInputStream);
                            SingletonPPB.getInstance().setGames((ArrayList<Game>) objectInputStream.readObject());
                            objectInputStream.close();
                            fileInputStream.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(SplashScreenActivity.this,
                                    getString(R.string.could_not_read_game_from_internal),
                                    Toast.LENGTH_LONG).show();
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(SplashScreenActivity.this,
                                    getString(R.string.error_reading_game_from_internal),
                                    Toast.LENGTH_LONG).show();
                        }
                        sleep(500);

                    } catch (Exception e) {

                    } finally {

                        goMain();

                    }
                }
            };
            welcomeThread.start();
    }


    private void goMain() {
        Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}