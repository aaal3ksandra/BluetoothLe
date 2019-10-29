package com.example.bluetoothle;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main Activity";
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    android.bluetooth.le.BluetoothLeScanner mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    BluetoothDevice mBluetoothDevice;
    UUID CLIMATE_UUID = UUID.fromString("19B10010-E8F2-537E-4F6C-D104768A1214");
    UUID TEMP_CHAR_UUID = UUID.fromString("19B10012-E8F2-537E-4F6C-D104768A1214");
    UUID[] serviceUUIDS = new UUID[]{CLIMATE_UUID};
    byte[] curTemp;

    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (mBluetoothAdapter == null || (!mBluetoothAdapter.isEnabled())) {
            Intent enabledBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabledBluetoothIntent, 1);
        } else startScan();
    }

    public void startScan() {
        List<ScanFilter> scanFilter = null;
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        if (serviceUUIDS != null) {
            scanFilter = new ArrayList<>();
            for (UUID serviceUUID : serviceUUIDS) {
                ScanFilter scanFilters = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(serviceUUID))
                        .build();
                scanFilter.add(scanFilters);
            }
        }
        // Checks if Bluetooth is supported on the device.

        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.startScan(scanFilter, scanSettings, mScanCallback);
            //Toast.makeText(context, mBluetoothDevice.getAddress(), Toast.LENGTH_LONG);
            Log.d(TAG, "started scanning");
        } else {
            //Stop Scanning for Bluetooth Device
            mBluetoothLeScanner.stopScan(mScanCallback);
            Log.d(TAG, "stopped scanning");
        }
    }



    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mBluetoothDevice = result.getDevice();
            mBluetoothDevice.connectGatt(getApplicationContext(), true, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
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

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        public List<BluetoothGattCharacteristic> chars = new ArrayList<>();

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange called.");
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                    final List<BluetoothGattService> services = gatt.getServices();
                    Log.i(TAG, String.format(Locale.ENGLISH, "discovered %d services for '%s'", services.size(), mBluetoothAdapter.getName()));
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close();
                } else {
                }
            } else {
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered called");

            if (status == GATT_SUCCESS) {
                BluetoothGattCharacteristic temp = gatt.getService(CLIMATE_UUID).getCharacteristic(TEMP_CHAR_UUID);
                chars.add(temp);
                requestCharacteristics(gatt);
            }
        }

        public void requestCharacteristics(BluetoothGatt gatt) {
            gatt.readCharacteristic(chars.get(chars.size() - 1));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic charateristic, int status) {
            super.onCharacteristicRead(gatt, charateristic, status);
            if (status == 0) {
                if (charateristic.getUuid().equals(TEMP_CHAR_UUID))
                    curTemp = charateristic.getValue();
                for (byte b : curTemp) {
                    System.out.println("TEMP Value: " + b);
                    TextView temp = findViewById(R.id.temp_reading);
                    temp.setText(b);
                }

                chars.remove(chars.get(chars.size() - 1));
            }
        }


    };


}





