package com.uhf.uhfdemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.uhf.util.MLog;
import com.tencent.mmkv.MMKV;
import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;

import realid.rfidlib.CommonUtil;

public class SelectActivity extends AppCompatActivity {


    private MMKV mkv;
    private Button umModule, slrModule,rmModule,gxModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);
        mkv = MMKV.defaultMMKV();
        umModule = findViewById(R.id.umModule);
        slrModule = findViewById(R.id.slrModule);
        rmModule = findViewById(R.id.rmModule);
        gxModule = findViewById(R.id.gxModule);
        String moduleType = mkv.decodeString(CommonUtil.CURRENT_UHF_MODULE, "");
        MLog.e("moduleType = " + moduleType);

        String baud = mkv.decodeString("baud","");

        if (TextUtils.isEmpty(moduleType) ) {
            umModule.setVisibility(View.VISIBLE);
            slrModule.setVisibility(View.VISIBLE);
            rmModule.setVisibility(View.VISIBLE);
            gxModule.setVisibility(View.VISIBLE);
            if (MyApp.isChangeBaud) {
                if (TextUtils.isEmpty(baud))
                    onSelectBaudEvent();
            }
        } else {
            MyApp.getMyApp().setUhfMangerImpl(UHFManager.getUHFImplSigleInstance(UHFModuleType.valueOf(moduleType)));
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    public void onClick(View view) {
        UHFModuleType mType = UHFModuleType.UM_MODULE;
        switch (view.getId()) {
            case R.id.umModule:
                mType = UHFModuleType.UM_MODULE;
                break;
            case R.id.slrModule:
                mType = UHFModuleType.SLR_MODULE;
                break;
            case R.id.rmModule:
                mType = UHFModuleType.RM_MODULE;
                break;
            case R.id.gxModule:
                mType = UHFModuleType.GX_MODULE;
                break;
            default:
                break;
        }
        mkv.encode(CommonUtil.CURRENT_UHF_MODULE, mType.name());
        MyApp.getMyApp().setUhfMangerImpl(UHFManager.getUHFImplSigleInstance(mType));
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void onSelectBaudEvent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.select_baud));
        final String[] items = getResources().getStringArray(R.array.baud);
        int val = 0;

        builder.setSingleChoiceItems(items, val, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mkv.encode("baud", which == 1 ? "921600" : "115200");
            }
        });

        builder.setPositiveButton(getResources().getString(R.string.dialog_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}
