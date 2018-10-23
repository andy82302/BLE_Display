package a860014.mpf.ble_display;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.king.view.radarview.RadarView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import a860014.mpf.ble_display.EventBus.MessageEvent;
import a860014.mpf.ble_display.EventBus.MyEventBus;
import a860014.mpf.ble_display.util.GpsUtil;
import a860014.mpf.ble_display.util.util_permission;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import ru.katso.livebutton.LiveButton;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getName();

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning = false;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_LOCATION = 2;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private LiveButton mLiveButton;
    private RadarView rv;
    private EditText mEditBLEname;

    boolean switchcheck = false;
    String bleName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        rv = (RadarView)findViewById(R.id.rv);
        rv.setCircleColor(Color.rgb(19, 157, 227));
        mLiveButton = (LiveButton) findViewById(R.id.LiveButton);
        mEditBLEname = (EditText) findViewById(R.id.EditBLEname);
        mLiveButton.setText(R.string.Scan_start);
        mLiveButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                bleName = mEditBLEname.getText().toString();
                switchcheck = true;
                MainActivityPermissionsDispatcher.LocationPermissionWithPermissionCheck(MainActivity.this);
            }
        });
        BLE_Sup_Check();
        MainActivityPermissionsDispatcher.LocationPermissionWithPermissionCheck(MainActivity.this);
        //推播EventBus註冊
        MyEventBus.eventBus.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        BLE_Sup_Check();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //推播EventBus註銷
        MyEventBus.eventBus.unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("請開啟藍芽才能搜尋藍芽，是否要開啟?")
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent enableLocation = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(enableLocation, REQUEST_ENABLE_LOCATION);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                       }
                   })
                    .create()
                    .show();
            return;
        }else if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            if(!GpsUtil.isOPen(this)) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("藍芽需開啟定位功能才能搜尋 " + "\r\n" + "請開啟「定位」")
                        .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent enableLocation = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(enableLocation, REQUEST_ENABLE_LOCATION);
                            }
                        })
                        .create()
                        .show();
            }else{
                if(switchcheck) {
                    scanLeDevice(true);
                }
            }
        }else if(requestCode == REQUEST_ENABLE_LOCATION && !GpsUtil.isOPen(this)){
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("藍芽需開啟定位功能才能搜尋 " + "\r\n" + "請開啟「定位」")
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent enableLocation = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(enableLocation, REQUEST_ENABLE_LOCATION);
                        }
                    })
                    .create()
                    .show();
        }else{
            if(switchcheck) {
                scanLeDevice(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Eventbus 接收處
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Log.v(TAG, "GET event: "+event);
    };

    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void LocationPermission() {
       if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
       }else if(!GpsUtil.isOPen(this)){
           new AlertDialog.Builder(MainActivity.this)
                   .setMessage("藍芽需開啟定位功能才能搜尋 "+"\r\n"+"請開啟「定位」")
                   .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           Intent enableLocation = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                           startActivityForResult(enableLocation, REQUEST_ENABLE_LOCATION);
                       }
                   })
//                   .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                       @Override
//                       public void onClick(DialogInterface dialog, int which) {
//                       }
//                   })
                   .create()
                   .show();
       }else if(switchcheck){

           if(!mScanning){
               mLiveButton.setText(R.string.Scan_stop);
               scanLeDevice(true); }
           else{
               mLiveButton.setText(R.string.Scan_start);
               scanLeDevice(false);
           }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
        if(grantResults[0]==-1) {
            if (!mBluetoothAdapter.isEnabled()) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    }

    @OnShowRationale({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void OnShowRationale(final PermissionRequest request) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("未允許「" + getString(R.string.app_name) + "」打開位置權限，將使「" + getString(R.string.app_name) + "」無法正常運作，是否重新設定權限？")
                .setPositiveButton("重新設定權限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .create()
                .show();
    }

    @OnPermissionDenied({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void OnPermissionDenied() {
    }

    @OnNeverAskAgain({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void OnNeverAskAgain() {
        util_permission.showDialog(this, R.string.Request_GPS, R.string.App_Setting, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                util_permission.openAppSetting(MainActivity.this);
            }
        });
    }

    private void BLE_Sup_Check(){
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert mBluetoothManager != null;
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if(mBluetoothAdapter ==  null){
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;

                   rv.stop();
                   rv.setVisibility(View.INVISIBLE);

                    mLiveButton.setText(R.string.Scan_start);
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            rv.start();
            rv.setVisibility(View.VISIBLE);

            mLiveButton.setText(R.string.Scan_stop);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mHandler.removeCallbacksAndMessages(null);
            mScanning = false;

            rv.stop();
            rv.setVisibility(View.INVISIBLE);

            mLiveButton.setText(R.string.Scan_start);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String aaa = device.getName();
                            if(device.getName()!=null) {
                                if (device.getName().equals(bleName)) {

                                    scanLeDevice(false);

                                    final Intent intent = new Intent(MainActivity.this,DeviceControlActivity.class);
                                    intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                                    intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

                                    startActivity(intent);

                                }
                            }
                        }
                    });
                }
            };

}
