/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package a860014.mpf.ble_display;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import a860014.mpf.ble_display.util.CircleButton;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;

import static a860014.mpf.ble_display.SampleGattAttributes.COMMUNICATION;
import static a860014.mpf.ble_display.SampleGattAttributes.NOTIFY;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Toolbar toolbar;
    private TextView mbledata;
    private CircleButton cb_unlock;
    private CircleButton cb_lock;

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    private List<BluetoothGattService> mGattServices;

    // 資料讀取
    public final int BTbuf_array_num = 512;
    public byte[] BTbuf_array = new byte[BTbuf_array_num];
    int BTbuf_in = 0;
    int BTbuf_out = 0;
    int Motor_Data_in = 0;
    int ble_byte_count = 0;
    int[] Motor_CAN_Data = new int[14];
    int Int_buf_in_temp_Pre = 0;
    int Int_buf_in_temp_Now = 0;
    boolean Motor_Data_Start_Flag = false;
    int MotorCAN_ID = 0;

    //發送資料
    int[] unlock_BLE_Lock={0x3c,0x00,0x07,0x07,0x08,0xcd,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x3e};
    int[] lock_BLE_Lock={0x3c,0x00,0x07,0x07,0x08,0xce,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x3e};
    int[] read_BLE_Lock={0x3c,0x00,0x07,0x07,0x08,0xcf,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x3e};

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                if(!mConnected){
                    tintMenuIcon(DeviceControlActivity.this,toolbar.getMenu().getItem(0),R.color.colorAccent);
                }
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                if(mConnected){
                    tintMenuIcon(DeviceControlActivity.this,toolbar.getMenu().getItem(0),R.color.white);
                }
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                mGattServices = mBluetoothLeService.getSupportedGattServices();
                StartReceiver(true);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                final byte[] dataArr = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_RAW);
                final String datavalue = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                String datavalue_byte = Arrays.toString(dataArr);
                mbledata.setText(datavalue_byte);
                Log.i("Data-Value", datavalue_byte);
                if (dataArr != null) {
                    for (ble_byte_count = 0; ble_byte_count < dataArr.length; ble_byte_count++) {
                        BTbuf_array[BTbuf_in] = dataArr[ble_byte_count];
                        BTbuf_in++;
                        BTbuf_in = BTbuf_in & (BTbuf_array_num - 1);     //限制陣列的範圍
                    }
                }
                GreenFly_MODE(dataArr);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_control_activity);

        mbledata = (TextView) findViewById(R.id.bledata);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        cb_lock = (CircleButton)findViewById(R.id.cb_lock);
        cb_unlock = (CircleButton)findViewById(R.id.cb_unlock);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main);
        toolbar.setTitle(mDeviceName);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setElevation(R.drawable.toolbar_dropshadow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tintMenuIcon(DeviceControlActivity.this,toolbar.getMenu().getItem(0),R.color.colorAccent);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        cb_unlock.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                SendByte(unlock_BLE_Lock);
            }
        });

        cb_lock.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                SendByte(lock_BLE_Lock);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        } catch(IllegalArgumentException e) {
            Log.d(TAG, "Already_RegisterReceiver");
        }
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cb_unlock.RemoveAnimation();
        cb_lock.RemoveAnimation();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_ble:
                if(!mConnected){
                    mBluetoothLeService.connect(mDeviceAddress);
                }else{
                    mBluetoothLeService.disconnect();
                }
                Log.i("CCCCCCCCCCCCCCCC", "press" + "CCCCCCCCCCCCCCCC");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    //改變 MENU_ICON顏色
    public static void tintMenuIcon(Context context, MenuItem item, @ColorRes int color) {
        Drawable normalDrawable = item.getIcon();
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, context.getResources().getColor(color));

        item.setIcon(wrapDrawable);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void SendByte(int[] send) {
        BluetoothGattCharacteristic characteristic =null;
        for(int i=0; i<mGattServices.size();i++){
            if(mGattServices.get(i).getUuid().toString().equals(COMMUNICATION)){
                for(int j=0; j<mGattServices.get(i).getCharacteristics().size();j++){
                    if(mGattServices.get(i).getCharacteristics().get(j).getUuid().toString().equals(NOTIFY)){
                        characteristic=mGattServices.get(i).getCharacteristics().get(j);
                    }
                }
            }
        }

        final int charaProp = characteristic.getProperties();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
            mBluetoothLeService.writeOTABootLoaderCommand(characteristic, send);
            Log.d(TAG, "嘗試寫入");
        } else {
            Log.d(TAG, "SendByte寫入失敗final");
        }
    }


    private void StartReceiver(boolean on) {

        BluetoothGattCharacteristic characteristic =null;
        for(int i=0; i<mGattServices.size();i++){
            if(mGattServices.get(i).getUuid().toString().equals(COMMUNICATION)){
                for(int j=0; j<mGattServices.get(i).getCharacteristics().size();j++){
                    if(mGattServices.get(i).getCharacteristics().get(j).getUuid().toString().equals(NOTIFY)){
                        characteristic=mGattServices.get(i).getCharacteristics().get(j);
                    }
                }
            }
        }

        final int charaProp = characteristic.getProperties();
        final UUID uuid = characteristic.getUuid();
        Log.i("StartReceiver", "receiver uuid = " + uuid);
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

            if (on) {
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            } else {
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, false);
            }
        }

    }

    private void GreenFly_MODE(byte[] dataArr) {
        while (BTbuf_in != BTbuf_out) {
            Int_buf_in_temp_Now = BTbuf_array[BTbuf_out];
            if (Int_buf_in_temp_Now < 0) {
                Int_buf_in_temp_Now = Int_buf_in_temp_Now + 256;
            }
            BTbuf_out++;
            BTbuf_out = BTbuf_out & (BTbuf_array_num - 1);
            if (!Motor_Data_Start_Flag) {
                if (Int_buf_in_temp_Now == '<' && Int_buf_in_temp_Pre == '>') {
                    Motor_Data_in = 0;
                    Motor_Data_Start_Flag = true;
                }
            } else {
                if (Motor_Data_in >= 14) {
                    Motor_Data_Start_Flag = false;
                    Motor_Data_in = 0;
                }
                Motor_CAN_Data[Motor_Data_in] = Int_buf_in_temp_Now;
                Motor_Data_in++;
                if (Motor_Data_in == 13) {
                    if (Int_buf_in_temp_Now == '>') {
                        GO_GreenFly_CMD();
                        Motor_Data_Start_Flag = false;
                        Motor_Data_in = 0;
                    }
                }
            }
            Int_buf_in_temp_Pre = Int_buf_in_temp_Now;
        }
    }//void GreenFly_MODE

    private void GO_GreenFly_CMD() {

        MotorCAN_ID = Motor_CAN_Data[1] * 256 + Motor_CAN_Data[2];
        switch (MotorCAN_ID) {
            case 101:
                break;
            case 102:
                break;
            case 201:
                break;
            case 202:
                break;
            case 301:
                break;
            case 302:
                break;
            case 401:
                break;
            case 402:
                break;
            case 501:
                break;
            case 502:
                break;
            case 702:
                break;
            case 708:
                break;
            case 801:
                break;
            case 802:
                break;
        }
    }
}
