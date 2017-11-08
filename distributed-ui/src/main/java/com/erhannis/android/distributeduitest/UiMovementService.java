package com.erhannis.android.distributeduitest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.erhannis.android.distributeduitest.starnetwork.StackableLocalService;
import com.erhannis.android.distributeduitest.starnetwork.StarService;

import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import java8.util.Objects;
import java8.util.function.Consumer;

/**
 * //TODO Change texts, ids?
 *
 * Created by erhannis on 10/14/17.
 */

public class UiMovementService extends StackableLocalService<UiMovementService> implements DistributedUiCommunicator {
  private static final String TAG = "UiMovementService";

  private NotificationManager mNM;

  private final ListOrderedMap<FragmentHandle, String> fragmentLocations = new ListOrderedMap<>();

  public UiMovementService() {
    super(StarService.class);
  }

  @Override
  public void onCreate() {
    mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

    // Display a notification about us starting.  We put an icon in the status bar.
    showNotification();

    doBindServices();
  }

  private StarService getStarService() {
    return ((StarService)getSubServices()[0]);
  }

  @Override
  protected void onAllConnected() {
    getStarService().registerAsHub(mStarMessageHubCallback);
    getStarService().registerAsSatellite(mStarMessageSatelliteCallback);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "Received start id " + startId + ": " + intent);
    return START_STICKY;
  }


  @Override
  protected void doUnbindServices() {
    // This should be the only place where this check is needed.
    if (getSubServices()[0] != null) {
      getStarService().unregisterAsHub(mStarMessageHubCallback);
      getStarService().unregisterAsSatellite(mStarMessageSatelliteCallback);
    }
    super.doUnbindServices();
  }

  @Override
  public void onDestroy() {
    // Cancel the persistent notification.
    mNM.cancel(StarService.NOTIFICATION);

    doUnbindServices();

    // Tell the user we stopped.
    Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
  }

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
  protected final Consumer<Object> mStarMessageSatelliteCallback = new Consumer<Object>() {
    @Override
    public void accept(Object msg) {
      if (msg instanceof DistributedUIFragmentChange) {
        DistributedUIFragmentChange change = (DistributedUIFragmentChange) msg;
        if (!Objects.equals(fragmentLocations.get(change.fragment), change.target)) {
          if (Objects.equals(fragmentLocations.get(change.fragment), getStarService().getId())) {
            // Fragment is moving away from here
            for (Consumer<FragmentHandle> c : dropFragmentCallbacks) {
              try {
                c.accept(change.fragment);
              } catch (Exception e) {
                Log.e(TAG, "Error running callback", e);
              }
            }
          } else if (change.target != null && change.target.equals(getStarService().getId())) {
            // Fragment is moving here
            for (Consumer<FragmentHandle> c : hostFragmentCallbacks) {
              try {
                c.accept(change.fragment);
              } catch (Exception e) {
                Log.e(TAG, "Error running callback", e);
              }
            }
          }
        }
        fragmentLocations.put(change.fragment, change.target);
      } else if (msg instanceof DistributedUIMethodCall) {
        //TODO Not sure if this is finished
        DistributedUIMethodCall call = (DistributedUIMethodCall) msg;
        for (Consumer<DistributedUIMethodCall> c : rpcSatelliteCallbacks) {
          try {
            c.accept(call);
          } catch (Exception e) {
            Log.e(TAG, "Error running satellite callback", e);
          }
        }
      }
    }
  };

  protected final Consumer<Object> mStarMessageHubCallback = new Consumer<Object>() {
    @Override
    public void accept(Object msg) {
      if (msg instanceof DistributedUIMethodCall) {
        //TODO Not sure if this is finished
        DistributedUIMethodCall call = (DistributedUIMethodCall) msg;
        for (Consumer<DistributedUIMethodCall> c : rpcHubCallbacks) {
          try {
            c.accept(call);
          } catch (Exception e) {
            Log.e(TAG, "Error running hub callback", e);
          }
        }
      }
    }
  };
  //</editor-fold>

  private Queue<Consumer<FragmentHandle>> hostFragmentCallbacks = new ConcurrentLinkedQueue<>();
  private Queue<Consumer<FragmentHandle>> dropFragmentCallbacks = new ConcurrentLinkedQueue<>();
  private Queue<Consumer<DistributedUIMethodCall>> rpcSatelliteCallbacks = new ConcurrentLinkedQueue<>();
  private Queue<Consumer<DistributedUIMethodCall>> rpcHubCallbacks = new ConcurrentLinkedQueue<>();

  public void registerCallbacks(boolean isHub, Consumer<FragmentHandle> hostFragment, Consumer<FragmentHandle> dropFragment, Consumer<DistributedUIMethodCall> rpcCallback) {
    hostFragmentCallbacks.add(hostFragment);
    dropFragmentCallbacks.add(dropFragment);
    if (isHub) {
      rpcHubCallbacks.add(rpcCallback);
    } else {
      rpcSatelliteCallbacks.add(rpcCallback);
    }
  }

  public void unregisterCallbacks(boolean isHub, Consumer<FragmentHandle> hostFragment, Consumer<FragmentHandle> dropFragment, Consumer<DistributedUIMethodCall> rpcCallback) {
    hostFragmentCallbacks.remove(hostFragment);
    dropFragmentCallbacks.remove(dropFragment);
    if (isHub) {
      rpcHubCallbacks.remove(rpcCallback);
    } else {
      rpcSatelliteCallbacks.remove(rpcCallback);
    }
  }

  public void registerFragment(FragmentHandle handle) {
    if (!fragmentLocations.keySet().contains(handle)) {
      fragmentLocations.put(handle, getStarService().getId());
      //TODO Is this necessary?
      relocateFragment(handle, getStarService().getId());
    }
  }

  public ListOrderedMap<FragmentHandle, String> getFragmentLocations() {
    ListOrderedMap<FragmentHandle, String> res = new ListOrderedMap<FragmentHandle, String>();
    res.putAll(fragmentLocations);
    return res;
  }

  public void relocateFragment(FragmentHandle handle, String satellite) {
    getStarService().sendToAll(new DistributedUIFragmentChange(satellite, handle));
  }

  public List<String> getLocations() {
    return getStarService().getAllDevices();
  }

  //<editor-fold desc="Comms pass-through">
  @Override
  public void sendToHub(String method, Object... args) {
    getStarService().sendToHub(new DistributedUIMethodCall(method, args));
  }

  @Override
  public Object sendToHubAndWait(String method, Object... args) {
    return getStarService().sendToHubAndWait(new DistributedUIMethodCall(method, args));
  }

  @Override
  public void sendToSatellites(String method, Object... args) {
    getStarService().sendToSatellites(new DistributedUIMethodCall(method, args));
  }

  @Override
  public Map<String, Object> sendToSatellitesAndWait(String method, Object... args) {
    return getStarService().sendToSatellitesAndWait(new DistributedUIMethodCall(method, args));
  }

  @Override
  public void sendToSatellite(String satellite, String method, Object... args) {
    getStarService().sendToSatellite(satellite, new DistributedUIMethodCall(method, args));
  }

  @Override
  public Object sendToSatelliteAndWait(String satellite, String method, Object... args) {
    return getStarService().sendToSatelliteAndWait(satellite, new DistributedUIMethodCall(method, args));
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