package pt.ipleiria.pp;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.model.Task;

public class MelhorCurso extends AppCompatActivity {

    private Button button;
    private TextView textView;
    private Task task;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.melhorcurso);

        button = (Button) this.findViewById(R.id.button);


        Intent i = getIntent();
        task = (Task) i.getSerializableExtra("task");
        task = SingletonPPB.getInstance().containsIDTask(task.getId());

        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_record_voice, 0, 0, 0);
        button.setCompoundDrawablePadding(100);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
                try{
                    startActivityForResult(intent,200);
                }catch (ActivityNotFoundException a){

                    Snackbar.make(findViewById(android.R.id.content), "Intent problem!", Snackbar.LENGTH_LONG).show();

                }
            }
        });

        Snackbar.make(findViewById(android.R.id.content), "Press the button to answer the question!", Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 200){
            if(resultCode == RESULT_OK && data != null){
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);


                if(result.get(0).toUpperCase().contains("ELECTRO")||result.get(0).toUpperCase().contains("ENGENHARIA ELETROTÉCNICA E DE COMPUTADORES")||result.get(0).toUpperCase().contains("ENGENHARIA ELETROTÉCNICA")){

                    Snackbar.make(findViewById(android.R.id.content), "Finish!", Snackbar.LENGTH_LONG).show();

                    task.setTaskComplete(true);
                    Intent intent = new Intent(MelhorCurso.this, GameActivity.class);
                    intent.putExtra("FinishTask",task.getIdGame());
                    startActivity(intent);
                    finish();

                }else{
                    Snackbar.make(findViewById(android.R.id.content), "Try again!", Snackbar.LENGTH_LONG).show();
                }
            }
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