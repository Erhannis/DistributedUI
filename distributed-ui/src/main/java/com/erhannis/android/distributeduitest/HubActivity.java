package com.erhannis.android.distributeduitest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Map;

import java8.util.function.Consumer;

public abstract class HubActivity extends AppCompatActivity implements DistributedUiActivity {
  private static final String TAG = "HubActivity";

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

      mBoundService.registerAsHub(mMessageCallback);
    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundService = null;
      toast("Disconnected from star network service");
    }
  };

  void doBindService() {
    boolean bound = bindService(new Intent(HubActivity.this, StarService.class), mConnection, Context.BIND_AUTO_CREATE);
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
    setContentView(R.layout.activity_main);

    doBindService();

    findViewById(R.id.btnMoveFragment).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          //channel.publish("Show buttons fragment".getBytes());
        } catch (Exception e) {
          Log.e(TAG, "Error publishing to channel", e);
        }
      }
    });
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
