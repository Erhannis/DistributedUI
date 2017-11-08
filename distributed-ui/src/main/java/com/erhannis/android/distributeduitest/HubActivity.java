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

import com.erhannis.android.distributeduitest.starnetwork.StarService;

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
      switch (msg.method) {
        case "implementsInterface":
          //TODO TODO Do
          break;
        default:
          onMessage(msg.method, msg.args);
          break;
      }
    }
  };

  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      ((UiMovementService.LocalBinder)service).addStackConnectedListener(new Consumer<UiMovementService>() {
        @Override
        public void accept(UiMovementService uiMovementService) {
          mBoundService = uiMovementService;

          toast("Connected to ui movement service");
          Log.d(TAG, "Fully connected to UiMovementService");

          for (FragmentHandle f : getFragmentHandles()) {
            mBoundService.registerFragment(f);
          }
          mBoundService.registerCallbacks(true, mHostFragmentCallback, mDropFragmentCallback, mRpcCallback);
        }
      });
      Log.d(TAG, "Partially connected to UiMovementService");
    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundService = null;
      toast("Disconnected from ui movement service");
    }
  };

  void doBindService() {
    boolean bound = bindService(new Intent(HubActivity.this, UiMovementService.class), mConnection, Context.BIND_AUTO_CREATE);
    //TODO Change out logging
    System.out.println("bound: " + bound);
    mIsBound = true;
  }

  void doUnbindService() {
    if (mIsBound) {
      //TODO Hmm.  This could lead to leaks, if doUnbindService is called before the service is properly bound.
      //TODO DelayedCallback?
      //TODO Some kind of generic opposing method calls?
      mBoundService.unregisterCallbacks(true, mHostFragmentCallback, mDropFragmentCallback, mRpcCallback);
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

  public abstract void hostFragment(FragmentHandle fragmentHandle);

  public abstract void dropFragment(FragmentHandle fragmentHandle);

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

  @Override
  public boolean implementsInterface(Class iface) {
    return iface.isAssignableFrom(this.getClass());
  }

  public abstract List<FragmentHandle> getFragmentHandles();

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
