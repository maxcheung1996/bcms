package com.uhf.setting;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.tencent.mmkv.MMKV;
import com.uhf.event.BaseFragment;
import com.uhf.uhfdemo.LeftFragment;
import com.uhf.uhfdemo.MyApp;
import com.uhf.uhfdemo.R;
import com.uhf.util.MLog;
import com.uhf.util.MUtil;
import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

import static com.uhf.uhfdemo.MyApp.if7100Module;
import static com.uhf.uhfdemo.MyApp.ifRMModule;
import static com.uhf.uhfdemo.MyApp.ifSupportR2000Fun;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * author CYD
 * date 2018/11/22
 * email cyd19950902@qq.com
 */
public class PoweFrequencyFragment extends BaseFragment implements View.OnClickListener {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.common_setting, container, false);
    }

    private Spinner powerGet, area_frequency, sesionMode, invMode, protocolType, sp_fastid,sp_tid_filter,powerReadGet,powerWriteGet,sp_target,sp_encode;

    private EditText inventory_time, sleep_time,showtemp,stop_time;
    private CheckBox poweroff_save, save_inv_mode;

    private TextView showNeedList;
    private MMKV mkv;

    private EditText Et_times, Et_num, Et_work_time, Et_sleep_time, Et_tag_num;

    private int[] cn_920 = {922375, 921125, 924125, 920875, 921875, 921375, 923875, 922625, 923125, 924375, 922125, 923375, 920625, 921625, 923625, 922875};
    private int[] cn_840 = {842375, 841125, 844125, 840875, 841875, 841375, 843875, 842625, 843125, 844375, 842125, 843375, 840625, 841625, 843625, 842875};
    private int[] eu = {867500, 866300, 865700, 866900};
    private int[] us = {915750, 927250, 902750, 915250, 903250, 926750, 910750, 922750, 906750, 926250, 904250, 920250, 919250, 909250, 918750, 917750,
            905250, 904750, 925250, 921750, 914750, 913750, 922250, 911250, 911750, 903750, 908750, 905750, 912250, 906250, 917250, 914250, 907250, 918250,
            916250, 910250, 907750, 924750, 909750, 919750, 916750, 913250, 923750, 908250, 925750, 912750, 924250, 921250, 920750, 923250};

    private int[] cn_920_210 = {920125, 920375, 920625, 920875, 921125, 921375, 921625, 921875, 922125, 922375, 922625, 923375, 923625, 923875, 924125, 924375, 924625, 924875};
    private int[] us_210 = {902250, 902750, 903250, 903750, 904250, 904750, 905250, 905750, 906250, 906750, 907250, 907750, 908250, 908750, 909250, 909750, 910250, 910750,
            911250, 911750, 912250, 912750, 913250, 913750, 914250, 914750, 915250, 915750, 916250, 916750, 917250, 917750, 918250, 918750, 919250, 919750, 920250,
            920750, 921250, 921750, 922250, 922750, 923250, 923750, 924250, 924750, 925250, 925750, 926250, 926750, 927250};
    private boolean ifJ06;

    private LinearLayout temp;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        sp_fastid = view.findViewById(R.id.sp_fastid);
        sp_tid_filter = view.findViewById(R.id.sp_tid_filter);
        view.findViewById(R.id.get_fastid).setOnClickListener(this);
        view.findViewById(R.id.set_fastid).setOnClickListener(this);
        view.findViewById(R.id.set_tid_filter).setOnClickListener(this);
        showNeedList = view.findViewById(R.id.showNeedList);
        powerGet = view.findViewById(R.id.powerGet);
        area_frequency = view.findViewById(R.id.area_frequency);
        sesionMode = view.findViewById(R.id.sesionMode);
        invMode = view.findViewById(R.id.invMode);
        save_inv_mode = view.findViewById(R.id.save_inv_mode);
        ((CheckBox) view.findViewById(R.id.soundSwitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MyApp.ifOpenSound = isChecked;
            }
        });

        ((CheckBox) view.findViewById(R.id.ASCIIorHex)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MyApp.ifASCII = isChecked;
            }
        });

        ((CheckBox) view.findViewById(R.id.restricted_reading)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MyApp.isZhuYanCustomReading = isChecked;
            }
        });
        stop_time = view.findViewById(R.id.stop_time);
        view.findViewById(R.id.start_stop_time).setOnClickListener(this);
        view.findViewById(R.id.close_stop_time).setOnClickListener(this);

        view.findViewById(R.id.get_temp).setOnClickListener(this);
        view.findViewById(R.id.setPower).setOnClickListener(this);
        view.findViewById(R.id.getPower).setOnClickListener(this);
        view.findViewById(R.id.set_area_frequency).setOnClickListener(this);
        view.findViewById(R.id.get_area_frequency).setOnClickListener(this);
        LinearLayout session_setting_title = view.findViewById(R.id.session_setting_title);
        Button setSessionMode = view.findViewById(R.id.set_session_mode);
        setSessionMode.setOnClickListener(this);
        Button getSessionMode = view.findViewById(R.id.get_session_mode);
        getSessionMode.setOnClickListener(this);
        view.findViewById(R.id.set_frq_rang).setOnClickListener(this);
        view.findViewById(R.id.get_frq_rang).setOnClickListener(this);
        Button set_inv_mode = view.findViewById(R.id.set_inv_mode);
        set_inv_mode.setOnClickListener(this);
        Button get_inv_mode = view.findViewById(R.id.get_inv_mode);
        get_inv_mode.setOnClickListener(this);
        view.findViewById(R.id.getProtocol).setOnClickListener(this);
        view.findViewById(R.id.setProtocol).setOnClickListener(this);
        protocolType = view.findViewById(R.id.protocol);

        showtemp = view.findViewById(R.id.showTemp);
        temp = view.findViewById(R.id.temp);
        inventory_time = view.findViewById(R.id.inventory_time);
        sleep_time = view.findViewById(R.id.sleep_time);
        poweroff_save = view.findViewById(R.id.poweroff_save);
        view.findViewById(R.id.set_inventory_time).setOnClickListener(this);
        view.findViewById(R.id.get_inventory_time).setOnClickListener(this);

        //芯联target 编码设置
        sp_target = view.findViewById(R.id.target);
        sp_encode = view.findViewById(R.id.decode);
        view.findViewById(R.id.get_target_mode).setOnClickListener(this);
        view.findViewById(R.id.set_target_mode).setOnClickListener(this);
        view.findViewById(R.id.get_decode_mode).setOnClickListener(this);
        view.findViewById(R.id.set_decode_mode).setOnClickListener(this);

        //读写功率单独设置
        powerReadGet = view.findViewById(R.id.powerReadGet);
        powerWriteGet = view.findViewById(R.id.powerWriteGet);
        view.findViewById(R.id.setReadPower).setOnClickListener(this);
        view.findViewById(R.id.getReadPower).setOnClickListener(this);
        view.findViewById(R.id.setWritePower).setOnClickListener(this);
        view.findViewById(R.id.getWritePower).setOnClickListener(this);

        //判断是否为小i设备
        // Determine if it is an i-series
        ifJ06 = MyApp.getMyApp().getUhfMangerImpl().ifJ06();

        //判断是否为UM7模块
        // Determine if it is a UM7 module
        if (!ifSupportR2000Fun) {
            int firmwareVersion = Integer.valueOf(MyApp.getMyApp().getUhfMangerImpl().firmwareVerGet().substring(0,1));
            MLog.e("firmwareVersion = " + firmwareVersion);
            if(firmwareVersion < 2 ){
                area_frequency.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                        android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.frequency_only_cn2_usa_l)));
            }else {
                area_frequency.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                        android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.frequency_only_cn2_usa)));
            }
            invMode.setAdapter(new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.invMode_only_fast_auto)));
            getSessionMode.setVisibility(View.GONE);
            setSessionMode.setVisibility(View.GONE);
            session_setting_title.setVisibility(View.GONE);
        }
        if (UHFModuleType.SLR_MODULE == UHFManager.getType()) {
            view.findViewById(R.id.frq_param).setVisibility(View.VISIBLE);
            ((View) invMode.getParent()).setVisibility(View.VISIBLE);
            invMode.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.r2000_invMode)));
            if (!MyApp.if5100Module) {
                set_inv_mode.setVisibility(View.VISIBLE);
                get_inv_mode.setVisibility(View.VISIBLE);
            }else {
                set_inv_mode.setVisibility(View.GONE);
                get_inv_mode.setVisibility(View.GONE);
            }
            save_inv_mode.setVisibility(View.GONE);
            if (MyApp.isZhuYanCustom) {
                if (!MyApp.if5100Module)
                    save_inv_mode.setVisibility(View.VISIBLE);
            }
        }
        if (UHFModuleType.RM_MODULE == UHFManager.getType()) {
            view.findViewById(R.id.frq_param).setVisibility(View.VISIBLE);
            ((View) invMode.getParent()).setVisibility(View.VISIBLE);
            view.findViewById(R.id.protocol_set_get).setVisibility(View.VISIBLE);
            view.findViewById(R.id.protocolType).setVisibility(View.VISIBLE);
            protocolType.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.protocol_type)));
            invMode.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.RM01_invMode)));
            save_inv_mode.setVisibility(View.GONE);
            set_inv_mode.setVisibility(View.VISIBLE);
            get_inv_mode.setVisibility(View.VISIBLE);
            view.findViewById(R.id.session_fun_layout).setVisibility(View.VISIBLE);
        }
        if (UHFModuleType.SLR_MODULE == UHFManager.getType() && MyApp.if7100Module) {
            area_frequency.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                    android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.frequency_7100)));
            view.findViewById(R.id.poweroff_save).setVisibility(View.GONE);
            set_inv_mode.setVisibility(View.VISIBLE);
            get_inv_mode.setVisibility(View.VISIBLE);
            ((View) invMode.getParent()).setVisibility(View.VISIBLE);
            invMode.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.e710_invMode)));
            view.findViewById(R.id.sleep_time_layout).setVisibility(View.GONE);
            view.findViewById(R.id.frq_param).setVisibility(View.VISIBLE);
            view.findViewById(R.id.target_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.decode_layout).setVisibility(View.VISIBLE);
        }
        if (UHFModuleType.GX_MODULE == UHFManager.getType()) {
            view.findViewById(R.id.protocol_set_get).setVisibility(View.VISIBLE);
            view.findViewById(R.id.protocolType).setVisibility(View.VISIBLE);
            protocolType.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.protocol_type_gx)));
            invMode.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.invMode_gx)));
            view.findViewById(R.id.frq_param).setVisibility(View.GONE);
            temp.setVisibility(View.GONE);
        }
        Integer[] powerData;
        if (MyApp.if5100Module) {
            view.findViewById(R.id.sleep_time_layout).setVisibility(View.GONE);
            view.findViewById(R.id.session_fun_layout).setVisibility(View.GONE);
            if (!MyApp.powerChange) {
                powerData = new Integer[26];
                for (int i = 5; i <= 30; i++) {
                    powerData[i - 5] = i;
                }
            } else {
                powerData = new Integer[31];
                for (int i = 0; i <= 30; i++) {
                    powerData[i] = i;
                }
            }
        } else {
            if (!MyApp.powerChange) {
                int size = ifJ06 ? 11 : (ifSupportR2000Fun ? 26 + 3 : 26);
                view.findViewById(R.id.um7ExtraFun).setVisibility(ifJ06 ? View.GONE : View.VISIBLE);
                powerData = new Integer[size];
            }else {
                int size = ifJ06 ? 11 : (ifSupportR2000Fun ? 31 + 3 : 31);
                view.findViewById(R.id.um7ExtraFun).setVisibility(ifJ06 ? View.GONE : View.VISIBLE);
                powerData = new Integer[size];
            }
            if (ifRMModule) {
                powerData = new Integer[24];
                for (int i= 10;i<=33;i++)
                {
                    powerData[i - 10] = i;
                }
            }
            else if (ifSupportR2000Fun) {
                if (!MyApp.powerChange) {
                    for (int i = 5; i <= 30 + 3; i++) {
                        powerData[i - 5] = i;
                    }
                }else {
                    for (int i = 0; i <= 30 + 3; i++) {
                        powerData[i] = i;
                    }
                }
            } else if (ifJ06) {
                for (int i = 0; i <= 10; i++) {
                    powerData[i] = i;
                }
            } else {
                for (int i = 0; i <= 25; i++) {
                    powerData[i] = i;
                }
            }
        }
        ArrayAdapter adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, powerData);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        powerGet.setAdapter(adapter);
        powerWriteGet.setAdapter(adapter);
        powerReadGet.setAdapter(adapter);
        initPower();
        getAreaFrequency();
        if (MyApp.isZhuYanCustom) {
            view.findViewById(R.id.restricted_reading).setVisibility(View.VISIBLE);
        }
    }

    private void initView(View view) {
        Et_times = view.findViewById(R.id.ct_params).findViewById(R.id.Et_times);
        Et_num = view.findViewById(R.id.ct_params).findViewById(R.id.Et_num);
        Et_work_time = view.findViewById(R.id.ct_params).findViewById(R.id.Et_work_time);
        Et_sleep_time = view.findViewById(R.id.ct_params).findViewById(R.id.Et_sleep_time);
        Et_tag_num = view.findViewById(R.id.ct_params).findViewById(R.id.Et_tag_num);
        view.findViewById(R.id.ct_params).findViewById(R.id.Bt_set_params).setOnClickListener(this);
    }

    //获取功率
    // Get power
    private boolean initPower() {
        int power = MyApp.getMyApp().getUhfMangerImpl().powerGet();
        MLog.e(power + "");
        if (power < 0 || power > 30 + 3) {
            return false;
        }
        if (ifRMModule) {
            if (power >=10) {
                powerGet.setSelection(power - 10);
                MLog.e("power = " + power);
                return true;
            }
        }
        else if (ifSupportR2000Fun) {
            if (power >= 5) {
                powerGet.setSelection(power - 5);
                MLog.e("power = " + power);
                return true;
            }
        } else if (ifJ06) {
            powerGet.setSelection(power);
            return true;
        } else {
            if (power > 25)
                power = 25;
            powerGet.setSelection(power);
            return true;
        }
        return false;
    }

    //获取功率
    // Get power
    private boolean initPower1() {
        int power = MyApp.getMyApp().getUhfMangerImpl().powerGet();
        MLog.e(power + "");
        if (power < 0 || power > 30 + 3) {
            return false;
        }
        if (ifRMModule) {
            if (power >=10) {
                powerGet.setSelection(power - 10);
                MLog.e("power = " + power);
                return true;
            }
        }
        else if (ifSupportR2000Fun) {
            if (!MyApp.powerChange) {
                if (power >= 5) {
                    if (power == 33) {
                        powerGet.setSelection(MyApp.MaxPower-5);
                    }else
                        powerGet.setSelection(power - 8);
                    MLog.e("power = " + power);
                    return true;
                }
            }else {
                if (power == 5) {
                    powerGet.setSelection(MMKV.defaultMMKV().decodeInt("setPower"));
                    return true;
                } else if (power > 5) {
                    powerGet.setSelection(power);
                    return true;
                }
            }
        } else if (ifJ06) {
            powerGet.setSelection(power);
            return true;
        } else {
            if (power > 25)
                power = 25;
            powerGet.setSelection(power);
            return true;
        }
        return false;
    }

    //获取区域频率
    // Get area frequency
    private boolean getAreaFrequency() {
        int area_fq = MyApp.getMyApp().getUhfMangerImpl().frequencyModeGetNotFixedFreq();
        MLog.e("area_fq = " + area_fq);
        if (area_fq < 0)
            return false;
        if (ifSupportR2000Fun && !if7100Module && !ifRMModule) {
            area_frequency.setSelection(area_fq);
        } else if (if7100Module){
            switch (area_fq) {
                case 1:
                    area_frequency.setSelection(0);
                    break;
                case 2:
                    area_frequency.setSelection(1);
                    break;
                case 3:
                    area_frequency.setSelection(2);
                    break;
                default:
                    return false;
            }
        }else if (ifRMModule) {
            if (area_fq == 0) {
                area_frequency.setSelection(0);
            } else if (area_fq == 1) {
                area_frequency.setSelection(1);
            } else if (area_fq == 2) {
                area_frequency.setSelection(2);
            } else if (area_fq == 3) {
                 area_frequency.setSelection(3);
            } else {
                return false;
            }

        }else {
            switch (area_fq) {
                case 1:
                    area_frequency.setSelection(0);
                    break;
                case 3:
                    area_frequency.setSelection(1);
                    break;
                case 5:

                    area_frequency.setSelection(2);
                    break;
                default:
                    return false;
            }
        }
/*        boolean areaFQFlag = area_fq >= 0 && area_fq <= 3;
        if (areaFQFlag) {
            if (ifSupportR2000Fun) {
                area_frequency.setSelection(area_fq);
            } else {
                area_frequency.setSelection(area_fq == 1 ? 0 : 1);
            }
        }*/
        return true;
    }

    private void setPower() {
        int value = (int) powerGet.getSelectedItem();
        MLog.e("value = " + value);
        boolean ifValue = MyApp.getMyApp().getUhfMangerImpl().powerSet(value);
        setResult(ifValue);
        if (ifValue && MyApp.saveSet)
            MMKV.defaultMMKV().encode("power",value);
    }

    private void setPower1() {
        int value = (int) powerGet.getSelectedItem();
        MLog.e("value = " + value);
        if (MyApp.powerChange) {
            if (value<=5 && value >=0) {
                MyApp.powerSize = value;
                value = 5;
                MMKV.defaultMMKV().encode("setPower",MyApp.powerSize);
            } else
                MMKV.defaultMMKV().encode("setPower",value);
        }
        boolean ifValue = false;
        if (value >=30) {
            ifValue = MyApp.getMyApp().getUhfMangerImpl().powerSet(33);
            MyApp.MaxPower = value;
        }else
            ifValue = MyApp.getMyApp().getUhfMangerImpl().powerSet(value + 3);
        setResult(ifValue);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 设置功率
            // set power
            case R.id.setPower:
                setPower();
                break;
            case R.id.getPower:
                setResult(initPower());
                break;
            //设置区域频率
            // set area frequency
            case R.id.set_area_frequency:
                int postion = 0;
                if (ifSupportR2000Fun && !if7100Module) {
                    postion = area_frequency.getSelectedItemPosition();
                } else if (if7100Module) {
                    postion = area_frequency.getSelectedItemPosition();
                    switch (postion) {
                        case 0:
                            postion = 1;
                            break;
                        case 1:
                            postion = 2;
                            break;
                        case 2:
                            postion = 3;
                            break;
                        default:
                            break;
                    }
                }else {
                    postion = area_frequency.getSelectedItemPosition();
                    switch (postion) {
                        case 0:
                            postion = 1;
                            break;
                        case 1:
                            postion = 3;
                            break;
                        case 2:
                            postion = 5;
                            break;
                        default:
                            break;
                    }
                }
                MLog.e("postion = " + postion);
                boolean ifSetAreaFQ = MyApp.getMyApp().getUhfMangerImpl().frequencyModeSet(postion);
                setResult(ifSetAreaFQ);
                if (ifSetAreaFQ && MyApp.saveSet)
                    MMKV.defaultMMKV().encode("frequencyModeSet",postion);
                break;
            case R.id.get_area_frequency:
                setResult(getAreaFrequency());
                break;
            case R.id.set_session_mode:
                int vals = sesionMode.getSelectedItemPosition();
                setResult(MyApp.getMyApp().getUhfMangerImpl().sessionModeSet(vals));
                break;
            case R.id.get_session_mode:
                //session模式
                //session mode
                int sMode = MyApp.getMyApp().getUhfMangerImpl().sessionModeGet();
                boolean modeGetFlag = sMode > -1 && sMode < 4;
                if (modeGetFlag) {
                    sesionMode.setSelection(sMode);
                }
                System.currentTimeMillis();
                setResult(modeGetFlag);
                break;
            case R.id.set_inventory_time:
                String iTime = inventory_time.getText().toString();
                String sTime = sleep_time.getText().toString();
                if (TextUtils.isEmpty(iTime) || TextUtils.isEmpty(sTime)) {
                    MUtil.show(R.string.time_empty_notice);
                } else {
                    boolean ifCheck = poweroff_save.isChecked();
                    boolean flag = MyApp.getMyApp().getUhfMangerImpl().inventoryWaitTime_Set(Integer.parseInt(iTime), Integer.parseInt(sTime), ifCheck);
                    setResult(flag);
                }
                break;
            case R.id.get_inventory_time:
                int[] times = MyApp.getMyApp().getUhfMangerImpl().inventoryWaitTime_Get();
                if (times == null) {
                    setResult(false);
                } else {
                    inventory_time.setText(times[0] + "");
                    sleep_time.setText(times[1] + "");
                }
                break;
            case R.id.set_frq_rang:
                selectFrq();
                break;
            case R.id.get_frq_rang:
                showNeedList.setText(null);
                //当前实际设置的频点
                // Current frequency range of settings
                int[] getFrqRange = null;
                getFrqRange = MyApp.getMyApp().getUhfMangerImpl().frequenceRange_Get();
                if (getFrqRange == null) {
                    setResult(false);
                    return;
                }
                for (int i = 0; i < getFrqRange.length; i++) {
                    if (i < getFrqRange.length - 1)
                        showNeedList.append(getFrqRange[i] + ",");
                    else
                        showNeedList.append(getFrqRange[i] + "");
                }
                setResult(true);
                break;
            case R.id.set_inv_mode:
                boolean flag = false;
                int mode = 0;
                if (if7100Module) {
                    if (invMode.getSelectedItemPosition() == 0){
                        mode = 4;
                    } else if (invMode.getSelectedItemPosition() == 1){
                        mode = 3;
                    }else if (invMode.getSelectedItemPosition()==2){
                        mode = 5;
                    }else if (invMode.getSelectedItemPosition()==3){
                        mode = 6;
                    }
                    flag = MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(mode);
                    if (MyApp.saveSet)
                        MMKV.defaultMMKV().encode("inventoryMode",mode);
                }else if (ifRMModule) {
                    if (invMode.getSelectedItemPosition() == 0) {
                        mode = 0;
                    }else if (invMode.getSelectedItemPosition() == 1) {
                        mode = 1;
                    }else if (invMode.getSelectedItemPosition() == 2) {
                        mode = 2;
                    }
                    flag = MyApp.getMyApp().getUhfMangerImpl().inventoryModelSet(mode,true);
                }else if (UHFManager.getType()==UHFModuleType.SLR_MODULE) {
                    if (invMode.getSelectedItemPosition() == 0){
                        mode = 4;
                    } else if (invMode.getSelectedItemPosition() == 1){
                        mode = 2;
                    }
                    flag = MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(mode);
                }else if (UHFModuleType.GX_MODULE == UHFManager.getType()) {
                    mode = invMode.getSelectedItemPosition();
                    flag = MyApp.getMyApp().getUhfMangerImpl().inventoryModelSet(mode,true);
                }else {
                    if (ifSupportR2000Fun)
                        flag = MyApp.getMyApp().getUhfMangerImpl().inventoryModelSet(invMode.getSelectedItemPosition(), save_inv_mode.isChecked());
                    else
                        flag = MyApp.getMyApp().getUhfMangerImpl().inventoryModelSet(invMode.getSelectedItemPosition() == 0 ? 1 : 4, save_inv_mode.isChecked());
                }
                setResult(flag);
                if (MyApp.isZhuYanCustom) {
                    if (flag && save_inv_mode.isChecked()) {
                        MMKV.defaultMMKV().encode("inventoryMode", mode);
                    }
                    if (mode == 4) {
                        MyApp.getMyApp().getUhfMangerImpl().sessionModeSet(0);
                    }
                }

                break;
            case R.id.get_inv_mode:
                if (if7100Module) {
                    int val = MyApp.getMyApp().getUhfMangerImpl().slrInventoryModelGet();
                    if (val == 4) {
                        invMode.setSelection(0);
                    }else if(val == 3) {
                        invMode.setSelection(1);
                    }else if (val == 5) {
                        invMode.setSelection(2);
                    }else if (val == 6) {
                        invMode.setSelection(3);
                    }
                    setResult(val == 4 || val == 3 || val == 5 ||val == 6);
                }else if (ifRMModule){
                    int val = MyApp.getMyApp().getUhfMangerImpl().inventoryModelGet();
                    invMode.setSelection(val);
                    setResult(val > -1);
                } else if (UHFManager.getType()==UHFModuleType.SLR_MODULE) {
                    int val = MyApp.getMyApp().getUhfMangerImpl().slrInventoryModelGet();
                    if (val == 4) {
                        invMode.setSelection(0);
                    }else if(val == 2) {
                        invMode.setSelection(1);
                    }
                    setResult(val == 4 || val == 2);
                }else {
                    int val = MyApp.getMyApp().getUhfMangerImpl().inventoryModelGet();
                    boolean getInvResult = val >= 0 && val < 5;
                    if (getInvResult) {
                        if (ifSupportR2000Fun)
                            invMode.setSelection(val);
                        else
                            invMode.setSelection(val == 1 ? 0 : 1);
                    }
                    setResult(getInvResult);
                }

                break;
            case R.id.get_temp:
                String temp = MyApp.getMyApp().getUhfMangerImpl().getModuleTemp();
                if (temp != null) {
                    showtemp.setText(temp);
                    setResult(true);
                }else {
                    setResult(false);
                }
                break;
            case R.id.start_stop_time:
                MyApp.ifAutoStopLabel = true;
                if (TextUtils.isEmpty(stop_time.getText().toString())) {
                    MyApp.ifAutoStopLabel = false;
                }else {
                    MyApp.stopLabelTime = Integer.parseInt(stop_time.getText().toString());
                }
                break;
            case R.id.close_stop_time:
                MyApp.ifAutoStopLabel = false;
                break;
            case R.id.getProtocol:
                int protocol = MyApp.getMyApp().getUhfMangerImpl().getRFIDProtocolStandard();
                if (UHFModuleType.RM_MODULE == UHFManager.getType()) {
                    if (protocol > -1) {
                        protocolType.setSelection(protocol);
                        setResult(true);
                    } else setResult(false);
                }else if (UHFModuleType.GX_MODULE == UHFManager.getType()) {
                    if (protocol == 0) {
                        protocolType.setSelection(0);
                        setResult(true);
                    }else if (protocol == 2) {
                        protocolType.setSelection(1);
                        setResult(true);
                    }else
                        setResult(false);
                }
                break;
            case R.id.setProtocol:
                if (UHFModuleType.RM_MODULE == UHFManager.getType()) {
                    boolean setProtocol = MyApp.getMyApp().getUhfMangerImpl().setRFIDProtocolStandard(protocolType.getSelectedItemPosition());
                    setResult(setProtocol);
                    if (setProtocol)
                        MyApp.protocolType = protocolType.getSelectedItemPosition() + 1;
                }else if (UHFModuleType.GX_MODULE == UHFManager.getType()) {
                    int setProtocol = protocolType.getSelectedItemPosition();
                    setResult(MyApp.getMyApp().getUhfMangerImpl().setRFIDProtocolStandard(setProtocol==0?0:2));
                    MyApp.protocolType = setProtocol ==0 ? 1 : setProtocol == 1 ? 3: 1;
                }
                break;
            case R.id.set_fastid:
                setResult(MyApp.getMyApp().getUhfMangerImpl().setFasTidMode(sp_fastid.getSelectedItemPosition()));
                break;
            case R.id.get_fastid:

                break;
            case R.id.set_tid_filter:
                setResult(MyApp.getMyApp().getUhfMangerImpl().setTidRepetition(sp_tid_filter.getSelectedItemPosition() == 1));
                break;
            case R.id.Bt_set_params:
                setResult(MyApp.getMyApp().getUhfMangerImpl().setQuickModeParams(Integer.parseInt(Et_times.getText().toString()),Integer.parseInt(Et_num.getText().toString()),Integer.parseInt(Et_work_time.getText().toString()),Integer.parseInt(Et_sleep_time.getText().toString()),Integer.parseInt(Et_tag_num.getText().toString())));
                break;
            case R.id.setReadPower:
                setReadPower();
                break;
            case R.id.getReadPower:
                setResult(getReadPower());
                break;
            case R.id.setWritePower:
                setWritePower();
                break;
            case R.id.getWritePower:
                setResult(getWritePower());
                break;
            case R.id.set_target_mode:
                setTarget();
                break;
            case R.id.get_target_mode:
                getTarget();
                break;
            case R.id.get_decode_mode:
                getEncode();
                break;
            case R.id.set_decode_mode:
                setEncode();
                break;
            default:
                break;

        }

    }

    private void setTarget() {
        int value = sp_target.getSelectedItemPosition();
        boolean isSuc = MyApp.getMyApp().getUhfMangerImpl().setGen2Target(value);
        setResult(isSuc);
    }

    private void getTarget() {
        int value = MyApp.getMyApp().getUhfMangerImpl().getGen2Target();
        if (value>-1){
            sp_target.setSelection(value);
        }else{
            setResult(false);
        }
    }

    private void getEncode() {
        int value = MyApp.getMyApp().getUhfMangerImpl().getGen2Decode();
        if (value>-1){
            sp_encode.setSelection(value);
        }else{
            setResult(false);
        }
    }

    private void setEncode() {
        int value = sp_encode.getSelectedItemPosition();
        boolean isSuc = MyApp.getMyApp().getUhfMangerImpl().setGen2Decode(value);
        setResult(isSuc);
    }


    private void setWritePower() {
        int value = (int) powerWriteGet.getSelectedItem();
        MLog.e("value = " + value);
        int[] readWritePower = MyApp.getMyApp().getUhfMangerImpl().getReadWritePower();
        if (readWritePower == null) {
            setResult(false);
            return;
        }
        boolean ifValue = MyApp.getMyApp().getUhfMangerImpl().setReadWritePower(readWritePower[0],value);
        setResult(ifValue);
    }

    private void setReadPower() {
        int value = (int) powerReadGet.getSelectedItem();
        MLog.e("value = " + value);
        int[] readWritePower = MyApp.getMyApp().getUhfMangerImpl().getReadWritePower();
        if (readWritePower == null) {
            setResult(false);
            return;
        }
        boolean ifValue = MyApp.getMyApp().getUhfMangerImpl().setReadWritePower(value,readWritePower[1]);
        setResult(ifValue);

    }

    private boolean getReadPower() {
        int[] readWritePower = MyApp.getMyApp().getUhfMangerImpl().getReadWritePower();
        if (readWritePower == null) {
            return false;
        }
        int power = readWritePower[0];
        if (power < 0 || power > 30 + 3) {
            return false;
        }
        if (ifRMModule) {
            if (power >=10) {
                powerReadGet.setSelection(power - 10);
                MLog.e("power = " + power);
                return true;
            }
        } else if (ifSupportR2000Fun) {
            if (power >= 5) {
                powerReadGet.setSelection(power - 5);
                MLog.e("power = " + power);
                return true;
            }
        } else if (ifJ06) {
            powerReadGet.setSelection(power);
            return true;
        } else {
            if (power > 25)
                power = 25;
            powerReadGet.setSelection(power);
            return true;
        }
        return false;
    }

    private boolean getWritePower() {
        int[] readWritePower = MyApp.getMyApp().getUhfMangerImpl().getReadWritePower();
        if (readWritePower == null) {
            return false;
        }
        int power = readWritePower[1];
        if (power < 0 || power > 30 + 3) {
            return false;
        }
        if (ifRMModule) {
            if (power >=10) {
                powerWriteGet.setSelection(power - 10);
                MLog.e("power = " + power);
                return true;
            }
        } else if (ifSupportR2000Fun) {
            if (power >= 5) {
                powerWriteGet.setSelection(power - 5);
                MLog.e("power = " + power);
                return true;
            }
        } else if (ifJ06) {
            powerWriteGet.setSelection(power);
            return true;
        } else {
            if (power > 25)
                power = 25;
            powerWriteGet.setSelection(power);
            return true;
        }
        return false;
    }


    private AlertDialog ad;
    //存放选中下标
    // Saving selected subscript
    private HashSet<Integer> hSet = new HashSet();
    //存放当前区域频率的所有频点
    // Saving of all frequencies in the current area
    private int[] getFrqRange = null;

    //设置需要的频点
    // set frequency range
    private void selectFrq() {
        getFrqRange = null;
        final int area_fq = MyApp.getMyApp().getUhfMangerImpl().frequencyModeGetNotFixedFreq();
        if (ifSupportR2000Fun) {
            switch (area_fq) {
                case 0:
                    getFrqRange = cn_840;
                    break;
                case 1:
                    getFrqRange = cn_920;
                    break;
                case 2:
                    getFrqRange = eu;
                    break;
                case 3:
                    getFrqRange = us;
                    break;
                default:
                    break;
            }
        } else {
            //兼容UM2,UM1等模块
            // Compatible with UM2, UM1 modules
            getFrqRange = area_fq == 1 ? cn_920_210 : us_210;
        }
        if (getFrqRange != null) {
            hSet.clear();
            ad = new AlertDialog.Builder(getContext()).create();
            ad.setTitle(R.string.selected_fq_rang);
            GridView gd = new GridView(getContext());
            gd.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            gd.setNumColumns(3);
            Integer getFrqRanges[] = new Integer[getFrqRange.length];
            for (int i = 0; i < getFrqRange.length; i++) {
                getFrqRanges[i] = getFrqRange[i];
            }
            ArrayAdapter adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_single_choice, getFrqRanges);
            gd.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    CheckedTextView cb = view.findViewById(android.R.id.text1);
                    if (cb.isChecked()) {
                        hSet.add(position);
                    } else {
                        hSet.remove(position);
                    }
                }
            });
            gd.setAdapter(adapter);
            ad.setView(gd);
            ad.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.determine), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //实际选中的频点
                    //Actually selected frequency point
                    int[] frqSel = new int[hSet.size()];
                    int i = 0;
                    for (Integer integer : hSet) {
                        frqSel[i] = getFrqRange[integer];
                        i++;
                    }
                    int ifsuccess = MyApp.getMyApp().getUhfMangerImpl().frequenceRange_Set(0, frqSel.length, frqSel,area_fq);
                    setResult_New(ifsuccess);
                }
            });
            ad.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ad.dismiss();
                }
            });

            ad.create();
            ad.show();
        }
    }

    private void setResult(boolean flag) {
        if (flag) {
            MUtil.show(R.string.success);
        } else {
            MUtil.show(R.string.failed);
        }
    }
    private void setResult_New(int flag) {
        if (flag ==1) {
            MUtil.show(R.string.success);
        } else {
            MUtil.show(R.string.failed);
        }
    }

}
