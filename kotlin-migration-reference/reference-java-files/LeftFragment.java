package com.uhf.uhfdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.uhf.event.BackResult;
import com.uhf.event.BaseFragment;
import com.uhf.event.GetRFIDThread;
import com.uhf.event.OnKeyListener;
import com.uhf.event.OnLowPower;
import com.uhf.util.DataAdapter;
import com.uhf.util.DataConversionUtils;
import com.uhf.util.ExcelUtil;
import com.uhf.util.MLog;
import com.uhf.util.MUtil;
import com.uhf.util.ThreadUtil;
import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.uhf.uhfdemo.MyApp.ifASCII;
import static com.uhf.uhfdemo.MyApp.ifRMModule;
import static com.uhf.uhfdemo.MyApp.ifSupportR2000Fun;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;



public class LeftFragment extends BaseFragment implements View.OnClickListener, BackResult, AdapterView.OnItemSelectedListener,OnKeyListener, OnLowPower {

    private LinearLayout invDataSet;
    private TextView read_RFID;
    private TextView tagNumbers, readNumbers, useTimes, epcShow, tidShow, usrShow, rfuShow, tempShow,export;
    private Spinner invtDataTypeSet;
    private View epc_tid_show_divd_Line, epc_usr_show_divd_Line, epc_rfu_show_divd_Line;
    private String tagNumber, readNumber, takeTime;

    private long startTime, usTim, pauseTime;
    private DataAdapter mListAdapter;

    private Button btn_power;

    private EditText et_power;


    private Handler handler = new Handler();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tagNumber = getString(R.string.tag_number) + ":";
        readNumber = getString(R.string.read_number) + ":";
        takeTime = getString(R.string.user_time) + ":";
        return inflater.inflate(R.layout.fragment_left, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    private void initView(final View v) {
        btn_power = v.findViewById(R.id.btn_power);
        et_power = v.findViewById(R.id.et_power);

        btn_power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyApp.getMyApp().getUhfMangerImpl().powerTransmitted(Integer.parseInt(et_power.getText().toString()));
            }
        });

        read_RFID = v.findViewById(R.id.read_RFID);
        //清空数据
        // Clear data
        v.findViewById(R.id.clear_Data).setOnClickListener(this);
        ListView specific_Msg = v.findViewById(R.id.specific_Msg);

        tagNumbers = v.findViewById(R.id.tagNumbers);
        readNumbers = v.findViewById(R.id.readNumbers);
        useTimes = v.findViewById(R.id.useTimes);

        export = v.findViewById(R.id.export);
        export.setOnClickListener(this);

        epcShow = v.findViewById(R.id.epcShow);
        tidShow = v.findViewById(R.id.tidShow);
        usrShow = v.findViewById(R.id.usrShow);
        rfuShow = v.findViewById(R.id.rfuShow);
        tempShow = v.findViewById(R.id.temp);
        epc_tid_show_divd_Line = v.findViewById(R.id.epc_tid_show_divd_Line);
        epc_usr_show_divd_Line = v.findViewById(R.id.epc_usr_show_divd_Line);
        epc_rfu_show_divd_Line = v.findViewById(R.id.epc_rfu_show_divd_Line);
        invDataSet = v.findViewById(R.id.invDataSet);
        invtDataTypeSet = v.findViewById(R.id.invModSet);
        refreshUI();
        invtDataTypeSet.setOnItemSelectedListener(this);
        if (MyApp.currentInvtDataType > -1) {
            SystemClock.sleep(1000);
            refreshTagDataTypeShow(MyApp.currentInvtDataType);
            invtDataTypeSet.setSelection(MyApp.currentInvtDataType);
        }

        read_RFID.setOnClickListener(this);
        read_RFID.setText(GetRFIDThread.getInstance().isIfPostMsg() ? R.string.stop_rfid : R.string.read_rfid);

        mListAdapter = new DataAdapter(getActivity(), realDataMap, realKeyList, tidList, usrList, rfuList, rssiList, gtinList);
        specific_Msg.setAdapter(mListAdapter);
        GetRFIDThread.getInstance().setBackResult(this);
        if (MyApp.protocolType == 1) {
            epcShow.setText(getString(R.string.epc_code));
        }else if (MyApp.protocolType == 2) {
            epcShow.setText(getString(R.string.code_area));
        }else if (MyApp.protocolType == 3) {
            epcShow.setText(getString(R.string.tid_code));
            tidShow.setText(getString(R.string.usr_code));
        }

    }

    @Override
    public void refreshUI() {
        super.refreshUI();
        //是否为UM7的设备,或者是否为5100模块
        // Determining if it is a UM7 module , Or is it a 5100 module
        if (MyApp.getMyApp().getUhfMangerImpl().ifJ06() || !ifSupportR2000Fun || MyApp.if5100Module) {
            invDataSet.setVisibility(View.GONE);
        }else if (ifRMModule) {
            invDataSet.setVisibility(View.VISIBLE);
            invtDataTypeSet.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                    android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.readTagType_7100)));
            if (MyApp.protocolType == 2) {
                invDataSet.setVisibility(View.GONE);
                MyApp.currentInvtDataType = 0;
            }
        }else if (UHFModuleType.SLR_MODULE == UHFManager.getType()) {
            invtDataTypeSet.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                    android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.readTagType_7100)));
        }else if (UHFModuleType.GX_MODULE == UHFManager.getType()) {
            invDataSet.setVisibility(View.VISIBLE);
            if (MyApp.protocolType == 3) {
                invtDataTypeSet.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                        android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.readTagType_gx_6b)));
            }
        }else {
            invDataSet.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.read_RFID:
               startOrStopRFID();
                break;
            //清除显示数据
            // Clear data
            case R.id.clear_Data:
                if (GetRFIDThread.getInstance().isIfPostMsg()) {
                    MUtil.show(R.string.notice_clean_data);
                } else {
                    clearData();
                }

                break;
            case R.id.export:
                if (GetRFIDThread.getInstance().isIfPostMsg()) {
                    MUtil.show(R.string.notice_export_data);
                } else {
                    exportData();
                }

                break;
            default:
                break;
        }
    }

    private void clearData() {
        dataMap.clear();
        realDataMap.clear();
        realKeyList.clear();
        tidList.clear();
        usrList.clear();
        rfuList.clear();
        rssiList.clear();
        usTim = pauseTime = 0;
        useTimes.setText(takeTime);
        tagNumbers.setText(tagNumber);
        readNumbers.setText(readNumber);
        mListAdapter.notifyDataSetChanged();
    }

    //开启或停止RFID模块
    // Start or Stop RFID
    Runnable task;
    Runnable stop_time_task;
    boolean ifOpenFan = false;
    public void startOrStopRFID() {
        boolean flag = !GetRFIDThread.getInstance().isIfPostMsg();
        if (flag && MyApp.isLowPower) {
            MUtil.show(getString(R.string.low_power));
            return;
        }
        invtDataTypeSet.setEnabled(!flag);
        if (flag) {
            if (UHFModuleType.SLR_MODULE == UHFManager.getType() && MyApp.if5100Module){
                MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(0);
            }
            MyApp.getMyApp().getUhfMangerImpl().startInventoryTag();

            long tempTime = pauseTime;
            startTime = System.currentTimeMillis() - tempTime;
            ifSoundThreadAlive = true;
            playSound();
            if (ifRMModule && UHFModuleType.RM_MODULE == UHFManager.getType()) {
                handler.postDelayed(task = new Runnable() {
                    @Override
                    public void run() {
                        if (Temp > 55) {
                            if (!ifOpenFan) {
                                MyApp.getMyApp().getUhfMangerImpl().openFan();
                                ifOpenFan = true;
                            }
                        }else if (Temp < 55) {
                            if (ifOpenFan) {
                                MyApp.getMyApp().getUhfMangerImpl().closeFan();
                                ifOpenFan = false;
                            }
                        }
                        handler.postDelayed(this,5000);
                    }
                },5000);
            }
            if (MyApp.ifAutoStopLabel) {
                if (MyApp.stopLabelTime != -1) {
                    handler.postDelayed(stop_time_task = new Runnable() {
                        @Override
                        public void run() {
                            read_RFID.performClick();
                        }
                    },MyApp.stopLabelTime);
                }
            }
            reducingPowerDissipation(true);
            //setVolumeTimer();
        } else {
            ifHaveTag = false;
            ifSoundThreadAlive = false;
            Boolean i = MyApp.getMyApp().getUhfMangerImpl().stopInventory();
            if (ifRMModule && UHFModuleType.RM_MODULE == UHFManager.getType()) {
                if (task !=null) {
                    if (ifOpenFan) {
                        MyApp.getMyApp().getUhfMangerImpl().closeFan();
                        ifOpenFan = false;
                    }
                    handler.removeCallbacks(task);
                }
            }
            if (stop_time_task != null) {
                handler.removeCallbacks(stop_time_task);
            }
            reducingPowerDissipation(false);
            //cancelVolumeTimer();
        }
        GetRFIDThread.getInstance().setIfPostMsg(flag);
        read_RFID.setText(flag ? R.string.stop_rfid : R.string.read_rfid);

    }

    //识别的标签数据
    //The label's info what has been identified
    private Map<String, Integer> dataMap = new HashMap<>();

    private long postDataTime = 0;

    int Temp = 0;
    @Override
    public void postResult(String[] tagData) {
        postDataTime = SystemClock.elapsedRealtime();
        ifHaveTag = true;
        //获取TID
        // get TID
        String tid = tagData[0];
        String usr = tagData[0];
        String rfu = tagData[0];
        String rssi = tagData[2];
        if (ifRMModule) {
            if (Temp != Integer.parseInt(tagData[3]))
                Temp = Integer.parseInt(tagData[3]);
            Log.e("TAG", "postResult: " +Temp);
        }
        //拿到EPC
        // get EPC
        if(MyApp.currentInvtDataType == 3){
            usr = tagData[0];
        } else if(MyApp.currentInvtDataType == 4) {
            tid = tagData[0].substring(0,24);
            usr = tagData[0].substring(24);
        }else if (MyApp.currentInvtDataType == 5) {
            tid = tagData[0].substring(0,24);
            rfu = tagData[0].substring(24,tagData[0].length()-2);
        } else if (MyApp.currentInvtDataType == 6){
            rfu =tagData[0];
        }
        String epc = tagData[1];
        if (ifASCII) {
            tid = convertHexToString(tid);
            epc = convertHexToString(epc);
            usr = convertHexToString(usr);
            rfu = convertHexToString(rfu);
        }

        //对UM模组传上来的RSSI进行操作
        if (UHFManager.getType() == UHFModuleType.UM_MODULE || UHFManager.getType() == UHFModuleType.RM_MODULE) {
            int Hb = Integer.parseInt(rssi.substring(0, 2), 16);
            int Lb = Integer.parseInt(rssi.substring(2, 4), 16);
            int rssi1 = ((Hb - 256 + 1) * 256 + (Lb - 256)) / 10;
            rssi = String.valueOf(rssi1);
        }
        String filterData = MyApp.currentInvtDataType == 1 || MyApp.currentInvtDataType == 2 || MyApp.currentInvtDataType == 4 || MyApp.currentInvtDataType == 5 ? tid : epc;
//        if (!TextUtils.isEmpty(tid) && !customizationList.contains(tid) && (MyApp.currentInvtDataType == 1 || MyApp.currentInvtDataType == 2 || MyApp.currentInvtDataType == 4 || MyApp.currentInvtDataType == 5)) {
//            if (Integer.parseInt(rssi) < rssiLimit)
//                return;
//        }
        //如果已存在，就拿到数量 ,note：因为EPC不唯一，又有客户把EPC弄成一样的，所以不能只以EPC区分
        // If it already exists, get the number
        Integer number = dataMap.get(/*epc*/filterData);
        if (number == null) {
            dataMap.put(/*epc*/filterData, 1);
            updateUI(epc, tid, usr, rfu, rssi,1);
        } else {
            int newNB = number + 1;
            dataMap.put(/*epc*/filterData, newNB);
            updateUI(epc, tid, usr, rfu, rssi,newNB);
        }
    }

    @Override
    public void postInventoryRate(final long rate) {
        if (getActivity() == null)
            return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 张数/秒
                // tags per second
                readNumbers.setText(rate + readNumber);
            }
        });
    }

    boolean isDown  = true;
    long keyTime;
    @Override
    public void onKeyDown(int keyCode, KeyEvent event) {
        MLog.e("KeyDown keyCode =" + keyCode);
        //把枪按钮被按下,默认值为F8,F4,BTN4
        //The trigger button is pressed, and the default value is F8,F4,BTN4
        if (keyCode == KeyEvent.KEYCODE_F8 || keyCode == KeyEvent.KEYCODE_F4 || keyCode == KeyEvent.KEYCODE_BUTTON_4 || keyCode == KeyEvent.KEYCODE_PROG_RED || keyCode == KeyEvent.KEYCODE_BUTTON_3/*|| keyCode == KeyEvent.KEYCODE_BUTTON_1|| keyCode ==KeyEvent.KEYCODE_F9 || keyCode == KeyEvent.KEYCODE_F10*/) {
            startOrStopRFID();
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_2) {
            if (isDown) {
                if (MyApp.getMyApp().getiScanInterface() == null)
                    return;
                MyApp.getMyApp().getiScanInterface().scan_start();
                isDown = false;
            }
        }
    }
    @Override
    public void onKeyUp(int keyCode, KeyEvent event) {
        MLog.e("KeyUp keyCode =" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BUTTON_2) {
            if (MyApp.getMyApp().getiScanInterface() == null)
                return;
            MyApp.getMyApp().getiScanInterface().scan_stop();
            isDown = true;
        }
    }

    private Map<String, Integer> realDataMap = new HashMap<>();
    // realDataMap的key
    // realDataMap's key
    private List<String> realKeyList = new ArrayList<>();
    //tid info
    private List<String> tidList = new ArrayList<>();
    //usr info
    private List<String> usrList = new ArrayList<>();
    //rfu info
    private List<String> rfuList = new ArrayList<>();
    //rssi info
    private List<String> rssiList = new ArrayList<>();

    //custom customization
    private List<String> gtinList = new ArrayList<>();

    private void updateUI(final String epc, final String tid, final String usr, final String rfu, final String rssi, final int readNumberss) {
        if (!isAdded())
            return;
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String filterData = MyApp.currentInvtDataType == 1 || MyApp.currentInvtDataType == 2 || MyApp.currentInvtDataType == 4 || MyApp.currentInvtDataType == 5 ? tid : epc;
                if (readNumberss > 1) {
                    //修改数量
                    //Modified quantity
                    realDataMap.put(/*epc*/filterData, readNumberss);
                    String rssibuf = rssiList.get(realKeyList.indexOf(filterData));
                    rssiList.set(realKeyList.indexOf(filterData),rssi);
                } else {
                    realDataMap.put(/*epc*/filterData, 1);
                    realKeyList.add(/*epc*/filterData);
                    tidList.add(MyApp.currentInvtDataType <= 0 ? tid : epc);
                    usrList.add(usr);
                    rfuList.add(rfu);
                    rssiList.add(rssi);
                    if (MyApp.isShangHaiTaoPinCustom)
                        gtinList.add(DataConversionUtils.Decode(epc));
                    else
                        gtinList.add(null);
                    MLog.e("tid = " + tid + " epc = " + epc +" usr = " + usr + " rfu = " + rfu);

                }
                long endTime = System.currentTimeMillis();
                //盘点标签开始到结束的获取时间
                //Acquisition time from start to end of inventorying tag
                pauseTime = usTim = endTime - startTime;
                //花费的时间
                //Time spent
                useTimes.setText(takeTime + usTim);
                //标签数量
                //Number of tags
                tagNumbers.setText(tagNumber + realDataMap.size());
                mListAdapter.notifyDataSetChanged();
                long realUseTime = System.currentTimeMillis() - lastTime;
                /*  if (MyApp.ifOpenSound && realUseTime > 60) {
                    MyApp.getMyApp().playSound();
                    lastTime = endTime;
                }*/
                //tempShow.setText(Temp + "℃");
            }
        });
    }

    private Timer mVolumeTimer;
    private Timer mVolumeTimer2;

    private void setVolumeTimer() {
        int time = 300000;
        mVolumeTimer = new Timer();
        TimerTask mTimerTask = new TimerTask() {
            @Override
            public void run() {
                MyApp.getMyApp().setVolume(0.2f);
                mVolumeTimer.cancel();
            }
        };
        mVolumeTimer.schedule(mTimerTask, time);
        mVolumeTimer2 = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                MyApp.getMyApp().setVolume(0.1f);
                mVolumeTimer2.cancel();
            }
        };
        mVolumeTimer2.schedule(timerTask,time*2);
    }

    private void cancelVolumeTimer() {
        if (mVolumeTimer != null) {
            mVolumeTimer.cancel();
            mVolumeTimer = null;
        }
        if (mVolumeTimer2 != null) {
            mVolumeTimer2.cancel();
            mVolumeTimer2 = null;
        }
        MyApp.getMyApp().setVolume(1);
    }

    private boolean ifSoundThreadAlive = true;
    private boolean ifHaveTag = false;
    private long lastTime;

    private void playSound() {
        ThreadUtil.getInstance().getExService().execute(new Runnable() {
            @Override
            public void run() {
                while (MyApp.ifOpenSound && ifSoundThreadAlive) {
                    if (ifHaveTag) {
                        lastTime = SystemClock.elapsedRealtime();
                        //超过1s无数据暂停播放声音
                        //Pause playing sound without data for more than 1 s
                        if (lastTime != 0 && lastTime - postDataTime > 600) {
                            ifHaveTag = false;
                            continue;
                        }
                        MyApp.getMyApp().playSound();
                        SystemClock.sleep(600);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ifHaveTag = false;
        ifSoundThreadAlive = false;
    }

    private void refreshTagDataTypeShow(int position) {
        clearData();
        epcShow.setVisibility(position == 1 ? View.GONE : View.VISIBLE);
        tidShow.setVisibility(position == 0 || position == 3 || position == 6 ? View.GONE : View.VISIBLE);
        usrShow.setVisibility(position == 3 || position == 4 ? View.VISIBLE : View.GONE);
        rfuShow.setVisibility(position == 5 || position == 6 ? View.VISIBLE : View.GONE);
        epc_tid_show_divd_Line.setVisibility(position == 0 || position == 3 || position == 6 ? View.GONE : View.VISIBLE);
        epc_usr_show_divd_Line.setVisibility(position == 3 || position == 4 ? View.VISIBLE : View.GONE);
        epc_rfu_show_divd_Line.setVisibility(position == 5 || position == 6 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (MyApp.ifFirst) {
            MyApp.ifFirst = false;
            return;
        }
        MyApp.currentInvtDataType = position;
        mListAdapter.setMode(position);
        refreshTagDataTypeShow(position);
        //OnlyTID+EPC,和EPC模式
        //OnlyTID+EPC,and EPC MODE
        if (UHFModuleType.GX_MODULE == UHFManager.getType() && MyApp.protocolType == 3) {
            switch (position) {
                case 0:
                    position = 0;
                    break;
                case 1:
                    position = 2;
                    break;
                case 2:
                    position = 1;
                    break;
            }
            MyApp.getMyApp().getUhfMangerImpl().readTagModeSet(position, 0, 0, 0);
        } else {
            switch (position) {
                case 0:
                    position = 0;
                    break;
                case 1:
                    position = 1;
                    break;
                case 2:
                    position = 1;
                    break;
                case 3:
                    position = 2;
                    break;
                case 4:
                    position = 3;
                    break;
                case 5:
                    position = 4;
                    break;
                case 6:
                    position = 5;
                    break;
            }
            //position = Math.min(position, 1);
            if (position == 2 || position == 3) {
                boolean i = MyApp.getMyApp().getUhfMangerImpl().readTagModeSet(position, 0, 24, 0);
                MLog.e("position= " + position + " readTagModeSet = " + i);
            } else if (position == 1) {
                MyApp.getMyApp().getUhfMangerImpl().readTagModeSet(position, 0, 6, 0);
            } else if (position == 5) {
                MyApp.getMyApp().getUhfMangerImpl().readTagModeSet(position, 4, 1, 0);
            } else {
                boolean i = MyApp.getMyApp().getUhfMangerImpl().readTagModeSet(position, 0, 0, 0);
                MLog.e("position= " + position + " readTagModeSet = " + i);
            }

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //将16进制转为ASCII码字符
    public String convertHexToString(String hex){

        StringBuilder sb = new StringBuilder();
        if (hex == null) {
            return null;
        }
        //49204c6f7665204a617661 split into two characters 49, 20, 4c... //保证两位进行操作
        for( int i=0; i<hex.length()-1; i+=2 ){
            //grab the hex in pairs //将数据两位分组
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal //将十六进制转换成十进制
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character //将十进制转换成ascii字符
            sb.append((char)decimal);
        }

        return sb.toString();
    }

    //导出数据到Excel表中
    private void exportData() {
        List<List<String>> object = new ArrayList<>();
        String[] title = null;
        Date dt = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
        String time = sdf.format(dt);
        String fileName  = "UHFDemo" + "_" + time +".xls";
        switch (MyApp.currentInvtDataType) {
            case 0:
                if (realKeyList.size() == 0)
                    return;
                object.add(realKeyList);
                title = new String[]{getString(R.string.epc_code)};
                ExcelUtil.initExcel(fileName,getString(R.string.export),title);
                ExcelUtil.writeObjListToExcel(object,fileName, getContext());
                break;
            case 1:
                if (realKeyList.size() == 0)
                    return;
                title = new String[]{getString(R.string.tid_code)};
                object.add(realKeyList);
                ExcelUtil.initExcel(fileName,getString(R.string.export),title);
                ExcelUtil.writeObjListToExcel(object,fileName, getContext());
                break;
            case 2:
                if (realKeyList.size() == 0 || tidList.size() == 0)
                    return;
                title = new String[]{getString(R.string.epc_code),getString(R.string.tid_code)};
                object.add(tidList);
                object.add(realKeyList);
                ExcelUtil.initExcel(fileName,getString(R.string.export),title);
                ExcelUtil.writeObjListToExcel(object,fileName, getContext());
                break;
            case 3:
                if (realKeyList.size() == 0 || usrList.size() == 0)
                    return;
                title = new String[]{getString(R.string.epc_code),getString(R.string.usr_code)};
                object.add(realKeyList);
                object.add(usrList);
                ExcelUtil.initExcel(fileName,getString(R.string.export),title);
                ExcelUtil.writeObjListToExcel(object,fileName, getContext());
                break;
            case 4:
                if (realKeyList.size() == 0 || usrList.size() == 0 || tidList.size()==0)
                    return;
                title = new String[]{getString(R.string.epc_code),getString(R.string.tid_code),getString(R.string.usr_code)};
                object.add(tidList);
                object.add(realKeyList);
                object.add(usrList);
                ExcelUtil.initExcel(fileName,getString(R.string.export),title);
                ExcelUtil.writeObjListToExcel(object,fileName, getContext());
                break;
            case 5:
                if (realKeyList.size() == 0 || rfuList.size() == 0 || tidList.size()==0)
                    return;
                title = new String[]{getString(R.string.epc_code),getString(R.string.tid_code),getString(R.string.rfu_code)};
                object.add(tidList);
                object.add(realKeyList);
                object.add(rfuList);
                ExcelUtil.initExcel(fileName,getString(R.string.export),title);
                ExcelUtil.writeObjListToExcel(object,fileName, getContext());
                break;
            case 6:
                if (realKeyList.size() == 0 || rfuList.size() == 0 )
                    return;
                title = new String[]{getString(R.string.epc_code),getString(R.string.rfu_code)};
                object.add(realKeyList);
                object.add(rfuList);
                ExcelUtil.initExcel(fileName,getString(R.string.export),title);
                ExcelUtil.writeObjListToExcel(object,fileName, getContext());
                break;
        }
    }

    PowerManager.WakeLock wakeLock;
    /**
     * 请求唤醒锁
     */
    public void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) Objects.requireNonNull(getActivity()).getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this.getClass().getCanonicalName());
            wakeLock.acquire();
        }
    }

    /**
     * 释放锁
     */
    public void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void reducingPowerDissipation(boolean ifStart) {
        Intent PowerDissipation = new Intent("android.intent.action.CONTINUCEUHF");
        PowerDissipation.putExtra("ifStart",ifStart);
        Objects.requireNonNull(getContext()).sendBroadcast(PowerDissipation);
//        if (ifStart)
//            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        else
//            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
}
