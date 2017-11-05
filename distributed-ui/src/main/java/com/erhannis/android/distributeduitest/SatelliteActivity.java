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

import com.erhannis.android.distributeduitest.starnetwork.StarService;

import java.util.Map;
import java.util.UUID;

import java8.util.function.Consumer;

public class SatelliteActivity extends AppCompatActivity implements DistributedUiActivity {
  private static final String TAG = "SatelliteActivity";
  public static final String NAME = "DistUIName";

  private boolean mIsBound = false;
  private UiMovementService mBoundService;

  protected final Consumer<FragmentHandle> mHostFragmentCallback = new Consumer<FragmentHandle>() {
    @Override
    public void accept(FragmentHandle f) {
      hostFragment(f);
    }
  };

  protected final Consumer<FragmentHandle> mDropFragmentCallback = new Consumer<FragmentHandle>() {
    @Override
    public void accept(FragmentHandle f) {
      dropFragment(f);
    }
  };

  protected final Consumer<DistributedUIMethodCall> mRpcCallback = new Consumer<DistributedUIMethodCall>() {
    @Override
    public void accept(DistributedUIMethodCall msg) {
      onMessage(msg.method, msg.args);
    }
  };

  private final ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mBoundService = ((UiMovementService.LocalBinder)service).getService();
      mBoundService.registerCallbacks(mHostFragmentCallback, mDropFragmentCallback, mRpcCallback);
      toast("Connected to ui movement service");
    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundService = null;
      toast("Disconnected from ui movement service");
    }
  };

  void doBindService() {
    boolean bound = bindService(new Intent(SatelliteActivity.this, StarService.class), mConnection, Context.BIND_AUTO_CREATE);
    System.out.println("bound: " + bound);
    mIsBound = true;
  }

  void doUnbindService() {
    if (mIsBound) {
      mBoundService.unregisterCallbacks(mHostFragmentCallback, mDropFragmentCallback, mRpcCallback);
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
    Fragment fragment = fragmentManager.findFragmentByTag(fragmentHandle.name);
    if (fragment != null) {
      FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
      fragmentTransaction.remove(fragment);
      fragmentTransaction.commit();
    } else {
      Log.d(TAG, "Fragment not found: " + fragmentHandle.name);
    }
  }

  @Override
  public boolean implementsInterface(Class iface) {
    //TODO TODO Do
    return true;
  }

  //<editor-fold desc="Comms pass-through">
  @Override
  public void sendToHub(String method, Object... args) {
    mBoundService.sendToHub(method, args);
  }

  @Override
  public Object sendToHubAndWait(String method, Object... args) {
    return mBoundService.sendToHubAndWait(method, args);
  }

  @Override
  public void sendToSatellites(String method, Object... args) {
    mBoundService.sendToSatellites(method, args);
  }

  @Override
  public Map<String, Object> sendToSatellitesAndWait(String method, Object... args) {
    return mBoundService.sendToSatellitesAndWait(method, args);
  }

  @Override
  public void sendToSatellite(String satellite, String method, Object... args) {
    mBoundService.sendToSatellite(satellite, method, args);
  }

  @Override
  public Object sendToSatelliteAndWait(String satellite, String method, Object... args) {
    return mBoundService.sendToSatelliteAndWait(satellite, method, args);
  }
  //</editor-fold>

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
