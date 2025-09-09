package com.uhf.uhfdemo;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.uhf.event.BackResult;
import com.uhf.event.BaseFragment;
import com.uhf.event.GetRFIDThread;
import com.uhf.event.OnLowPower;
import com.uhf.util.MUtil;
import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;

import java.util.Objects;

import static android.text.TextUtils.isEmpty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class SearchFragment extends BaseFragment implements View.OnClickListener, BackResult, OnLowPower {

    private EditText etSetAds, etSetLen, etSetData;
    private CheckBox cBSetFilterSave;
    private Spinner spSetFilterMb;
    private Button Bt_SetFilter, Bt_Clear;

    private TextView currentTag, showRssi;
    private ProgressBar singnalStrength;
    private Button searchFilterTag;
    private String TAG;
    private String content;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);
        TAG = "SearchFragment";
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    public void initView(View v) {
        spSetFilterMb = v.findViewById(R.id.spinner_MB);
        etSetAds = v.findViewById(R.id.Et_set_ads);
        etSetLen = v.findViewById(R.id.Et_Set_len);
        etSetData = v.findViewById(R.id.Et_Set_data);
        cBSetFilterSave = v.findViewById(R.id.CB_Save);
        if (UHFModuleType.SLR_MODULE == UHFManager.getType()) {
            cBSetFilterSave.setVisibility(View.GONE);
        }

        Bt_SetFilter = v.findViewById(R.id.Bt_SetFilter);
        Bt_Clear = v.findViewById(R.id.Bt_Clear);
        Bt_SetFilter.setOnClickListener(this);
        Bt_Clear.setOnClickListener(this);
        //单位bit
        // Unit is bit
        etSetAds.setText("32");
        //单位bit
        // Unit is bit
        etSetLen.setText("96");
        //单位hex
        // unit is hex
        etSetData.setText("1234567890ABCDEF12345678");

        currentTag = v.findViewById(R.id.currentTag);
        showRssi = v.findViewById(R.id.showRssi);
        singnalStrength = v.findViewById(R.id.singnalStrength);
        searchFilterTag = v.findViewById(R.id.searchFilterTag);
        searchFilterTag.setOnClickListener(this);

        content = getText(R.string.current_Tag).toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        GetRFIDThread.getInstance().setBackResult(this);
        GetRFIDThread.getInstance().setSearchTag(true);
        //MyApp.getMyApp().getUhfMangerImpl().slrSetEPCData(this);
        slrMoudle = MyApp.getMyApp().getUhfMangerImpl().slrInventoryModelGet();
        Log.e(TAG, "startOrStopRFID: " + slrMoudle);
    }

    @Override
    public void onPause() {
        super.onPause();
        GetRFIDThread.getInstance().setSearchTag(false);
        //clearFilter();
        if (slrMoudle !=  MyApp.getMyApp().getUhfMangerImpl().slrInventoryModelGet())
        MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(slrMoudle);
    }

    @Override

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Bt_SetFilter:
                filter();
                break;
            case R.id.Bt_Clear:
                clearFilter();
                break;
            case R.id.searchFilterTag:
                startOrStopRFID();
                break;
            default:
                break;
        }
    }

    private void filter() {
        if (ifNotNull()) {
            int ads = Integer.valueOf(etSetAds.getText().toString());
            int len = Integer.valueOf(etSetLen.getText().toString());
            int val = spSetFilterMb.getSelectedItemPosition();
            int flag = 0;
            if(cBSetFilterSave.isChecked()) flag =1;
            else flag = 0;
            boolean status = MyApp.getMyApp().getUhfMangerImpl().filterSet
                    (MyApp.UHF[val], ads, len, etSetData.getText().toString(), flag);
            if (status) {
                //定频915MHZ,定功率25
                // fixed frequency 915MHZ,fixed power 25
                MyApp.getMyApp().getUhfMangerImpl().sessionModeSet(0);
                MyApp.getMyApp().getUhfMangerImpl().powerSet(25);
//                MyApp.getMyApp().getUhfMangerImpl().frequencyModeSet(4);
                //MyApp.getMyApp().getUhfMangerImpl().frequenceRange_Set(0 ,1,new int[]{915250},0);
                MyApp.getMyApp().getUhfMangerImpl().frequenceRange_Set(0 ,4,new int[]{915250,915750,916250,916750},0);
                MUtil.show(R.string.fiter_success);
            } else {
                MUtil.show(R.string.fiter_failed);
            }
        } else {
            MUtil.show(R.string.data_notnull);
        }

    }

    private void clearFilter() {
        int val = spSetFilterMb.getSelectedItemPosition();
        int flag = 0;
        if(cBSetFilterSave.isChecked()) flag =1;
        else flag = 0;
        boolean status = MyApp.getMyApp().getUhfMangerImpl().filterSet
                (MyApp.UHF[val], 0, 0, etSetData.getText().toString(), flag);
        if (status) {
            MUtil.show(R.string.clean_success);
            //重置功率为30，区域频率为美国
            // Reset power is 30, area frequency is US
            MyApp.getMyApp().getUhfMangerImpl().powerSet(30);
            MyApp.getMyApp().getUhfMangerImpl().frequencyModeSet(3);
        } else {
            MUtil.show(R.string.clean_failed);
        }
    }

    private boolean ifNotNull() {
        return !isEmpty(etSetAds.getText()) && !isEmpty(etSetLen.getText()) && !isEmpty(etSetData.getText());
    }

    int slrMoudle = -1;
    //开启或停止RFID模块
    // Start Or Stop RFID
    public void startOrStopRFID() {
        boolean flag = !GetRFIDThread.getInstance().isIfPostMsg();
        if (flag && MyApp.isLowPower) {
            MUtil.show(getString(R.string.low_power));
            return;
        }
        if (flag) {
            MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(4);
            MyApp.getMyApp().getUhfMangerImpl().startInventoryTag();
            Bt_SetFilter.setEnabled(false);
            Bt_Clear.setEnabled(false);
        } else {
            Bt_SetFilter.setEnabled(true);
            Bt_Clear.setEnabled(true);
            MyApp.getMyApp().getUhfMangerImpl().stopInventory();
        }

        GetRFIDThread.getInstance().setIfPostMsg(flag);
        searchFilterTag.setText(flag ? R.string.stop_rfid : R.string.read_rfid);
    }

    @Override
    public void onKeyDown(int keyCode, KeyEvent event) {
        //把枪按钮被按下,默认值为F8,F4,BTN4
        // When button is pressed, default value is F8,F4,BTN4
        Log.e("idata","onKeyDown");

        if (keyCode == KeyEvent.KEYCODE_F8 || keyCode == KeyEvent.KEYCODE_F4 || keyCode == KeyEvent.KEYCODE_BUTTON_4 || keyCode == KeyEvent.KEYCODE_PROG_RED) {
            startOrStopRFID();
        }
    }

    //RSSI的最大值和最小值
    // Maximum and minimum values of RSSI
    private short maxValue = -29, minValue = -70;

    @Override
    public void postResult(String[] tagData) {
        if (tagData != null) {
            String epc = tagData[1];
            final int[] progressAndRssi = convertRssiToPrgress(tagData[2]);
            Log.e(TAG, "postResult: " + tagData[2]);
            final String epcStr = content + epc;
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    currentTag.setText(epcStr);
                    singnalStrength.setProgress(progressAndRssi[0]);
                    showRssi.setText(getString(R.string.current_rssi) + progressAndRssi[1]);
                    playSound(progressAndRssi[0]);
                    Log.e(TAG,"postResult :  playSound "+progressAndRssi[0]);

                }
            });
        } else {
            if (singnalStrength.getProgress() != 0)
            //无数据时候清空UI显示
            // Clear UI display when no data is available
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    currentTag.setText(null);
                    showRssi.setText(null);
                    singnalStrength.setProgress(0);
                }
            });
        }
    }

    private int[] convertRssiToPrgress(String rssiStr) {
        int[] progressAndRssi = new int[2];
        if (UHFModuleType.UM_MODULE == UHFManager.getType()) {
            // 小数位的值
            // Fractional values
//            int floatBit = Integer.parseInt(rssiStr.substring(rssiStr.length() - 1, rssiStr.length()));
//            // 整数位的值
//            // Integer Values
//            int intgerBit = Integer.parseInt(rssiStr.substring(0, rssiStr.length() - 2));
//            //四舍五入操作
//            // Rounding up
//            if (floatBit > 5) {
//                --intgerBit;
//            } else {
//                ++intgerBit;
//            }
//            int rssi = intgerBit;
//            //取差值
//            // Take the difference
//            // int length = maxValue - minValue;
//            if (rssi >= maxValue) {
//                rssi = maxValue;
//            } else if (rssi <= minValue) {
//                rssi = minValue;
//            }
//            rssi -= (minValue - 11);
///*            final int finalRssi = rssi;
//            final int finalRealRssi = intgerBit;*/
//            progressAndRssi[0] = rssi;
//            progressAndRssi[1] = intgerBit;
            int Hb = Integer.parseInt(rssiStr.substring(0,2), 16);
            int Lb = Integer.parseInt(rssiStr.substring(2, 4), 16);
            int rssi1 = ((Hb - 256 + 1) * 256 + (Lb - 256)) / 10;
            int rssi = rssi1;
            if (rssi >= maxValue) {
                rssi = maxValue;
            } else if (rssi <= minValue) {
                rssi = minValue;
            }
            if (MyApp.ifUM510)
                rssi -= (minValue - 11);
            else
                rssi -= (minValue - 1);
            progressAndRssi[0] = rssi;
            progressAndRssi[1] = rssi1;
            Log.e("tag","rssi = " + rssi);
        } else if (UHFModuleType.SLR_MODULE == UHFManager.getType()) {
            int intgerBit = Integer.parseInt(rssiStr); // 整数位的值
            int rssi = Math.abs(intgerBit + 70);
            progressAndRssi[0] = rssi;
            progressAndRssi[1] = intgerBit;
        }
        return progressAndRssi;
    }


    @Override
    public void postInventoryRate(long rate) {

    }

    private long currentMinute, oldMinute;

    //音源播放
    // Play Sound
    private void playSound(int val) {
        if (MyApp.ifOpenSound) {
            if (val > 30) {
                MyApp.getMyApp().playSound();
                oldMinute = System.currentTimeMillis();
            } else if (val > 20) {
                currentMinute = System.currentTimeMillis();
                if (currentMinute - oldMinute > 300) {
                    MyApp.getMyApp().playSound();
                    oldMinute = currentMinute;
                }
            } else if (val > 10) {
                currentMinute = System.currentTimeMillis();
                if (currentMinute - oldMinute > 600) {
                    MyApp.getMyApp().playSound();
                    oldMinute = currentMinute;
                }
            } else if (val > 0) {
                currentMinute = System.currentTimeMillis();
                if (currentMinute - oldMinute > 900) {
                    MyApp.getMyApp().playSound();
                    oldMinute = currentMinute;
                }
            }
        }
    }

    @Override
    public void chargeChange(boolean isLow) {
        if (isLow) {
            if (GetRFIDThread.getInstance().isIfPostMsg()) {
                startOrStopRFID();
                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MUtil.show(getString(R.string.low_power));
                    }
                });
            }
        }
    }


//    @Override
//    public void getEpcData(String[] tagData) {
//        if (tagData != null) {
//            String epc = tagData[1];
//            final int[] progressAndRssi = convertRssiToPrgress(tagData[2]);
//            final String epcStr = content + epc;
//            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
//                @SuppressLint("SetTextI18n")
//                @Override
//                public void run() {
//                    currentTag.setText(epcStr);
//                    singnalStrength.setProgress(progressAndRssi[0]);
//                    showRssi.setText(getString(R.string.current_rssi) + progressAndRssi[1]);
//                    playSound(progressAndRssi[0]);
//                    Log.e(TAG,"postResult2 :  playSound "+progressAndRssi[0]);
//
//                }
//            });
//        } else {
//            //无数据时候清空UI显示
//            // Clear UI display when no data is available
//            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    currentTag.setText(null);
//                    showRssi.setText(null);
//                    singnalStrength.setProgress(0);
//                }
//            });
//        }
//    }
}
