package com.erhannis.android.distributeduitest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.hgross.blaubot.core.IBlaubotDevice;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public abstract class HubActivity extends AppCompatActivity implements DistributedUiActivity {
  private static final String TAG = "HubActivity";

  private boolean mIsBound = false;
  private StarService mBoundService;

  protected final Consumer<Object> mMessageCallback = new Consumer<Object>() {
    @Override
    public void accept(Object msg) {
      if (msg instanceof DistributedUIMethodCall) {
        DistributedUIMethodCall call = (DistributedUIMethodCall)msg;
        switch (call.method) {
          case "implementsInterface":
            //TODO TODO Do
            break;
          default:
            onMessage(call.method, call.args);
            break;
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

      mBoundService.registerAsHub(mMessageCallback);
    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundService = null;
      toast("Disconnected from star network service");
    }
  };

  void doBindService() {
    boolean bound = bindService(new Intent(HubActivity.this, StarService.class), mConnection, Context.BIND_AUTO_CREATE);
    //TODO Change out logging
    System.out.println("bound: " + bound);
    mIsBound = true;
  }

  void doUnbindService() {
    if (mIsBound) {
      mBoundService.unregisterAsHub(mMessageCallback);
      unbindService(mConnection);
      mIsBound = false;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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
  public void sendToHub(String method, Object... args) {
    onMessage(method, args);
  }

  @Override
  public Object sendToHubAndWait(String method, Object... args) {
    return onMessage(method, args);
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
  public boolean implementsInterface(Class iface) {
    return iface.isAssignableFrom(this.getClass());
  }

  protected void toast(final String msg) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        System.out.println("toast: " + msg);
        Toast.makeText(HubActivity.this, msg, Toast.LENGTH_LONG).show();
      }
    });
  }
}
