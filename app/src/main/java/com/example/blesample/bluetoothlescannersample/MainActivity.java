package com.example.blesample.bluetoothlescannersample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getName();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;

    private BluetoothGatt bluetoothGatt;

    /**
     * 検索機器の機器名
     * TODO set device name
     */
    private final String DEVICE_NAME = "xxxxxxxxx";
    /**
     * 対象のサービスUUID
     * TODO set service uuid
     */
    private static final String DEVICE_BUTTON_SENSOR_SERVICE_UUID = "xxxxxxx0-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    /**
     * 対象のキャラクタリスティックUUID
     * TODO set characteristic uuid
     */
    private static final String DEVICE_BUTTON_SENSOR_CHARACTERISTIC_UUID = "xxxxxxx1-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
    /**
     * キャラクタリスティック設定UUID
     * TODO set config uuid
     */
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "xxxxxxx2-xxxx-xxxx-xxxx-xxxxxxxxxxxx";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 権限がない場合はリクエスト
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Bluetoothサポート有無のチェック
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth未対応機種", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        scanCallback = initCallbacks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    private void startScan() {
        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        }
        bluetoothLeScanner.stopScan(scanCallback);
        bluetoothLeScanner.startScan(scanCallback);
        // TODO setStatus(BleStatus.SCANNING);
    }

    private void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
            // TODO setStatus(BleStatus.CLOSED);
        }
    }


    private ScanCallback initCallbacks() {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                // デバイス情報取得
                Log.d("==========", result.getDevice() + "::" + result.getRssi());
                if (result != null && result.getDevice() != null) {
                    // TODO 処理を実装
                    if (DEVICE_NAME.equals(result.getDevice().getName())) {
                        // TODO setStatus(BleStatus.DEVICE_FOUND);

                        // 省電力のためスキャンを停止
                        bluetoothLeScanner.stopScan(scanCallback);

                        // GATT接続を試みる
                        bluetoothGatt = result.getDevice().connectGatt(MainActivity.this, false, bluetoothGattCallback);
                    }
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };
    }


    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: " + status + " -> " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // GATTへ接続成功
                // サービスを検索する
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // GATT通信から切断された
                // TODO setStatus(BleStatus.DISCONNECTED);
                bluetoothGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(DEVICE_BUTTON_SENSOR_SERVICE_UUID));
                if (service == null) {
                    // サービスが見つからなかった
                    // TODO setStatus(BleStatus.SERVICE_NOT_FOUND);
                } else {
                    // サービスを見つけた
                    // TODO setStatus(BleStatus.SERVICE_FOUND);

                    BluetoothGattCharacteristic characteristic =
                            service.getCharacteristic(UUID.fromString(DEVICE_BUTTON_SENSOR_CHARACTERISTIC_UUID));

                    if (characteristic == null) {
                        // キャラクタリスティックが見つからなかった
                        // TODO setStatus(BleStatus.CHARACTERISTIC_NOT_FOUND);
                    } else {
                        // キャラクタリスティックを見つけた
                        //
                        // Notification を要求する
                        boolean registered = gatt.setCharacteristicNotification(characteristic, true);

                        // Characteristic の Notification 有効化
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);

                        if (registered) {
                            // Characteristics通知設定完了
                            // TODO setStatus(BleStatus.NOTIFICATION_REGISTERED);
                        } else {
                            // TODO setStatus(BleStatus.NOTIFICATION_REGISTER_FAILED);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // READ成功
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged");
            // Characteristicの値更新通知
            if (DEVICE_BUTTON_SENSOR_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString())) {
                Byte value = characteristic.getValue()[0];
                boolean left = (0 < (value & 0x02));
                boolean right = (0 < (value & 0x01));
                updateButtonState(left, right);
            }
        }
    };

    private void updateButtonState(final boolean left, final boolean right) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View leftView = findViewById(R.id.left);
                View rightView = findViewById(R.id.right);
                leftView.setBackgroundColor((left ? Color.BLUE : Color.TRANSPARENT));
                rightView.setBackgroundColor((right ? Color.BLUE : Color.TRANSPARENT));
            }
        });
    }

//    private void setStatus(BleStatus status) {
//        mStatus = status;
//        mHandler.sendMessage(status.message());
//    }

    private enum BleStatus {
        DISCONNECTED, SCANNING, SCAN_FAILED, DEVICE_FOUND, SERVICE_NOT_FOUND, SERVICE_FOUND, CHARACTERISTIC_NOT_FOUND, NOTIFICATION_REGISTERED, NOTIFICATION_REGISTER_FAILED, CLOSED;

        public Message message() {
            Message message = new Message();
            message.obj = this;
            return message;
        }
    }
}
