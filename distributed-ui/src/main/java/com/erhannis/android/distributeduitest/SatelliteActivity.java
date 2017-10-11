package com.erhannis.android.distributeduitest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import java8.util.function.Consumer;

public abstract class SatelliteActivity extends AppCompatActivity implements DistributedUiActivity {
  private static final String TAG = "SatelliteActivity";
  public static final String NAME = "DistUIName";
  public static final UUID MY_UUID = UUID.fromString("e72cd6c5-2c05-42ca-8f77-91d90649c970");

  private boolean mIsBound = false;
  private StarService mBoundService;

  protected final Consumer<Object> mMessageCallback = new Consumer<Object>() {
    @Override
    public void accept(Object msg) {
      if (msg instanceof DistributedUIMethodCall) {
        DistributedUIMethodCall call = (DistributedUIMethodCall)msg;
        onMessage(call.method, call.args);
      } else {
        Log.e(TAG, "Received an unknown message: " + msg);
      }
    }
  };

  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mBoundService = ((StarService.LocalBinder)service).getService();
      toast("Connected to star network service");

      mBoundService.registerAsSatellite(mMessageCallback);
    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundService = null;
      toast("Disconnected from star network service");
    }
  };

  void doBindService() {
    boolean bound = bindService(new Intent(SatelliteActivity.this, StarService.class), mConnection, Context.BIND_AUTO_CREATE);
    System.out.println("bound: " + bound);
    mIsBound = true;
  }

  void doUnbindService() {
    if (mIsBound) {
      mBoundService.unregisterAsSatellite(mMessageCallback);
      unbindService(mConnection);
      mIsBound = false;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_satellite);

    doBindService();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    doUnbindService();
  }

  public void onReceiveFragment(String method, Object... args) {
    if ("moveFragment".equals(method)) {
      //TODO Just a test hack
      FragmentManager fragmentManager = getSupportFragmentManager();
      FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
      ButtonsFragment fragment = new ButtonsFragment();
      fragmentTransaction.add(R.id.llFragmentHolder, fragment);
      fragmentTransaction.commit();
    } else {
      System.out.println("unknown method: " + method);
    }
  }

  @Override
  public void sendToHub(String method, Object... args) {
    mBoundService.sendToHub(new DistributedUIMethodCall(method, args));
  }

  @Override
  public Object sendToHubAndWait(String method, Object... args) {
    return mBoundService.sendToHubAndWait(new DistributedUIMethodCall(method, args));
  }

  @Override
  public void sendToSatellites(String method, Object... args) {
    mBoundService.sendToSatellites(new DistributedUIMethodCall(method, args));
  }

  @Override
  public Map<String, Object> sendToSatellitesAndWait(String method, Object... args) {
    return mBoundService.sendToSatellitesAndWait(new DistributedUIMethodCall(method, args));
  }

  @Override
  public void sendToSatellite(String satellite, String method, Object... args) {
    mBoundService.sendToSatellite(satellite, new DistributedUIMethodCall(method, args));
  }

  @Override
  public Object sendToSatelliteAndWait(String satellite, String method, Object... args) {
    return mBoundService.sendToSatelliteAndWait(satellite, new DistributedUIMethodCall(method, args));
  }

  protected void toast(final String msg) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        System.out.println("toast: " + msg);
        Toast.makeText(SatelliteActivity.this, msg, Toast.LENGTH_LONG).show();
      }
    });
  }
}
