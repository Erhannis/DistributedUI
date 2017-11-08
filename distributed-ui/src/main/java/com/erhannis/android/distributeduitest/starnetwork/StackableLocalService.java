package com.erhannis.android.distributeduitest.starnetwork;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.erhannis.android.distributeduitest.UiMovementService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import java8.util.function.Consumer;

import static android.content.ContentValues.TAG;

/**
 * Represents a local service that depends on other local services.  When the callback passed to
 * (LocalBinder).addStackConnectedListener(Consumer&gt;IMPL&lt;) is called, it is guaranteed that
 * all services in the chain have started and are connected.
 *
 * Being local services, they should never disconnect, and thus should be available at every point
 * after that - calling code should not need to check if a given service is non-null and connected,
 * if the callback has occurred.
 * At least until I discover otherwise.
 *
 * IMPL should be the implementing class.
 *
 * NOTE: if there's a cycle in the dependencies, this will never complete.  The services will keep waiting for each other.
 * //TODO Could add checking (and resolution?) for that.
 *
 * Created by erhannis on 11/7/17.
 */

public abstract class StackableLocalService<IMPL extends StackableLocalService> extends Service {
  private static final String TAG = "StackableLocalService";

  public class LocalBinder extends Binder {
    public void addStackConnectedListener(Consumer<IMPL> callback) {
      mStackConnectedCallerback.addCallback(callback);
    }
  }

  private final IBinder mBinder = new LocalBinder();
  private boolean mIsConnectedToAll = false;
  private final DelayedCallerback<IMPL> mStackConnectedCallerback = new DelayedCallerback<>(new Consumer<Consumer<IMPL>>() {
    @Override
    public void accept(Consumer<IMPL> implConsumer) {
      implConsumer.accept((IMPL)StackableLocalService.this);
    }
  });

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  private final Class<? extends StackableLocalService>[] mBindServiceClasses;
  private final StackableLocalService[] mBoundServices;
  private final boolean[] mIsBoundServices;
  private final boolean[] mIsFullyConnectedServices;
  private final ServiceConnection[] mServiceConnections;

  public StackableLocalService(Class<? extends StackableLocalService>... bindServices) {
    mBindServiceClasses = Arrays.copyOf(bindServices, bindServices.length);
    mBoundServices = new StackableLocalService[mBindServiceClasses.length];
    mIsBoundServices = new boolean[mBindServiceClasses.length];
    mIsFullyConnectedServices = new boolean[mBindServiceClasses.length];
    mServiceConnections = new ServiceConnection[mBindServiceClasses.length];

    for (int i = 0; i < mServiceConnections.length; i++) {
      final int fi = i;
      mServiceConnections[i] = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
          ((StackableLocalService.LocalBinder)service).addStackConnectedListener(new Consumer<IMPL>() {
            @Override
            public void accept(IMPL service) {
              mBoundServices[fi] = service;
              mIsFullyConnectedServices[fi] = true;
              checkAllConnected();

              Log.d(TAG, "Fully connected to service: " + mBindServiceClasses[fi]);
            }
          });
          Log.d(TAG, "Partally connected to service: " + mBindServiceClasses[fi]);
        }

        public void onServiceDisconnected(ComponentName className) {
          mBoundServices[fi] = null;
          mIsFullyConnectedServices[fi] = false;
          mIsConnectedToAll = false;
          throw new IllegalStateException("Shouldn't be possible; disconnected from service: " + mBindServiceClasses[fi]);
        }
      };
    }
  }

  private synchronized void checkAllConnected() {
    for (boolean connected : mIsFullyConnectedServices) {
      if (!connected) {
        return;
      }
    }
    // Everything's connected

    mIsConnectedToAll = true;
    onAllConnected();
    //TODO What if they need to wait for something to finish before calling back?
    mStackConnectedCallerback.trigger();
  }

  protected abstract void onAllConnected();

  protected StackableLocalService[] getSubServices() {
    return Arrays.copyOf(mBoundServices, mBoundServices.length);
  }

  //<editor-fold desc="Binding">

  /**
   * Binds services.  Call in onCreate().
   */
  protected void doBindServices() {
    checkAllConnected();
    for (int i = 0; i < mBindServiceClasses.length; i++) {
      //TODO Is this the BIND flag we want?
      boolean bound = bindService(new Intent(StackableLocalService.this, mBindServiceClasses[i]), mServiceConnections[i], Context.BIND_AUTO_CREATE);
      Log.d(TAG, "bound to " + mBindServiceClasses[i] + ": " + bound);
      mIsBoundServices[i] = true;
    }
  }

  /**
   * Unbinds services.  Call in onDestroy(), but first call all your cleanup code.
   * //TODO Maybe they need access to mIsBoundServices?  - Only if connecting hasn't finished....
   */
  protected void doUnbindServices() {
    for (int i = 0; i < mBindServiceClasses.length; i++) {
      if (mIsBoundServices[i]) {
        unbindService(mServiceConnections[i]);
        Log.d(TAG, "unbound from " + mBindServiceClasses[i]);
        mIsBoundServices[i] = false;
      }
    }
  }
  //</editor-fold>
}
