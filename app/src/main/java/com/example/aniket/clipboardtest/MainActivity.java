package com.example.aniket.clipboardtest;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.gms.vision.text.Text;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;


public class MainActivity extends AppCompatActivity {
    TextView text;
    ImageView image;
    int red=0xffff0000;
    Switch backgroundSwitch,offlineSwitch;
    SharedPreferences.Editor editor;
    FloatingActionButton fab;
    String scanContent,scanFormat;
    ClipboardManager clipboard;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        text = (TextView) findViewById(R.id.text);
        image = (ImageView) findViewById(R.id.image);
        preferences=getApplicationContext().getSharedPreferences("service",MODE_PRIVATE);
        editor=preferences.edit();
        editor.putBoolean("app_open", true);
        editor.apply();
        backgroundSwitch= (Switch) findViewById(R.id.background_switch);
        backgroundSwitch.setChecked(preferences.getBoolean("start_service",true));
        offlineSwitch= (Switch) findViewById(R.id.offline_switch);
        offlineSwitch.setChecked(preferences.getBoolean("check_net",false));
        backgroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                editor.putBoolean("start_service", b);
                editor.apply();
            }
        });
        offlineSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editor.putBoolean("check_net",b);
                editor.apply();
            }
        });
        fab= (FloatingActionButton) findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             scanbr();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        editor=preferences.edit();
        editor.putBoolean("app_open", false);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        editor=preferences.edit();
        editor.putBoolean("app_open", true);
        editor.apply();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            if (scanningResult.getContents() != null) {
                scanContent = scanningResult.getContents();
                scanFormat = scanningResult.getFormatName();
            }

            if (scanContent!=null)
                Toast.makeText(getApplicationContext(),scanContent,Toast.LENGTH_LONG).show();
            ClipData data=ClipData.newPlainText("simple text",scanContent);
            clipboard.setPrimaryClip(data);

        }else{
            Toast.makeText(this,"Nothing scanned",Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        String pasteData = "";
        startService(new Intent(getApplicationContext(),MyService.class));
        if (clipboard!=null)
        if (clipboard.getPrimaryClip()!=null && clipboard.getPrimaryClip().getItemAt(0)!=null) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            pasteData = "" + item.getText();
            if (pasteData!=null) {
                text.setText(pasteData);
                qrcode(pasteData);
            }
        }
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if (!Settings.canDrawOverlays(this)){
                final AlertDialog.Builder builder=new AlertDialog.Builder(this);
                builder.setTitle("Allow Permission")
                        .setMessage("This app needs permission to draw over other apps. Enable drawing over other apps")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:"+getPackageName()));
                                startActivityForResult(intent,1749);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                builder.show();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        }

        if(preferences.getBoolean("first_open",true))
        TapTargetView.showFor(this,                 // `this` is an Activity
                TapTarget.forView(findViewById(R.id.floatingActionButton), "Scan QR code", "Click this button to scan any QR code")
                        // All options below are optional
                        .outerCircleColor(R.color.colorAccent)      // Specify a color for the outer circle
                        .targetCircleColor(R.color.white)   // Specify a color for the target circle
                        .titleTextSize(24)                  // Specify the size (in sp) of the title text
                        .titleTextColor(R.color.white)      // Specify the color of the title text
                        .descriptionTextSize(14)            // Specify the size (in sp) of the description text
                        .descriptionTextColor(R.color.red)  // Specify the color of the description text
                        // Specify a color for both the title and description text
                        .textTypeface(Typeface.SANS_SERIF)  // Specify a typeface for the text
                        .dimColor(R.color.white)            // If set, will dim behind the view with 30% opacity of the given color
                        .drawShadow(true)                   // Whether to draw a drop shadow or not
                        .cancelable(false)                  // Whether tapping outside the outer circle dismisses the view
                        .tintTarget(true)                   // Whether to tint the target view's color
                        .transparentTarget(false)           // Specify whether the target is transparent (displays the content underneath)
                        .icon(getDrawable(R.drawable.ic_add_a_photo))                     // Specify a custom drawable to draw as the target
                        .targetRadius(30),                  // Specify the target radius (in dp)
                new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                    @Override
                    public void onTargetClick(TapTargetView view) {
                        super.onTargetClick(view);      // This call is optional

                    }
                });
        editor=preferences.edit();
        editor.putBoolean("first_open", false);
        editor.apply();
    }


    private void qrcode(String pasteData) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(pasteData, BarcodeFormat.QR_CODE,500,500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            image.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }


    private void scanbr() {
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
        scanIntegrator.setPrompt("Scan");
        scanIntegrator.setBeepEnabled(true);
        //The following line if you want QR code
        scanIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        scanIntegrator.setOrientationLocked(false);
        scanIntegrator.setBarcodeImageEnabled(true);
        scanIntegrator.initiateScan();
    }
}
