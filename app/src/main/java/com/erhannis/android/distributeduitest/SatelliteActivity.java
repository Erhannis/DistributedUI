package com.erhannis.android.distributeduitest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

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
import java.util.UUID;

import eu.hgross.blaubot.android.BlaubotAndroid;
import eu.hgross.blaubot.android.BlaubotAndroidFactory;
import eu.hgross.blaubot.core.Blaubot;
import eu.hgross.blaubot.core.IBlaubotDevice;
import eu.hgross.blaubot.core.ILifecycleListener;
import eu.hgross.blaubot.messaging.BlaubotMessage;
import eu.hgross.blaubot.messaging.IBlaubotChannel;
import eu.hgross.blaubot.messaging.IBlaubotMessageListener;

public class SatelliteActivity extends AppCompatActivity implements DistributedUiActivity {
  private static final String TAG = "SatelliteActivity";
  public static final String NAME = "DistUIName";
  public static final UUID MY_UUID = UUID.fromString("e72cd6c5-2c05-42ca-8f77-91d90649c970");

  protected Blaubot mBlaubot;
  protected IBlaubotChannel mChannel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_satellite);

    mBlaubot = BlaubotAndroidFactory.createEthernetBlaubot(MY_UUID);
    mBlaubot.startBlaubot();

    mChannel = mBlaubot.createChannel((short)1);

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
        mChannel.subscribe(new IBlaubotMessageListener() {
          @Override
          public void onMessage(BlaubotMessage blaubotMessage) {
            try {
              //TODO Sort messages
              String msg = new String(blaubotMessage.getPayload());
              System.out.println("Got message: " + msg + " : " + blaubotMessage);
              if ("Show buttons fragment".equals(msg)) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                ButtonsFragment fragment = new ButtonsFragment();
                fragmentTransaction.add(R.id.llFragmentHolder, fragment);
                fragmentTransaction.commit();
              }
            } catch (Exception e) {
              Log.e(TAG, "Error parsing message", e);
            }
          }
        });
        mChannel.publish("Init: Hello world, from satellite!".getBytes());
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

    findViewById(R.id.btnListenForConnection).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          mChannel.publish("Hello world, from satellite!".getBytes());
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

  @Override
  public void send(String method, Object... args) {
    try {
      //TODO Static Kryo?
      Kryo kryo = new Kryo();
      DistributedUIMethodCall call = new DistributedUIMethodCall(method, args);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Output out = new Output(baos);
      kryo.writeClassAndObject(out, call);
      out.flush();
      out.flush();
      baos.flush();
      baos.close();
      //kryo.writeObject(out, call);
      mChannel.publish(baos.toByteArray());
    } catch (Exception e) {
      Log.e(TAG, "Error sending method event", e);
    }
  }

  @Override
  public Object sendAndWait(String method, Object... args) {
    return null;
  }
}
