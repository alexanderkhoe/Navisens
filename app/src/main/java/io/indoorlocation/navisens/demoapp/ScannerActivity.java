package io.indoorlocation.navisens.demoapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.RelativeLayout;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerActivity extends Activity implements ZXingScannerView.ResultHandler{

    private ZXingScannerView qrView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.scanner_activity);
        init();
    }

    private void init() {
        //Scanner
        qrView = new ZXingScannerView(this);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.relative_scan_take_single);
        rl.addView(qrView);
        qrView.setResultHandler(this);
        qrView.startCamera();
        qrView.setSoundEffectsEnabled(true);
        qrView.setAutoFocus(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (qrView != null) {
            qrView.setResultHandler(this);
            qrView.startCamera();
        }
    }

    @Override
    public void onPause() {
        if (qrView != null){
            qrView.stopCamera();
        }
        super.onPause();
    }

    @Override
    public void handleResult(Result rawResult) {
        Intent intent = new Intent();
        intent.putExtra("url", rawResult.getText());
        setResult(Activity.RESULT_OK, intent);
        finish();

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                qrView = new ZXingScannerView(this);
                setContentView(R.layout.scanner_activity);
                qrView.startCamera();

            }
        }
    }

}
