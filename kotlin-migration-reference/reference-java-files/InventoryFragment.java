package com.uhf.uhfdemo;

import static com.uhf.uhfdemo.MyApp.ifRMModule;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.uhf.base.UHFManager;
import com.uhf.base.UHFModuleType;
import com.uhf.event.BackResult;
import com.uhf.event.BaseFragment;
import com.uhf.event.GetRFIDThread;
import com.uhf.util.DataConversionUtils;
import com.uhf.util.InventoryAdapter;
import com.uhf.util.MLog;
import com.uhf.util.MUtil;
import com.uhf.util.ThreadUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InventoryFragment extends BaseFragment implements View.OnClickListener, BackResult {

    private ListView mListView;
    private Button mInventoryBtn,mClearBtn;
    private TextView mTagNumbersText,mReadNumbersText,mUseTimesText;
    private String tagNumber, readNumber, takeTime;
    private InventoryAdapter mInventoryAdapter;

    private long startTime, usTim, pauseTime;

    private Map<String, Integer> realDataMap = new HashMap<>();

    private List<String> typeList = new ArrayList<>();
    //tid info
    private List<String> tidList = new ArrayList<>();
    //usr info
    private List<String> epcList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();
    }

    private void initView(View view) {
        mInventoryBtn = view.findViewById(R.id.btn_inventory_all);
        mClearBtn = view.findViewById(R.id.btn_clear_all);
        mListView = view.findViewById(R.id.tag_info_listview);
        mTagNumbersText = view.findViewById(R.id.tagNumbers_all);
        mReadNumbersText = view.findViewById(R.id.readNumbers_all);
        mUseTimesText = view.findViewById(R.id.useTimes_all);

        mInventoryBtn.setOnClickListener(this);
        mClearBtn.setOnClickListener(this);
    }

    private void initData() {
        tagNumber = getString(R.string.tag_number) + ":";
        readNumber = getString(R.string.read_number) + ":";
        takeTime = getString(R.string.user_time) + ":";

        mInventoryAdapter = new InventoryAdapter(getActivity(),realDataMap,typeList,tidList,epcList);
        mListView.setAdapter(mInventoryAdapter);

        mInventoryBtn.setText(GetRFIDThread.getInstance().isIfPostMsg() ? R.string.stop_rfid : R.string.read_rfid);

        GetRFIDThread.getInstance().setBackResult(this);

        clearData();
    }

    private void startOrStopReadRFID() {
        boolean flag = !GetRFIDThread.getInstance().isIfPostMsg();
        if (flag) {
            MyApp.getMyApp().getUhfMangerImpl().inventoryISO6BAnd6CTag(false,0,0);
            long tempTime = pauseTime;
            startTime = System.currentTimeMillis() - tempTime;
            ifSoundThreadAlive = true;
            playSound();
        } else {
            ifHaveTag = false;
            ifSoundThreadAlive = false;
            MyApp.getMyApp().getUhfMangerImpl().stopInventory();
        }
        GetRFIDThread.getInstance().setIfPostMsg(flag);
        mInventoryBtn.setText(flag ? R.string.stop_rfid : R.string.read_rfid);
    }

    private void clearData() {
        dataMap.clear();
        realDataMap.clear();
        tidList.clear();
        epcList.clear();
        typeList.clear();
        usTim = pauseTime = 0;
        mUseTimesText.setText(takeTime);
        mTagNumbersText.setText(tagNumber);
        mReadNumbersText.setText(readNumber);
        mInventoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_inventory_all:
                startOrStopReadRFID();
                break;
            case R.id.btn_clear_all:
                if (GetRFIDThread.getInstance().isIfPostMsg()) {
                    MUtil.show(R.string.notice_clean_data);
                } else {
                    clearData();
                }
                break;
        }
    }

    //识别的标签数据
    //The label's info what has been identified
    private Map<String, Integer> dataMap = new HashMap<>();

    @Override
    public void postResult(String[] tagData) {
        postDataTime = SystemClock.elapsedRealtime();
        ifHaveTag = true;
        String type = tagData[0];
        String tid = tagData[1];
        String epc = tagData[2];
        Integer number = dataMap.get(tid);
        if (number==null) {
            dataMap.put(tid, 1);
            updateUI(type, tid, epc,1);
        }else {
            dataMap.put(tid, number+1);
            updateUI(type, tid, epc,number+1);
        }

    }

    @Override
    public void postInventoryRate(long rate) {
        if (getActivity() == null)
            return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 张数/秒
                // tags per second
                mReadNumbersText.setText(rate + readNumber);
            }
        });
    }

    @Override
    public void onKeyDown(int keyCode, KeyEvent event) {
        MLog.e("keyCode =" + keyCode);
        //把枪按钮被按下,默认值为F8,F4,BTN4
        //The trigger button is pressed, and the default value is F8,F4,BTN4
        if (keyCode == KeyEvent.KEYCODE_F8 || keyCode == KeyEvent.KEYCODE_F4 || keyCode == KeyEvent.KEYCODE_BUTTON_4 || keyCode == KeyEvent.KEYCODE_PROG_RED || keyCode == KeyEvent.KEYCODE_BUTTON_3/*|| keyCode == KeyEvent.KEYCODE_BUTTON_1|| keyCode ==KeyEvent.KEYCODE_F9 || keyCode == KeyEvent.KEYCODE_F10*/) {
            startOrStopReadRFID();
        }
    }

    private void updateUI(String type,String tid,String epc,int num) {
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (num > 1) {
                    realDataMap.put(tid, num);
                } else {
                    realDataMap.put(tid, 1);
                    tidList.add(tid);
                    typeList.add(type);
                    epcList.add(epc);

                }
                long endTime = System.currentTimeMillis();
                //盘点标签开始到结束的获取时间
                //Acquisition time from start to end of inventorying tag
                pauseTime = usTim = endTime - startTime;
                //花费的时间
                //Time spent
                mUseTimesText.setText(takeTime + usTim);
                //标签数量
                //Number of tags
                mTagNumbersText.setText(tagNumber + realDataMap.size());
                mInventoryAdapter.notifyDataSetChanged();
            }
        });
    }

    private boolean ifSoundThreadAlive = true;
    private boolean ifHaveTag = false;
    private long lastTime;
    private long postDataTime = 0;

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
}
