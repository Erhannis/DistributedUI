package com.erhannis.android.distributeduitest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.Map;
import java.util.UUID;

import java8.util.function.Consumer;

public class SatelliteActivity extends AppCompatActivity implements DistributedUiActivity {
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
      } else if (msg instanceof DistributedUIFragmentChange) {
        DistributedUIFragmentChange change = (DistributedUIFragmentChange)msg;
        switch (change.type) {
          case HOST_FRAGMENT:
            hostFragment(change.fragment);
            break;
          case DROP_FRAGMENT:
            dropFragment(change.fragment);
            break;
          default:
            throw new IllegalArgumentException("Unknown change type: " + change.type);
        }
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

  public void hostFragment(FragmentHandle fragmentHandle) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    try {
      Fragment fragment = fragmentManager.findFragmentByTag(fragmentHandle.name);
      if (fragment == null) {
        fragment = fragmentHandle.clazz.newInstance();
        fragmentTransaction.add(R.id.llFragmentHolder, fragment, fragmentHandle.name);
        fragmentTransaction.commit();
      } else {
        Log.e(TAG, "Fragment already hosted: " + fragmentHandle.name);
      }
    } catch (Exception e) {
      Log.e(TAG, "Error moving fragment", e);
    }
  }

  public void dropFragment(FragmentHandle fragmentHandle) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    Fragment fragment = fragmentManager.findFragmentByTag(fragmentHandle.name);
    if (fragment != null) {
      fragmentTransaction.remove(fragment);
    } else {
      Log.e(TAG, "Fragment not found: " + fragmentHandle.name);
    }
    fragmentTransaction.commit();
  }

  @Override
  public boolean implementsInterface(Class iface) {
    //TODO TODO Do
    return true;
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

  @Override
  public Object onMessage(String method, Object... args) {
    //TODO TODO Do
    return null;
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
