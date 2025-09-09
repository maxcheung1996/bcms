package com.uhf.uhfdemo;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.media.AudioManager;
import android.media.SoundPool;

import com.example.iscandemo.iScanInterface;
import com.uhf.base.UHFModuleType;
import com.uhf.util.MLog;
import com.tencent.mmkv.MMKV;
import com.uhf.base.UHFManager;
import com.uhf.util.MyCrashHandler;

import realid.rfidlib.MyLib;


/**
 * author CYD
 * date 2018/11/19
 */
public class MyApp extends Application {

    public static byte[] UHF = {0x01, 0x02, 0x03};
    private UHFManager uhfMangerImpl;
    private static MyApp myApp;
    private SoundPool soundPool;
    private int soundID;
    //是否启动盘点声音
    // Whether to activate the inventory sound
    public static boolean ifOpenSound = false;
    //应用是否处于弹框状态
    // Is the application in a pop-up box
    //  public static AlertDialog showAtd = null;
    public static int currentInvtDataType = -1;
    public static boolean ifSupportR2000Fun = true;
    public static boolean if5100Module = false;
    public static boolean if7100Module = false;
    public static boolean ifRMModule = false;

    public static boolean ifLockTagRead = false;
    public static boolean ifUM510 = false;

    //是否以ASCII码显示
    public static boolean ifASCII = false;

    //是否自动停止读卡
    public static boolean ifAutoStopLabel = false;

    //自动停止读卡时间
    public static int stopLabelTime = -1;

    //设置标签协议 1:ISO 2:GB
    public static int protocolType = 1;

    public static boolean ifFirst = true;

    public static int powerSize = 5;
    //控制将功率设到0的标志（规则：功率设置小于5实际设成5，等于0不读卡）
    public static boolean powerChange = false;

    public static int MaxPower = 33;

    //用来保存设置的参数的标志，暂时下电保存功率，频率，盘点模式
    public static boolean saveSet = true;

    public static boolean isShangHaiTaoPinCustom = false;

    public static boolean isZhuYanCustom = false;

    public static boolean isZhuYanCustomReading = false;
    public float volume = 1;
    private iScanInterface miScanInterface;
    public static boolean isLowPower = false;
    public static boolean isChangeBaud = false;

//    public static UHFModuleType currentUHFModule = UHFModuleType.UM_MODULE;

    @Override
    public void onCreate() {
        super.onCreate();
        myApp = this;
        MMKV.initialize(this);
        // 默认true开启日志调试，false关闭
        // Default true, true to enable logging debugging, false to disable
        MLog.ifShown = isApkInDebug();
        soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        soundID = soundPool.load(this, R.raw.beep, 1);
        MyCrashHandler.getInstance().init(this);
        //miScanInterface = new iScanInterface(this);
    }

    public static MyApp getMyApp() {

        return myApp;
    }

    public void setUhfMangerImpl(UHFManager uhfMangerImpl) {
        this.uhfMangerImpl = uhfMangerImpl;
    }

    public UHFManager getUhfMangerImpl() {
        return uhfMangerImpl;
    }

    public iScanInterface getiScanInterface() {
        return miScanInterface;
    }


    //播放滴滴滴的声音
    // Play the "di di di" sound
    public void playSound() {
        soundPool.play(soundID, volume, volume, 0, 1, 1);
    }

    public void setVolume(float volume) {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if(mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM) > 6) {
            this.volume = volume;
            soundPool.setVolume(soundID,volume,volume);
        }
    }

    /**
     * 判断当前应用是否是debug状态
     * Judge whether the current application is in debug state
     *
     * @return true当前为debug版本的apk，false不是debug版本
     * True is currently the APK of the debug version, and false is not the debug version
     */
    private boolean isApkInDebug() {
        try {
            ApplicationInfo info = getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }
}
