package com.luxjim.letzlearn;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncTask;
import static com.luxjim.letzlearn.db.DatabaseHelper.CARD_TABLE;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import com.luxjim.letzlearn.db.Card;
import com.luxjim.letzlearn.db.CardDAO;
import com.luxjim.letzlearn.db.CardListAdapter;
import com.luxjim.letzlearn.db.DatabaseHelper;
import com.luxjim.letzlearn.language.Language;
import com.luxjim.letzlearn.translate.Translate;

public class MainActivity extends AppCompatActivity {

   TextView mLink;
    TextView txt1;
    EditText edt1;
    private String translatedText = null;
    CardDAO cardDAO;
    DatabaseHelper databaseHelper;
    SQLiteDatabase sqLiteDatabase;

    private AdView mBannerAd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dico);

        //admob
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });
        //ads
        AdView mBannerAd = findViewById(R.id.adView);
        //mBannerAd.setAdUnitId(getString(R.string.ads_banner_id));
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("")
                .build();
        mBannerAd.loadAd(adRequest);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(getResources().getDrawable(R.drawable.logo2));
        actionBar.setTitle("  " + "Lëtz Learn");
        actionBar.setDisplayShowHomeEnabled(true);
        //setSupportActionBar(toolbar);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        // getSupportActionBar().setIcon(getResources().getDrawable(R.drawable.logo2));
        //getSupportActionBar().setTitle("Lëtz Learn");

        txt1 = findViewById(R.id.textview3);
        edt1 = findViewById(R.id.edtext1);
        edt1.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edt1.setRawInputType(InputType.TYPE_CLASS_TEXT);
        edt1.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {

                    Translate.setKey(ApiKeys.YANDEX_API_KEY);
                    final String term = edt1.getText().toString();

                    translatedText = Translate.execute(term, Language.FRENCH, Language.LUXEMBOURGISH);
                    txt1.setText(translatedText);

                    final String abc = translatedText.trim().replace(" ", "+");

                    String INPUT_TEXT = "INPUT_TEXT=" + abc;
                    Log.d("WAJIM******************",  INPUT_TEXT);
                    String INPUT_TYPE = "INPUT_TYPE=TEXT";
                    String OUTPUT_TYPE = "OUTPUT_TYPE=AUDIO";
                    String LOCALE = "LOCALE=lb";
                    String AUDIO = "AUDIO=WAVE_FILE";
                    
                    //Params to send to MaryTTS remote test server
                    
                    String parameters = INPUT_TEXT + "&" + INPUT_TYPE + "&" + OUTPUT_TYPE + "&" + LOCALE + "&" + AUDIO;
                    
                    //Language request at MaryTTS remote test server 
                    
                    Uri uri = Uri.parse("http://mary.dfki.de:59125/process?" + parameters);
                    MediaPlayer player = new MediaPlayer();
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setDataSource(getApplicationContext(), uri);
                    player.prepare();
                    player.start();

                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }

            }
        });
        SharedPreferences preferences=getSharedPreferences("wordlist",MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();

        editor.putString("luxembourgish",txt1.getText().toString());
        editor.putString("french",edt1.getText().toString());
        editor.commit();

        mLink = findViewById(R.id.textview5);
        if (mLink != null) {
            mLink.setMovementMethod(LinkMovementMethod.getInstance());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_dico, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_about:
                Intent intent = new Intent(getBaseContext(), LuxActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_lexicon:
                Intent i_showdb = new Intent(getBaseContext(), WordListActivity.class);
                startActivity(i_showdb);
                return true;

            //case R.id.action_saveData:
            //updatecardDB();
            //Toast.makeText(getApplicationContext(),"Update item selected...",Toast.LENGTH_LONG).show();
            case R.id.action_saveData:
                saveData();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void saveData() {
        final String luxembourgish = txt1.getText().toString();
        final String french = edt1.getText().toString();

        cardDAO = new CardDAO(getApplicationContext());
        databaseHelper = new DatabaseHelper(getApplicationContext());
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.LUXEMBOURGISH_COLUMN, luxembourgish );

        values.put(DatabaseHelper.FRENCH_COLUMN, french);
        long rowId = sqLiteDatabase.insert(CARD_TABLE, null, values);
        Toast.makeText(getApplicationContext(),"Data saved",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBannerAd != null) {
            mBannerAd.resume();
        }

    }

    @Override
    public void onPause() {
        if (mBannerAd != null) {
            mBannerAd.pause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mBannerAd != null) {
            mBannerAd.destroy();
        }

        super.onDestroy();
    }


}
