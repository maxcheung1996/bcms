package com.uhf.uhfdemo;

import static android.os.BatteryManager.EXTRA_STATUS;
import static android.os.BatteryManager.EXTRA_VOLTAGE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
//import android.support.annotation.NonNull;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentManager;
//import android.support.v4.app.FragmentTransaction;
//import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.tencent.mmkv.MMKV;
import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;
import com.uhf.event.BaseFragment;
import com.uhf.event.GetRFIDThread;
import com.uhf.event.OnKeyDownListener;
import com.uhf.event.OnKeyListener;
import com.uhf.event.OnLowPower;
import com.uhf.util.MLog;
import com.uhf.util.MUtil;
import com.uhf.util.ThreadUtil;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import realid.rfidlib.EmshConstant;
import realid.rfidlib.MyLib;

import static realid.rfidlib.EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_BATTERY_ERROR;
import static realid.rfidlib.EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_CHG_FULL;
import static realid.rfidlib.EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_CHG_GENERAL;
import static realid.rfidlib.EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_CHG_QUICK;
import static realid.rfidlib.EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_DSG_UHF;
import static realid.rfidlib.EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_STANDBY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private LeftFragment mLeftFragment;
    private RightFragment mRightFragment;
    private SearchFragment mSearchFragment;
    private FragmentManager manager;
    private ImageView toLeft, toRight, searchTag, inventoryTag;
    private InventoryFragment mInventoryFragment;

    private Object currentFragment;


    private boolean ifCharge = false; //whether the pistol trigger is charging
    //RFID标签信息获取线程
    // RFID tag information acquisition thread
    private GetRFIDThread rfidThread = GetRFIDThread.getInstance();
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private boolean ifRequesetPermission = true;
    private boolean ifPowerOn;
    private int setPower = 33;
    private MMKV mkv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT > 22)
            requestPermission();
        else
            init();
    }

    private void init() {
        toLeft = findViewById(R.id.toLeft);
        toRight = findViewById(R.id.toRight);
        searchTag = findViewById(R.id.searchTag);
        inventoryTag = findViewById(R.id.inventoryTag);
        manager = getSupportFragmentManager();
        //setCurrentPage(0);
        MUtil.showProgressDialog(getString(R.string.init_msg), this);
        rfidThread.start();
        ThreadUtil.getInstance().getExService().execute(new Runnable() {
            @Override
            public void run() {
                // 初始化开启把枪和串口配置(50把枪设备)
                // Initialisation of the device and serial port configuration （only 50 equipment）
                if (MyApp.getMyApp().getUhfMangerImpl().getDeviceInfo().isIfHaveTrigger()) {
                    ifPowerOn = MyApp.getMyApp().getUhfMangerImpl().powerOn();
                    MLog.e("powerOn = " + ifPowerOn);
                    MyApp.getMyApp().getUhfMangerImpl().changeConfig(true);
                    monitorEmsh();
                } else {
                    if (MyApp.isChangeBaud) {
                        if ("921600".equals(MMKV.defaultMMKV().decodeString("baud")) && UHFManager.getType() == UHFModuleType.SLR_MODULE)
                            MyApp.getMyApp().getUhfMangerImpl().isHighUHFBaud(true);
                        else if ("115200".equals(MMKV.defaultMMKV().decodeString("baud")) && UHFManager.getType() == UHFModuleType.SLR_MODULE)
                            MyApp.getMyApp().getUhfMangerImpl().isHighUHFBaud(false);
                    }
                    MLog.e("powerOn = " + MyApp.getMyApp().getUhfMangerImpl().powerOn());
                }
                getModuleInfo();
                ua01DeviceToConfigure();
            }
        });
        setCurrentPage(0);
        registerPowerCapacity();
        acquireWakeLock();

    }
    private void requestPermissionR() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                ifRequesetPermission = false;
                init();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1024);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1024 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                ifRequesetPermission = false;
                init();
            } else {
                Log.d("权限判断--------》","获取权限失败");
                Toast.makeText(this,"获取设备权限失败，请重新运行app并授权，否则将无法正常运行！",Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 1025) {
            if (Settings.canDrawOverlays(this)) {
                ifRequesetPermission = false;
                init();
            }
        }
    }

    //配置UA01设备，电源切换时，如果在盘点，则自动做停止盘点再盘点的动作
    private void ua01DeviceToConfigure() {
        int val = Settings.System.getInt(getContentResolver(), "idata_uhf_service_state", -1);
        //如果为UA01的设备,监听断电后上电
        MLog.e("val = " + val);
        if (val == 0) {
            registerPowerStatus();
        }
    }

    private PowerStatusCheck powerStatusCheck;

    private void registerPowerStatus() {
        if (powerStatusCheck == null) {
            powerStatusCheck = new PowerStatusCheck(this);
            IntentFilter mIntent = new IntentFilter("com.idata.uhf.power.supply");
            registerReceiver(powerStatusCheck, mIntent);
        }
    }

    private void unRegisterPowerStatus() {
        if (powerStatusCheck != null) {
            unregisterReceiver(powerStatusCheck);
        }
    }

    //UA01的供电状态
    private int currentPowerStatus = 0;

    static class PowerStatusCheck extends BroadcastReceiver {
        private WeakReference<MainActivity> mWeak;

        public PowerStatusCheck(MainActivity mainActivity) {
            this.mWeak = new WeakReference<>(mainActivity);
        }


        @Override
        public void onReceive(Context context, final Intent intent) {
            ThreadUtil.getInstance().getExService().execute(new Runnable() {
                @Override
                public void run() {
                    int uhfPowerSupply = intent.getIntExtra("uhfPowerSupply", -1);
                    MLog.e("uhfPowerSupply = " + uhfPowerSupply + " currentPowerStatus = " + mWeak.get().currentPowerStatus);
                    if (uhfPowerSupply >= -1 && uhfPowerSupply != mWeak.get().currentPowerStatus) {
                        if (GetRFIDThread.getInstance().isIfPostMsg()) {
                            GetRFIDThread.getInstance().setIfPostMsg(false);
                            MyApp.getMyApp().getUhfMangerImpl().stopInventory();
                            MyApp.getMyApp().getUhfMangerImpl().powerOff();
                            SystemClock.sleep(100);
                            MyApp.getMyApp().getUhfMangerImpl().powerOn();
                            MyApp.getMyApp().getUhfMangerImpl().getRFIDProtocolStandard();
                            MyApp.getMyApp().getUhfMangerImpl().startInventoryTag();
                            MyApp.getMyApp().getUhfMangerImpl().readTagModeSet(MyApp.currentInvtDataType,0,0,0);
                            GetRFIDThread.getInstance().setIfPostMsg(true);
                        }
                    }
                    mWeak.get().currentPowerStatus = uhfPowerSupply;
                    MLog.e("uhfPowerSupply = " + uhfPowerSupply);
                }
            });

        }
    }

    public PowerCapacityBroadcastReceiver powerCapacity;

    private void registerPowerCapacity() {
        IntentFilter filter = new IntentFilter();
        powerCapacity = new PowerCapacityBroadcastReceiver();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(powerCapacity,filter);
    }

    private void unRegisterPowerCapacity() {
        if (powerCapacity != null) {
            unregisterReceiver(powerCapacity);
            powerCapacity = null;
        }
    }

    private OnLowPower mOnLowPower;

    private int  electricQuantity = 100;

    public class PowerCapacityBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int currentElectricQuantity = intent.getIntExtra("level", 0);    //电池剩余电量
            int batteryVolt = intent.getIntExtra(EXTRA_VOLTAGE, -1);
            int status = intent.getIntExtra(EXTRA_STATUS,0); // 电池状态
            Log.d("TAG", "currentElectricQuantity " + currentElectricQuantity + " status " + status );
            if (currentElectricQuantity < 5) {
                if (!MyApp.isLowPower) {
                    MyApp.isLowPower = true;
                    if (currentFragment!=null && currentFragment instanceof OnLowPower)
                        mOnLowPower = (OnLowPower)currentFragment;
                    if (mOnLowPower!=null)
                        mOnLowPower.chargeChange(true);
                }
            }else {
                if (MyApp.isLowPower) {
                    MyApp.isLowPower = false;
                }
            }

//            if (currentElectricQuantity < 25) {
//                if (electricQuantity < 25) {
//
//                }else {
//                    if (GetRFIDThread.getInstance().isIfPostMsg()) {
//                        MyApp.getMyApp().getUhfMangerImpl().stopInventory();
//                        SystemClock.sleep(50);
//                        MyApp.getMyApp().getUhfMangerImpl().powerSet(25);
//                        MyApp.getMyApp().getUhfMangerImpl().startInventoryTag();
//                    } else {
//                        MyApp.getMyApp().getUhfMangerImpl().powerSet(25);
//                    }
//                }
//            } else if (currentElectricQuantity < 40) {
//                if (electricQuantity < 40) {
//
//                }else {
//                    if (GetRFIDThread.getInstance().isIfPostMsg()) {
//                        MyApp.getMyApp().getUhfMangerImpl().stopInventory();
//                        SystemClock.sleep(50);
//                        MyApp.getMyApp().getUhfMangerImpl().powerSet(30);
//                        MyApp.getMyApp().getUhfMangerImpl().startInventoryTag();
//                    } else {
//                        MyApp.getMyApp().getUhfMangerImpl().powerSet(30);
//                    }
//                }
//            }
            electricQuantity = currentElectricQuantity;
        }
    }



    private void getModuleInfo() {
        //给时间(根据机型配置性能判断，一般2.5S足以)让串口和模块初始化
        //Give time (according to the configuration performance of the model, 2.5S is generally sufficient) for the serial port and module to initialize
        SystemClock.sleep(2500);
        if (UHFModuleType.UM_MODULE == UHFManager.getType()) {
            //初始化判断UM系列的UHF模块类型
            //Initialize and judge the UHF module type of UM series
            String ver = MyApp.getMyApp().getUhfMangerImpl().hardwareVerGet();
            if (!TextUtils.isEmpty(ver)) {
                //判断是否支持UM7模块功能
                //Determine whether the UM 7 module function is supported
                char moduleType = ver.charAt(0);
                MyApp.ifSupportR2000Fun = moduleType == '7' || moduleType == '4' || moduleType == '5' || moduleType == '1';
                MLog.e("ifMode = " + MyApp.ifSupportR2000Fun + " ver =" + ver);
                MyApp.ifUM510 = moduleType == '5';
            }
            judgeModuleTypeAndRefreshUI(MyApp.ifSupportR2000Fun,false);
        } else if (UHFModuleType.SLR_MODULE == UHFManager.getType()) {
            String type = MyApp.getMyApp().getUhfMangerImpl().getUHFModuleType();
            MLog.e("type = " + type);
            if (!TextUtils.isEmpty(type)) {
                if (type.contains("SLR5100") ) {
                    MyApp.if5100Module = true;
                    judgeModuleTypeAndRefreshUI(false,false);
                }
                if (type.contains("7100")||type.contains("3100")||type.contains("SIM5100") || type.contains("3700") ||type.contains("3600")) {
                    MyApp.if7100Module = true;
                    judgeModuleTypeAndRefreshUI(true,false);
                    if (MyApp.saveSet) {
                        MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(MMKV.defaultMMKV().decodeInt("inventoryMode", 3));
                        MyApp.getMyApp().getUhfMangerImpl().powerSet(MMKV.defaultMMKV().decodeInt("power", 33));
                        MyApp.getMyApp().getUhfMangerImpl().frequencyModeSet(MMKV.defaultMMKV().decodeInt("frequencyModeSet", 3));
                    }else {
                        MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(3);
                    }
                }
            }
            MUtil.cancleDialog();
        }else if (UHFModuleType.RM_MODULE == UHFManager.getType()) {
            MyApp.getMyApp().getUhfMangerImpl().getRFIDProtocolStandard();
            MyApp.ifRMModule = true;
            judgeModuleTypeAndRefreshUI(false,false);
            MUtil.cancleDialog();
            Log.e("TAG", "getModuleInfo: " + MyApp.getMyApp().getUhfMangerImpl().powerGet() );
        }else if (UHFModuleType.GX_MODULE == UHFManager.getType()) {
            MUtil.cancleDialog();
            judgeModuleTypeAndRefreshUI(false,true);
        }
        if (MyApp.powerChange) {
            MyApp.getMyApp().getUhfMangerImpl().powerSet(Math.max(MMKV.defaultMMKV().decodeInt("setPower", 33), 5));
            MyApp.powerSize = MMKV.defaultMMKV().decodeInt("setPower", 33);
        }
        if (MyApp.isZhuYanCustom) {
            int mode = MMKV.defaultMMKV().decodeInt("inventoryMode", 4);
            MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(mode);
            if (mode == 4) {
                MyApp.getMyApp().getUhfMangerImpl().sessionModeSet(0);
            }
        }
    }

    private void judgeModuleTypeAndRefreshUI(final boolean isShowSearchPage,final boolean isShowInventoryPage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchTag.setVisibility(isShowSearchPage ? View.VISIBLE : View.GONE);
                inventoryTag.setVisibility(isShowInventoryPage ? View.VISIBLE : View.GONE);
                MUtil.cancleDialog();
                if (currentFragment != null && currentFragment instanceof BaseFragment) {
                    ((BaseFragment) currentFragment).refreshUI();
                    //Log.e("tag", "  judgeModuleTypeAndRefreshUI   ");
                }
            }
        });
    }

    private final int requestPermissionCode = 10;

    private void requestPermission() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestPermissionR();
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, requestPermissionCode);
            } else {
                ifRequesetPermission = false;
                init();
            }
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == requestPermissionCode) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                recyleResoure();
            } else {
                ifRequesetPermission = false;
                init();
            }
        }
    }

    //定时监听把枪状态
    // Listening for device status at regular intervals
    private void monitorEmsh() {
        mEmshStatusReceiver = new EmshStatusBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(EmshConstant.Action.INTENT_EMSH_BROADCAST);
        registerReceiver(mEmshStatusReceiver, intentFilter);

        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(EmshConstant.Action.INTENT_EMSH_REQUEST);
                intent.putExtra(EmshConstant.IntentExtra.EXTRA_COMMAND, EmshConstant.Command.CMD_REFRESH_EMSH_STATUS);
                sendBroadcast(intent);
            }
        };
        mTimer.schedule(mTimerTask, 0, 1000);
    }

    //设置当前显示的页面
    // Set the currently displayed page
    private void setCurrentPage(int tabPage) {
        FragmentTransaction transaction = manager.beginTransaction();
        switch (tabPage) {
            //主界面
            // Main screen
            case 0:
                currentFragment = mLeftFragment = (mLeftFragment == null ? new LeftFragment() : mLeftFragment);
                break;
            //设置界面
            // Setting screen    
            case 1:
                currentFragment = mRightFragment = (mRightFragment == null ? new RightFragment() : mRightFragment);
                break;
            //标签查找界面
            // Tag finder screen
            case 2:
                currentFragment = mSearchFragment = (mSearchFragment == null ? new SearchFragment() : mSearchFragment);
                break;
            case 3:
                currentFragment = mInventoryFragment = (mInventoryFragment == null ? new InventoryFragment() : mInventoryFragment);
                break;
            default:
                break;
        }
        transaction.replace(R.id.showData, (Fragment) currentFragment);
        transaction.commit();
    }

    private int[] pageId = {R.id.toLeft, R.id.toRight, R.id.searchTag,R.id.inventoryTag};

    public void onClick(View v) {
        for (int i = 0; i < pageId.length; i++) {
            if (pageId[i] == v.getId()) {
                if (GetRFIDThread.getInstance().isIfPostMsg() || GetRFIDThread.getInstance().getLockPostTag()) {
                    MUtil.show(R.string.notice_close_inventroy);
                    return;
                }
                setCurrentPage(i);
                toLeft.setImageResource(pageId[i] == R.id.toLeft ? R.drawable.main_click : R.drawable.main_noclick);
                toRight.setImageResource(pageId[i] == R.id.toRight ? R.drawable.set_click : R.drawable.set_noclick);
                searchTag.setImageResource(pageId[i] == R.id.searchTag ? R.drawable.search_clcik : R.drawable.search_noclcik);
                inventoryTag.setImageResource(pageId[i] == R.id.inventoryTag ? R.drawable.custom_click : R.drawable.custom_noclick);
                break;
            }
        }
    }

    //手柄按钮控制RFID线程读取与停止
    // Handle button controls RFID thread reading and stopping
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        MLog.e("keyCode" + keyCode);
        if (currentFragment instanceof OnKeyDownListener) {
            ((OnKeyDownListener) currentFragment).onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (currentFragment instanceof OnKeyListener) {
            ((OnKeyListener) currentFragment).onKeyUp(keyCode, event);
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        switchTriggerMode(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!ifRequesetPermission) {
            recyleResoure();
        }
        switchTriggerMode(true);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (!ifRequesetPermission) {
//            recyleResoure();
//        }
//        switchTriggerMode(true);
    }

    /**
     * 开关把枪触发iScan扫描的功能
     */
    private void switchTriggerMode(boolean flag) {
//        Intent switchIScanKey = new Intent("android.intent.action.UHF_CHECK_TRIGGER");
//        switchIScanKey.putExtra("isEnableScan", flag);
//        sendBroadcast(switchIScanKey);
        if (!flag) {
            Intent switchIScanKey = new Intent("android.intent.action.BARCODEUNLOCKSCANKEY");
            sendBroadcast(switchIScanKey);
        }else {
            Intent switchIScanKey = new Intent("android.intent.action.BARCODELOCKSCANKEY");
            sendBroadcast(switchIScanKey);
        }
    }

    private volatile long lastTime = 0;

    @Override
    public void onBackPressed() {
        long currentTime = SystemClock.currentThreadTimeMillis();
        if (lastTime != 0 && currentTime - lastTime < 500) {
            recyleResoure();
        } else {
            MUtil.show(R.string.exit_app);
        }
        lastTime = currentTime;
    }

    //下电，回收停止线程，退出应用
    // Power down, recycle stop threads, exit application
    private void recyleResoure() {
        //这里强制停止盘点，无论是否使用
        // Forced cessation of inventory, whether used or not
        MyApp.getMyApp().getUhfMangerImpl().stopInventory();
        switchTriggerMode(true);
        if (mEmshStatusReceiver != null) {
            unregisterReceiver(mEmshStatusReceiver);
            mEmshStatusReceiver = null;
        }
        if (mTimer != null || mTimerTask != null) {
            mTimerTask.cancel();
            mTimer.cancel();
            mTimerTask = null;
            mTimer = null;
        }
        rfidThread.destoryThread();
        MLog.e("poweroff = " + MyApp.getMyApp().getUhfMangerImpl().powerOff());
        MyApp.getMyApp().getUhfMangerImpl().changeConfig(false);
        unRegisterPowerStatus();
        releaseWakeLock();
        unRegisterPowerCapacity();
        System.exit(0);
    }

    PowerManager.WakeLock wakeLock;
    /**
     * 请求唤醒锁
     */
    public void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
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

    private EmshStatusBroadcastReceiver mEmshStatusReceiver;

    private int oldStatue = -1;

    public class EmshStatusBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (EmshConstant.Action.INTENT_EMSH_BROADCAST.equalsIgnoreCase(intent.getAction())) {

                int sessionStatus = intent.getIntExtra("SessionStatus", 0);
                int batteryPowerMode = intent.getIntExtra("BatteryPowerMode", -1);
                //MLog.e("sessionStatus = " + sessionStatus + "  batteryPowerMode  = " + batteryPowerMode);
                // 把枪电池当前状态
                // Current status of battery
                if ((sessionStatus & EmshConstant.EmshSessionStatus.EMSH_STATUS_POWER_STATUS) != 0) {
                    //相同状态不处理
                    // Same status does not process
                    if (batteryPowerMode == oldStatue) {
                        MUtil.cancelWaringDialog();
                        return;
                    }
                    oldStatue = batteryPowerMode;
                    switch (batteryPowerMode) {
                        case EMSH_PWR_MODE_STANDBY:
                            MLog.e("standby status 1 ifPowerOn = "+ifPowerOn);
//                            ifCharge = false;
//                            if (!ifPowerOn) {
//                                ifPowerOn = MyApp.getMyApp().getUhfMangerImpl().powerOn();
//                                getModuleInfo();
//                            }
//                            ifPowerOn = false;
                            break;
                        case EMSH_PWR_MODE_DSG_UHF:
                            MLog.e("DSG_UHF status");
//                            if (!ifPowerOn || ifCharge) {
//                                MLog.e("...power on again poweron "); //...power on again
//                                MyApp.getMyApp().getUhfMangerImpl().powerOff();
//                                SystemClock.sleep(200);
//                                ifCharge = false;
//                                ifPowerOn = MyApp.getMyApp().getUhfMangerImpl().powerOn();
//                            }
//                            MLog.e("...power on again poweron = " + ifPowerOn); //...power on again
                            MUtil.show(R.string.poweron_success);
                            break;
                        case EMSH_PWR_MODE_CHG_GENERAL:
                        case EMSH_PWR_MODE_CHG_QUICK:
                            ifCharge = true;
                            ifPowerOn = false;
                            MLog.e("charging status");
                            MUtil.show(R.string.charing);
                            break;
                        case EMSH_PWR_MODE_CHG_FULL:
                            ifCharge = true;
                            MLog.e("charging full status");
                            MUtil.show(R.string.charing_full);
                            break;
                        default:
                            break;
                    }
                } else {
                    oldStatue = EMSH_PWR_MODE_BATTERY_ERROR;
                    MLog.e("unknown status");
                    MUtil.warningDialog(MainActivity.this);
                }
            }
        }
    }
}
