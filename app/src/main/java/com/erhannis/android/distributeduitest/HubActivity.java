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

import eu.hgross.blaubot.android.BlaubotAndroidFactory;
import eu.hgross.blaubot.core.Blaubot;
import eu.hgross.blaubot.core.IBlaubotDevice;
import eu.hgross.blaubot.core.ILifecycleListener;
import eu.hgross.blaubot.messaging.BlaubotMessage;
import eu.hgross.blaubot.messaging.IBlaubotChannel;
import eu.hgross.blaubot.messaging.IBlaubotMessageListener;

public class HubActivity extends AppCompatActivity implements ButtonsFragment.ButtonsFragmentCallback {
  private static final String TAG = "HubActivity";

  protected static Blaubot mBlaubot;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mBlaubot = BlaubotAndroidFactory.createEthernetBlaubot(SatelliteActivity.MY_UUID);
    mBlaubot.startBlaubot();

    final IBlaubotChannel channel = mBlaubot.createChannel((short)1);

    // attach life cycle listener
    mBlaubot.addLifecycleListener(new ILifecycleListener() {
      @Override
      public void onDisconnected() {
        System.out.println("bb onDisconnected");
      }

      @Override
      public void onDeviceLeft(IBlaubotDevice blaubotDevice) {
        System.out.println("bb onDeviceLeft: " + blaubotDevice);
      }

      @Override
      public void onDeviceJoined(IBlaubotDevice blaubotDevice) {
        System.out.println("bb onDeviceJoined: " + blaubotDevice);
      }

      @Override
      public void onConnected() {
        System.out.println("bb onConnected");
        // THIS device connected to a network
        // you can now subscribe to channels and use them:
        channel.subscribe(new IBlaubotMessageListener() {
          @Override
          public void onMessage(BlaubotMessage blaubotMessage) {
            System.out.println("Got message: " + new String(blaubotMessage.getPayload()) + " : " + blaubotMessage);
          }
        });
        channel.publish("Init: Hello world, from hub!".getBytes());
        // onDeviceJoined(...) calls will follow for each OTHER device that was already connected
      }

      @Override
      public void onPrinceDeviceChanged(IBlaubotDevice oldPrince, IBlaubotDevice newPrince) {
        System.out.println("bb onPrinceDeviceChanged: " + oldPrince + " -> " + newPrince);
      }

      @Override
      public void onKingDeviceChanged(IBlaubotDevice oldKing, IBlaubotDevice newKing) {
        System.out.println("bb onKingDeviceChanged: " + oldKing + " -> " + newKing);
      }
    });

    findViewById(R.id.btnMoveFragment).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          channel.publish("Hello world, from hub!".getBytes());
        } catch (Exception e) {
          Log.e(TAG, "Error publishing to channel", e);
        }
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    mBlaubot.startBlaubot();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mBlaubot.stopBlaubot();
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
