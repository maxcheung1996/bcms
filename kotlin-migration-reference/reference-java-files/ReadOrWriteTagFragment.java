package com.uhf.setting;

import android.annotation.SuppressLint;
import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
import android.os.SystemClock;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;
import com.uhf.event.BackResult;
import com.uhf.event.BaseFragment;
import com.uhf.event.GetRFIDThread;
import com.uhf.uhfdemo.MyApp;
import com.uhf.uhfdemo.R;
import com.uhf.util.LockDataAdapter;
import com.uhf.util.MLog;
import com.uhf.util.MUtil;
import com.uhf.util.MyListView;
import com.uhf.util.ThreadUtil;

import static android.text.TextUtils.isEmpty;

import static com.uhf.uhfdemo.MyApp.ifRMModule;
import static com.uhf.uhfdemo.MyApp.ifSupportR2000Fun;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReadOrWriteTagFragment extends BaseFragment implements View.OnClickListener, BackResult, AdapterView.OnItemClickListener {

    private Spinner spFilterMbWr, spMbWr, spGBArea;
    private EditText EtFilterAdsTagWr, EtFilterlenTagWr, EtFilterDataTagWr, EtAdsTagWr, EtLenTagWr, EtDataTagWr, EtPwdTagWr;
    private EditText EtSelectData, EtAddressGB, EtLenGB, EtDataGB, EtPassWordGB;
    private CheckBox CBFilterWr,CBFilterWrGB;
    private String[] spirw = {"RFU", "EPC", "TID", "USR"};
    Boolean threadAlive = true;
    private LockDataAdapter mListAdapter;
    private MyListView mListView;
    private Button BtRead,openLED;
    private View tops, bottoms,gbReadWrite;
    private View layout_6b;
    private EditText Et_set_ads_6B,Et_Set_len_6B,et_match_tid,et_write_start_6b,et_write_match_tid_6b,et_write_data_6b,et_read_data_6b;
    private Spinner spinner_6b;
    private Button bt_read_6b,bt_write_6b;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_write_tag, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    @SuppressLint("SetTextI18n")
    private void initView(View v) {

        tops = v.findViewById(R.id.wTop);
        bottoms = v.findViewById(R.id.wBottom);
        //过滤部分
        // Filter
        spFilterMbWr = tops.findViewById(R.id.spinner_MB);
        EtFilterAdsTagWr = tops.findViewById(R.id.Et_set_ads);
        EtFilterlenTagWr = tops.findViewById(R.id.Et_Set_len);
        EtFilterDataTagWr = tops.findViewById(R.id.Et_Set_data);

        spFilterMbWr.setSelection(0);
        CBFilterWr = v.findViewById(R.id.CB_FlWr);

        //标签过滤操作，EPC开始位置（Bit）
        // Tag filtering, EPC start position (Bit)
        EtFilterAdsTagWr.setText("32");
        //标签过滤操作，EPC长度(Bit)
        // Tag filtering, EPC length (Bit)
        EtFilterlenTagWr.setText("96");
        //标签过滤操作，ECP数据（16进制）
        // Tag filtering, ECP data (hexadecimal)
        EtFilterDataTagWr.setText("1234567890ABCDEF12345678");


        //读写部分
        // Reading and writing
        LinearLayout acs_pswd = bottoms.findViewById(R.id.acs_pswd);
        //显示隐藏的acces password的布局
        // Show the layout of the access password
        acs_pswd.setVisibility(View.VISIBLE);

        spMbWr = bottoms.findViewById(R.id.spinner_MB);
        ArrayAdapter<String> arradp_rw = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, spirw);
        arradp_rw.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMbWr.setAdapter(arradp_rw);

        //设置选中EPC
        // Set selected EPC
        spMbWr.setSelection(1);

        EtAdsTagWr = bottoms.findViewById(R.id.Et_set_ads);
        EtLenTagWr = bottoms.findViewById(R.id.Et_Set_len);
        EtPwdTagWr = bottoms.findViewById(R.id.Et_AcPwdRead);
        EtDataTagWr = bottoms.findViewById(R.id.Et_Set_data);

        v.findViewById(R.id.Bt_Wr).setOnClickListener(this);
        v.findViewById(R.id.Bt_Rd).setOnClickListener(this);

        //读写EPC操作，密码（16进制）
        // Reading and writing the EPC, Password (hexadecimal)
        EtPwdTagWr.setText("00000000");
        //读写EPC操作，EPC开始位置(WORD类型)
        // Reading and writing the EPC, EPC start position (WORD type)
        EtAdsTagWr.setText("2");
        //读写EPC操作，EPC长度(WORD类型)
        // Reading and writing the EPC, EPC length (WORD type)
        EtLenTagWr.setText("6");
        // 读写操作，ECP数据（16进制）
        // Reading and writing the EPC, ECP data (hexadecimal)
        EtDataTagWr.setText("1234567890ABCDEF12345678");

        //此功能为让带LED的标签亮灭灯
        openLED = v.findViewById(R.id.bt_led);
//        if (MyApp.getMyApp().getUhfMangerImpl().ifJ06() || !ifSupportR2000Fun || MyApp.if5100Module || UHFModuleType.SLR_MODULE == UHFManager.getType() || UHFModuleType.RM_MODULE == UHFManager.getType()) {
//            openLED.setVisibility(View.GONE);
//        }
        openLED.setOnClickListener(this);

        //定制部分，用于T2X UM2，在此页面做盘点标签（盘点EPC）的功能，且点击itme会将数据显示在读写的数据输入框上
        BtRead = v.findViewById(R.id.read_RFID);
        v.findViewById(R.id.read_RFID).setOnClickListener(this);
        v.findViewById(R.id.clear_Data).setOnClickListener(this);
        mListView = v.findViewById(R.id.specific_Msg);
        mListAdapter = new LockDataAdapter(getActivity(),realKeyList);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(this);
        if(!MyApp.ifLockTagRead) {
            v.findViewById(R.id.read_epc).setVisibility(View.GONE);
        }else {
            v.findViewById(R.id.read_epc).setVisibility(View.VISIBLE);
        }
        GetRFIDThread.getInstance().setBackResult(this);
        dataMap.clear();
        realDataMap.clear();
        realKeyList.clear();

        //国芯模块6B标签部分
        layout_6b = v.findViewById(R.id.layout_6b);
        if (UHFModuleType.GX_MODULE == UHFManager.getType() && MyApp.protocolType == 3) {
            layout_6b.setVisibility(View.VISIBLE);
            tops.setVisibility(View.GONE);
            bottoms.setVisibility(View.GONE);
            CBFilterWr.setVisibility(View.GONE);
            v.findViewById(R.id.Bt_Rd).setVisibility(View.GONE);
            v.findViewById(R.id.Bt_Wr).setVisibility(View.GONE);
            openLED.setVisibility(View.GONE);
        }else {
            layout_6b.setVisibility(View.GONE);
            tops.setVisibility(View.VISIBLE);
            bottoms.setVisibility(View.VISIBLE);
            CBFilterWr.setVisibility(View.VISIBLE);
            v.findViewById(R.id.Bt_Rd).setVisibility(View.VISIBLE);
            v.findViewById(R.id.Bt_Wr).setVisibility(View.VISIBLE);
            openLED.setVisibility(View.VISIBLE);
        }
        Et_set_ads_6B = layout_6b.findViewById(R.id.Et_set_ads_6B);
        Et_Set_len_6B = layout_6b.findViewById(R.id.Et_Set_len_6B);
        et_match_tid = layout_6b.findViewById(R.id.et_match_tid);
        et_write_start_6b = layout_6b.findViewById(R.id.et_write_start_6b);
        et_write_match_tid_6b = layout_6b.findViewById(R.id.et_write_match_tid_6b);
        et_write_data_6b = layout_6b.findViewById(R.id.et_write_data_6b);
        et_read_data_6b = layout_6b.findViewById(R.id.et_read_data_6b);
        spinner_6b = layout_6b.findViewById(R.id.spinner_6b);
        bt_read_6b = layout_6b.findViewById(R.id.bt_read_6b);
        bt_write_6b = layout_6b.findViewById(R.id.bt_write_6b);
        bt_read_6b.setOnClickListener(this);
        bt_write_6b.setOnClickListener(this);

        //国标部分
//        gbReadWrite = v.findViewById(R.id.gb);
//        CBFilterWrGB = gbReadWrite.findViewById(R.id.cb_select_gb);
//        EtSelectData = gbReadWrite.findViewById(R.id.et_select_data);
//        EtAddressGB = gbReadWrite.findViewById(R.id.et_address);
//        EtLenGB = gbReadWrite.findViewById(R.id.et_len);
//        EtDataGB = gbReadWrite.findViewById(R.id.et_data);
//        EtPassWordGB = gbReadWrite.findViewById(R.id.et_pwd);
//        spGBArea = gbReadWrite.findViewById(R.id.sp_area);
//        gbReadWrite.findViewById(R.id.bt_read_gb).setOnClickListener(this);
//        gbReadWrite.findViewById(R.id.bt_write_gb).setOnClickListener(this);
//
//        EtDataGB.setText("1234567890ABCDEF12345678");
//        EtSelectData.setText("1234567890ABCDEF12345678");
//        EtAddressGB.setText("2");
//        EtLenGB.setText("6");
//        EtPassWordGB.setText("00000000");

        initViewGB();
    }

    private void initViewGB() {
        if(MyApp.protocolType == 2) {
            ArrayAdapter<String> arradp_rw = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.gb_select_area));
            arradp_rw.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spFilterMbWr.setAdapter(arradp_rw);

            ArrayAdapter<String> gb_rw = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.gb_area));
            gb_rw.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spMbWr.setAdapter(gb_rw);

            spMbWr.setSelection(1);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_led:
                if (GetRFIDThread.getInstance().getLockPostTag()) {
                    MUtil.show(R.string.notice_clean_data);
                } else {
                    if (openLED.getTag().equals("true")) {
                        threadAlive = true;
                        openLED.setTag("false");
                        openLED.setText(R.string.close_led);
                        ThreadUtil.getInstance().getExService().execute(new Runnable() {
                            @Override
                            public void run() {
                                while (threadAlive) {
                                    String flag = MyApp.getMyApp().getUhfMangerImpl().readTag("00000000", 0, 0, 0, "0", 0, 4, 1);
                                    SystemClock.sleep(200);
                                }
                            }
                        });
                    } else {
                        threadAlive = false;
                        openLED.setTag("true");
                        openLED.setText(R.string.open_led);
                    }
                }
                break;
            case R.id.read_RFID:
                startOrStopRFID();
                break;
            case R.id.clear_Data:
                if (GetRFIDThread.getInstance().getLockPostTag()) {
                    MUtil.show(R.string.notice_clean_data);
                } else {
                    clearData();
                }
                break;
            case R.id.bt_read_gb:
                if (!ifNotNullGB()) {
                    MUtil.show(R.string.data_notnull);
                    return;
                }else {
                    boolean ifCheck = CBFilterWrGB.isChecked();
                    String filterData = EtSelectData.getText().toString();
                    int readArea = spGBArea.getSelectedItemPosition();
                    int readAddress = Integer.parseInt(EtAddressGB.getText().toString());
                    int readLen = Integer.parseInt(EtLenGB.getText().toString());
                    String readPassWord = EtPassWordGB.getText().toString();
                    String resultStr = null;
                    if (ifCheck) {
                        resultStr = MyApp.getMyApp().getUhfMangerImpl().readGBTag(readPassWord, 0, 0, 0, filterData, readArea, readAddress, readLen);
                    } else {
                        resultStr = MyApp.getMyApp().getUhfMangerImpl().readGBTag(readPassWord, 0, 0, 0, "0", readArea, readAddress, readLen);
                    }
                    String show = resultStr == null ? getString(R.string.read_failed) : getString(R.string.read_success);
                    MUtil.show(show);
                    if (!TextUtils.isEmpty(resultStr)) {
                        EtDataGB.setText(resultStr);
                    }
                }
                break;
            case R.id.bt_write_gb:
                if (!ifNotNullGB()) {
                    MUtil.show(R.string.data_notnull);
                    return;
                }else {
                    boolean ifCheck = CBFilterWrGB.isChecked();
                    String filterData = EtSelectData.getText().toString();
                    int readArea = spGBArea.getSelectedItemPosition();
                    int readAddress = Integer.parseInt(EtAddressGB.getText().toString());
                    int readLen = Integer.parseInt(EtLenGB.getText().toString());
                    String readPassWord = EtPassWordGB.getText().toString();
                    String readData = EtDataGB.getText().toString();
                    boolean status = false;
                    if (ifCheck) {
                        status = MyApp.getMyApp().getUhfMangerImpl().writeGBTag(readPassWord, 0, 0, 0, filterData, readArea, readAddress, readLen,readData);
                    } else {
                        status = MyApp.getMyApp().getUhfMangerImpl().writeGBTag(readPassWord, 0, 0, 0, "0", readArea, readAddress, readLen,readData);
                    }
                    String show = status == false ? getString(R.string.write_failed) : getString(R.string.write_success);
                    MUtil.show(show);
                }
                break;
            case R.id.bt_read_6b:
            {
                int area = spinner_6b.getSelectedItemPosition();
                if (TextUtils.isEmpty(Et_set_ads_6B.getText()) || TextUtils.isEmpty(Et_Set_len_6B.getText())) {
                    MUtil.show(getString(R.string.read_failed));
                    return;
                }
                String matchTid = null;
                if (!TextUtils.isEmpty(et_match_tid.getText())) {
                    matchTid = et_match_tid.getText().toString();
                }
                String code = MyApp.getMyApp().getUhfMangerImpl().readTag6B(area == 0?0:2,Integer.parseInt(Et_set_ads_6B.getText().toString()),Integer.parseInt(Et_Set_len_6B.getText().toString()),matchTid);
                if (TextUtils.isEmpty(code)) {
                    MUtil.show(getString(R.string.read_failed));
                }else {
                    et_read_data_6b.setText(code);
                    MUtil.show(getString(R.string.read_success));
                }

            }
                break;
            case R.id.bt_write_6b:{
                if (TextUtils.isEmpty(et_write_start_6b.getText()) || TextUtils.isEmpty(et_write_match_tid_6b.getText()) || TextUtils.isEmpty(et_write_data_6b.getText())) {
                    MUtil.show(getString(R.string.write_failed));
                    return;
                }
                boolean isSuc = MyApp.getMyApp().getUhfMangerImpl().writeTag6B(Integer.parseInt(et_write_start_6b.getText().toString()),et_write_match_tid_6b.getText().toString(),et_write_data_6b.getText().toString());
                MUtil.show(isSuc ? getString(R.string.write_success) : getString(R.string.write_failed));
            }

                break;
            default:
                if (GetRFIDThread.getInstance().getLockPostTag()) {
                    MUtil.show(R.string.notice_clean_data);
                } else {
                    if (!ifNotNull()) {
                        MUtil.show(R.string.data_notnull);
                        return;
                    }
                    //true 则点击的是写入按钮，false 则是读取按钮
                    // true is the write button, false is the read button
                    boolean ifWrite = (v.getId() == R.id.Bt_Wr);
                    boolean status = false;
                    String result = null;
                    boolean ifChecked = CBFilterWr.isChecked();
                    int Flindex = spFilterMbWr.getSelectedItemPosition() + 1;
                    String PwdWr = EtPwdTagWr.getText().toString();
                    int Flads = Integer.valueOf(EtFilterAdsTagWr.getText().toString());
                    int Fllen = Integer.valueOf(EtFilterlenTagWr.getText().toString());
                    String Fldata = EtFilterDataTagWr.getText().toString();
                    int MbWr = spMbWr.getSelectedItemPosition();
                    int ads = Integer.valueOf(EtAdsTagWr.getText().toString());
                    int len = Integer.valueOf(EtLenTagWr.getText().toString());
                    String data = EtDataTagWr.getText().toString();
//                    Fldata = convertStringToHexString(Fldata);
//                    data = convertStringToHexString(data);

                    //启用过滤
                    // Enable filtering
                    if (ifChecked) {
                        //写操作
                        // Writing
                        if (ifWrite) {
                            if (MyApp.protocolType == 1) {
                                status = MyApp.getMyApp().getUhfMangerImpl().writeTag(PwdWr, Flindex, Flads, Fllen, Fldata, MbWr, ads, len, data);
                            }else {
                                status = MyApp.getMyApp().getUhfMangerImpl().writeGBTag(PwdWr, Flindex, Flads, Fllen, Fldata, MbWr, ads, len, data);
                            }
                        } else {
                            if (MyApp.protocolType == 1) {
                                result = MyApp.getMyApp().getUhfMangerImpl().readTag(PwdWr, Flindex, Flads, Fllen, Fldata, MbWr, ads, len);
                            }else {
                                result = MyApp.getMyApp().getUhfMangerImpl().readGBTag(PwdWr, Flindex, Flads, Fllen, Fldata, MbWr, ads, len);
                            }
                        }
                    } else {
                        //不启用过滤
                        // Disable filtering
                        if (ifWrite) {
                            if (MyApp.protocolType == 1) {
                                if (1 == MbWr) {
                                    status = MyApp.getMyApp().getUhfMangerImpl().writeDataToEpc(PwdWr, ads, len, data);
                                } else {
                                    status = MyApp.getMyApp().getUhfMangerImpl().writeTag(PwdWr, 0, 0, 0, "0", MbWr, ads, len, data);
                                }
                            }else
                                status = MyApp.getMyApp().getUhfMangerImpl().writeGBTag(PwdWr, 0, 0, 0, "0", MbWr, ads, len, data);
                        } else {
                            if (MyApp.protocolType == 1) {
                                result = MyApp.getMyApp().getUhfMangerImpl().readTag(PwdWr, 0, 0, 0, "0", MbWr, ads, len);
                            }else {
                                result = MyApp.getMyApp().getUhfMangerImpl().readGBTag(PwdWr, 0, 0, 0, "0", MbWr, ads, len);
                            }
                        }

                    }
                    String readSuccess = getString(R.string.read_success);
                    String showText = ifWrite ? (status ? getString(R.string.write_success) : getString(R.string.write_failed)) : (result == null ? getString(R.string.read_failed) : readSuccess);
                    MUtil.show(showText);
                    if (showText.equals(readSuccess)) {
                        //显示读取成功的数据
                        // Shows data read successfully
//                        if (convertHexToString(result).contains(";")) {
//                            EtDataTagWr.setText(convertHexToString(result).split(";")[0]);
//                        }else
                            EtDataTagWr.setText(result);
                    }
                }
                break;
        }

    }

    private boolean ifNotNull() {
        return !isEmpty(EtFilterAdsTagWr.getText()) && !isEmpty(EtFilterlenTagWr.getText()) && !isEmpty(EtFilterDataTagWr.getText()) &&
                !isEmpty(EtAdsTagWr.getText()) && !isEmpty(EtLenTagWr.getText()) && !isEmpty(EtDataTagWr.getText()) && !isEmpty(EtPwdTagWr.getText());
    }
    private boolean ifNotNullGB() {
        return !isEmpty(EtSelectData.getText()) && !isEmpty(EtAddressGB.getText()) && !isEmpty(EtLenGB.getText()) &&
                !isEmpty(EtPassWordGB.getText()) && !isEmpty(EtDataGB.getText());
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String Data = mListView.getItemAtPosition(i).toString();
        MLog.e("Data = " + Data);
        EtDataTagWr.setText(Data);
    }

    private void startOrStopRFID() {

        MyApp.currentInvtDataType = 0;
        boolean flag = !GetRFIDThread.getInstance().getLockPostTag();
        if (flag) {
            if (UHFModuleType.SLR_MODULE == UHFManager.getType() && MyApp.if5100Module ){
                MyApp.getMyApp().getUhfMangerImpl().slrInventoryModeSet(0);
            }
            Boolean i = MyApp.getMyApp().getUhfMangerImpl().startInventoryTag();
        } else {
            MyApp.getMyApp().getUhfMangerImpl().stopInventory();
            //GetRFIDThread.getInstance().destoryThread();
        }
        GetRFIDThread.getInstance().setLockPostTag(flag);
        BtRead.setText(flag ? R.string.stop_rfid : R.string.read);
    }

    private void clearData() {
        dataMap.clear();
        realDataMap.clear();
        realKeyList.clear();
        mListAdapter.notifyDataSetChanged();
    }

    private Map<String, Integer> dataMap = new HashMap<>(); //数据

    @Override
    public void postResult(String[] tagData) {
        String tid = tagData[0];//获取TID
        String epc = tagData[1]; //拿到EPC
        MLog.e("tid = " + tid + " epc = " + epc);
        //showDialog(epc);
        String filterData = epc ;
        Integer number = dataMap.get(/*epc*/filterData);//如果已存在，就拿到数量
        if (number == null) {
            dataMap.put(/*epc*/filterData, 1);
            updateUI(epc, 1);
        }
    }

    @Override
    public void postInventoryRate(long rate) {

    }


    private Map<String, Integer> realDataMap = new HashMap<>();
    // realDataMap的key
    // realDataMap's key
    private List<String> realKeyList = new ArrayList<>();
    private void updateUI(final String epc, final int num) {
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(num == 1){
                    String filterData = epc;
                    realKeyList.add(/*epc*/filterData);
                    mListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onKeyDown(int keyCode, KeyEvent event) {

    }

    public void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() *
                (listAdapter.getCount() - 1));
        params.height += 5;// if without this statement,the listview will be a
// little short
        listView.setLayoutParams(params);
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

    //将ASCII码字符串转为16进制
    public String convertStringToHexString(String str) {
//        String hexString = "0123456789abcdef";
//
//        if (str == null) {
//            return "";
//        }
//        byte[] bytes = str.getBytes();
//        StringBuilder sb = new StringBuilder(bytes.length * 2);
//        //将字节数组中每个字节拆解成2位16进制整数
//        for (int i = 0; i < bytes.length; i++) {
//            sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
//            sb.append(hexString.charAt((bytes[i] & 0x0f)));
//        }
//        return sb.toString().toUpperCase();
        char[] chars = str.toCharArray();
        StringBuffer hex = new StringBuffer();
        for(int i = 0; i < chars.length; i++){
            hex.append(Integer.toHexString((int)chars[i]));
        }
        return hex.toString();

    }

}

