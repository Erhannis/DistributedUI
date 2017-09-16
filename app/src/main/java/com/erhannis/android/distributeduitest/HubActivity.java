package com.erhannis.android.distributeduitest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

public class HubActivity extends AppCompatActivity implements ButtonsFragment.ButtonsFragmentCallback {
  private static final String TAG = "HubActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.btnMoveFragment).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
          System.err.println("Device does not support bluetooth");
          return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
          System.err.println("Bluetooth is not enabled");
          //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
          return;
        }
        System.out.println("Got adapter");

        //TODO Discover devices, pair?  Allow make appendage device discoverable?

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
          // There are paired devices. Get the name and address of each paired device.
          for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            System.out.println("Paired device: " + deviceName + " ; " + deviceHardwareAddress);
          }
        }

        BluetoothSocket tmp = null;
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice();
        System.out.println("Got device");

        try {
          tmp = device.createRfcommSocketToServiceRecord(SatelliteActivity.MY_UUID);
          System.out.println("Got socket");
        } catch (IOException e) {
          Log.e(TAG, "Socket's create() method failed", e);
          System.out.println("Did not get socket");
        }
        final BluetoothSocket socket = tmp;

        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              System.out.println("Connecting socket");
              socket.connect();
              System.out.println("Connected socket");
              InputStream is = socket.getInputStream();
              OutputStream os = socket.getOutputStream();
              System.out.println("Writing to stream");
              BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
              for (int i = 0; i < 10; i++) {
                bw.write("this is test " + i + "\n");
                bw.flush();
              }
              System.out.println("Wrote to stream");
              Thread.sleep(10000);
              bw.close();
              socket.close();
            } catch (IOException e) {
              e.printStackTrace();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }).start();
      }
    });
  }

  private void proxyTest() {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("Invoked: " + method + ", " + args);
        if (method.getReturnType() == void.class) {

        } else {

        }
        return null;
      }
    };
    Class<?> proxyClass = Proxy.getProxyClass(ButtonsFragment.ButtonsFragmentCallback.class.getClassLoader(), ButtonsFragment.ButtonsFragmentCallback.class);
    try {
      ButtonsFragment.ButtonsFragmentCallback bfc = (ButtonsFragment.ButtonsFragmentCallback) proxyClass.getConstructor(InvocationHandler.class).newInstance(handler);
      bfc.buttonClicked("this is a test");
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void buttonClicked(String s) {
    System.out.println("(HubActivity).buttonClicked(): " + s);
  }
}
