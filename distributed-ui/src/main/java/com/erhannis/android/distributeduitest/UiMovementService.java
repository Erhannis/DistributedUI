package com.erhannis.android.distributeduitest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.erhannis.android.distributeduitest.starnetwork.StarService;

import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java8.util.function.Consumer;

/**
 * //TODO Change texts, ids?
 *
 * Created by erhannis on 10/14/17.
 */

public class UiMovementService extends Service implements DistributedUiCommunicator {
  private static final String TAG = "UiMovementService";

  private NotificationManager mNM;

  private final ListOrderedMap<FragmentHandle, String> fragmentLocations = new ListOrderedMap<>();

  public class LocalBinder extends Binder {
    public UiMovementService getService() {
      return UiMovementService.this;
    }
  }

  @Override
  public void onCreate() {
    mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

    // Display a notification about us starting.  We put an icon in the status bar.
    showNotification();

    doBindStarService();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "Received start id " + startId + ": " + intent);
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    // Cancel the persistent notification.
    mNM.cancel(StarService.NOTIFICATION);
    doUnbindStarService();

    // Tell the user we stopped.
    Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  // This is the object that receives interactions from clients.  See
  // RemoteService for a more complete example.
  private final IBinder mBinder = new LocalBinder();

  /**
   * Show a notification while this service is running.
   */
  private void showNotification() {
    CharSequence text = getText(R.string.local_service_started);

    //TODO Maybe I SHOULDN'T have this for the activity, on satellites?
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, FragmentManagerActivity.class), 0);

    // Set the info for the views that show in the notification panel.
    Notification notification = new Notification.Builder(this)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)  //TODO set
            .setTicker(text)  // the status text
            .setWhen(System.currentTimeMillis())  // the time stamp
            .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
            .setContentText(text)  // the contents of the entry
            .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
            .build();

    // Send the notification.
    mNM.notify(StarService.NOTIFICATION, notification);
  }

  //<editor-fold desc="Bind StarService">
  private boolean mIsBoundToStarService = false;
  private StarService mBoundStarService;

  protected final Consumer<Object> mStarMessageCallback = new Consumer<Object>() {
    @Override
    public void accept(Object msg) {
      if (msg instanceof DistributedUIFragmentChange) {
        DistributedUIFragmentChange change = (DistributedUIFragmentChange) msg;
        if (fragmentLocations.get(change.fragment) != null && !fragmentLocations.get(change.fragment).equals(change.target)) {
          if (fragmentLocations.get(change.fragment).equals(mBoundStarService.getId())) {
            // Fragment is moving away from here
            for (Consumer<FragmentHandle> c : hostFragmentCallbacks) {
              try {
                c.accept(change.fragment);
              } catch (Exception e) {
                Log.e(TAG, "Error running callback", e);
              }
            }
          } else if (change.target != null && change.target.equals(mBoundStarService.getId())) {
            // Fragment is moving here
            for (Consumer<FragmentHandle> c : dropFragmentCallbacks) {
              try {
                c.accept(change.fragment);
              } catch (Exception e) {
                Log.e(TAG, "Error running callback", e);
              }
            }
          }
        } else if (msg instanceof DistributedUIMethodCall) {
          //TODO Not sure if this is finished
          DistributedUIMethodCall call = (DistributedUIMethodCall) msg;
          for (Consumer<DistributedUIMethodCall> c : rpcCallbacks) {
            try {
              c.accept(call);
            } catch (Exception e) {
              Log.e(TAG, "Error running callback", e);
            }
          }
        }
      }
    }
  };

  private ServiceConnection mStarConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mBoundStarService = ((StarService.LocalBinder)service).getService();
      toast("Connected to star network service");

      // Not sure if "satellite" is accurate
      mBoundStarService.registerAsSatellite(mStarMessageCallback);
    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundStarService = null;
      toast("Disconnected from star network service");
    }
  };

  private void doBindStarService() {
    boolean bound = bindService(new Intent(UiMovementService.this, StarService.class), mStarConnection, Context.BIND_AUTO_CREATE);
    //TODO Change out logging
    System.out.println("bound: " + bound);
    mIsBoundToStarService = true;
  }

  private void doUnbindStarService() {
    if (mIsBoundToStarService) {
      mBoundStarService.unregisterAsSatellite(mStarMessageCallback);
      unbindService(mStarConnection);
      mIsBoundToStarService = false;
    }
  }
  //</editor-fold>

  private ArrayList<Consumer<FragmentHandle>> hostFragmentCallbacks = new ArrayList<>();
  private ArrayList<Consumer<FragmentHandle>> dropFragmentCallbacks = new ArrayList<>();
  private ArrayList<Consumer<DistributedUIMethodCall>> rpcCallbacks = new ArrayList<>();

  public void registerCallbacks(Consumer<FragmentHandle> hostFragment, Consumer<FragmentHandle> dropFragment, Consumer<DistributedUIMethodCall> rpcCallback) {
    hostFragmentCallbacks.add(hostFragment);
    dropFragmentCallbacks.add(dropFragment);
    rpcCallbacks.add(rpcCallback);
  }

  public void unregisterCallbacks(Consumer<FragmentHandle> hostFragment, Consumer<FragmentHandle> dropFragment, Consumer<DistributedUIMethodCall> rpcCallback) {
    hostFragmentCallbacks.remove(hostFragment);
    dropFragmentCallbacks.remove(dropFragment);
    rpcCallbacks.remove(rpcCallback);
  }

  public void registerFragment(FragmentHandle handle) {
    if (!fragmentLocations.keySet().contains(handle)) {
      fragmentLocations.put(handle, mBoundStarService.getId());
      //TODO Is this necessary?
      relocateFragment(handle, mBoundStarService.getId());
    }
  }

  public ListOrderedMap<FragmentHandle, String> getFragmentLocations() {
    ListOrderedMap<FragmentHandle, String> res = new ListOrderedMap<FragmentHandle, String>();
    res.putAll(fragmentLocations);
    return res;
  }

  public void relocateFragment(FragmentHandle handle, String satellite) {
    if (mIsBoundToStarService) {
      mBoundStarService.sendToAll(new DistributedUIFragmentChange(satellite, handle));
    }
  }

  public List<String> getLocations() {
    if (mIsBoundToStarService) {
      return mBoundStarService.getAllDevices();
    }
    //TODO ?
    return null;
  }

  //<editor-fold desc="Comms pass-through">
  @Override
  public void sendToHub(String method, Object... args) {
    mBoundStarService.sendToHub(new DistributedUIMethodCall(method, args));
  }

  @Override
  public Object sendToHubAndWait(String method, Object... args) {
    return mBoundStarService.sendToHubAndWait(new DistributedUIMethodCall(method, args));
  }

  @Override
  public void sendToSatellites(String method, Object... args) {
    mBoundStarService.sendToSatellites(new DistributedUIMethodCall(method, args));
  }

  @Override
  public Map<String, Object> sendToSatellitesAndWait(String method, Object... args) {
    return mBoundStarService.sendToSatellitesAndWait(new DistributedUIMethodCall(method, args));
  }

  @Override
  public void sendToSatellite(String satellite, String method, Object... args) {
    mBoundStarService.sendToSatellite(satellite, new DistributedUIMethodCall(method, args));
  }

  @Override
  public Object sendToSatelliteAndWait(String satellite, String method, Object... args) {
    return mBoundStarService.sendToSatelliteAndWait(satellite, new DistributedUIMethodCall(method, args));
  }
  //</editor-fold>


  protected void toast(final String msg) {
    new Handler(getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        System.out.println("toast: " + msg);
        Toast.makeText(UiMovementService.this, msg, Toast.LENGTH_LONG).show();
      }
    });
  }
}