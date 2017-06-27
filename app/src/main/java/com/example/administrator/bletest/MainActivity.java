package com.example.administrator.bletest;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.security.auth.login.LoginException;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final int REQUEST_ENABLE_BT = 0;
    private final UUID MY_UUID = UUID.fromString("abcd1234-ab12-ab12-ab12-abcdef123456");
    private static final long SCAN_TIME = 1000;
    private Handler handler =  new Handler();
    private List<BluetoothDevice> list;
    private List<String> name;
    private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器
    private  BluetoothReceiver blueRe;//蓝牙设备
    private ListAdapter adapter;//填充数据
    private ListView listview;
    private BluetoothGattService service;
    private BluetoothGatt bluetoothGatt;
    private BluetoothSocket socket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        checkBlePermission();
        isSupportBle();
    }
    private void initView(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Button scan = ((Button) findViewById(R.id.btn));
        Button send = ((Button) findViewById(R.id.send));
        listview = ((ListView) findViewById(R.id.listview));
        list = new ArrayList<>();
        name = new ArrayList<>();
        scan.setOnClickListener(this);
        send.setOnClickListener(this);
        //注册蓝牙监听广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        blueRe = new BluetoothReceiver();
        registerReceiver(blueRe, intentFilter);
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                scanDevice();
                break;
            case R.id.send:
                break;
        }
    }

    /**
     * 判断设备是否支持蓝牙ble
     * @return
     */
    private boolean isSupportBle(){
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持ble", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            return true;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanDevice(){
        if (mBluetoothAdapter.isEnabled()){
            //可见进行扫描，10秒后停止扫描,用Handler进行延时
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.cancelDiscovery();
                }
            },SCAN_TIME);
            mBluetoothAdapter.startDiscovery();
        }else {
            //不可见，直接停止扫描
            mBluetoothAdapter.cancelDiscovery();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                Toast.makeText(this, "蓝牙已启用", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, "蓝牙未启用", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    /**
     * 检查蓝牙权限
     */
    public void checkBlePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        } else {
            Log.i("tag","已申请权限");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // 如果请求被取消，则结果数组为空。
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("tag","同意申请");
                } else {
                    Log.i("tag","拒绝申请");
                }
                return;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(list.get(position).getAddress());
        if (remoteDevice.getBondState()==BluetoothDevice.BOND_BONDED){
           new Thread(new Runnable() {
               @Override
               public void run() {
                   try {
                       socket = remoteDevice.createRfcommSocketToServiceRecord(MY_UUID);
                       socket.connect();
                       if (socket.isConnected()){
                           sendData();
                       }
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
           }).run();
        }else{
          new Thread(new Runnable() {
              @Override
              public void run() {
                  Method createBond = null;
                  try {
                      createBond = remoteDevice.getClass().getMethod("createBond");
                      boolean isok = (boolean) createBond.invoke(remoteDevice);
                      if (isok){
                          socket = remoteDevice.createRfcommSocketToServiceRecord(MY_UUID);
                          socket.connect();
                          if (socket.isConnected()){
                              sendData();
                          }
                      }
                  } catch (NoSuchMethodException e) {
                      e.printStackTrace();
                  } catch (InvocationTargetException e) {
                      e.printStackTrace();
                  } catch (IllegalAccessException e) {
                      e.printStackTrace();
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              }
          }).run();
        }
    }

    private void sendData() {
        // 蓝牙服务端发送简单的一个字符串：hello,world!给连接的客户
        String s = "hello,world ! by zhangphil";
        byte[] buffer = s.getBytes();
        try {
            OutputStream os = socket.getOutputStream();
            Log.d("JJJ=====", "服务器端数据发送完毕!"+s);
            os.write(buffer);
            os.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(blueRe);
    }
    class BluetoothReceiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e("JJJ=======", "str========: "+device.getName());
                if (!name.contains(device.getAddress())&&device.getAddress()!=null){
                    list.add(device);
                    name.add(device.getAddress());
                }
                adapter = new ListAdapter(list,context);
                listview.setAdapter(adapter);
                listview.setOnItemClickListener(MainActivity.this);
            }
        }
    }
}
