package com.example.aniket.clipboardtest;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class MyService extends Service {
    private WindowManager windowManager;
    private LinearLayout overheadview;
    String pasteData;
    private ClipboardManager mCM;
    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("service","Start");
        mCM = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mCM.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {

            @Override
            public void onPrimaryClipChanged() {
                if (mCM!=null)
                    if (mCM.getPrimaryClip()!=null && mCM.getPrimaryClip().getItemAt(0)!=null) {
                        String newClip = "" +  mCM.getPrimaryClip().getItemAt(0).getText();
                        Log.d("copied", "true");
                        SharedPreferences preferences = getApplicationContext().getSharedPreferences("service", MODE_PRIVATE);
                        if (preferences.getBoolean("start_service", true) && !isconnected() && newClip!=null && !preferences.getBoolean("app_open",false))
                            qrcode(newClip);
                    }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    private void qrcode(String pasteData) {
        Log.d("qrcode","Starts");

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(pasteData, BarcodeFormat.QR_CODE,500,500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            windowManager= (WindowManager) getSystemService(WINDOW_SERVICE);
            LayoutInflater inflater= (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            WindowManager.LayoutParams params=new WindowManager.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels* 0.8f),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
            params.gravity= Gravity.CENTER;
            overheadview= (LinearLayout) inflater.inflate(R.layout.barcode_layout,null);
            ImageView image= (ImageView) overheadview.findViewById(R.id.wop_image);
            TextView textView= (TextView) overheadview.findViewById(R.id.wop_text);
            image.setImageBitmap(bitmap);
            textView.setText(pasteData);
            ImageView close= (ImageView) overheadview.findViewById(R.id.close);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    windowManager.removeView(overheadview);
                }
            });
            windowManager.addView(overheadview,params);
        } catch (WriterException e) {
            e.printStackTrace();
            Log.e("catch",e.toString());
        }
    }

    private boolean isconnected() {
        SharedPreferences preferences=getApplicationContext().getSharedPreferences("service",MODE_PRIVATE);
        if (!preferences.getBoolean("check_net",false))
            return false;
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
