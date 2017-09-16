package com.erhannis.android.distributeduitest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.UUID;

import eu.hgross.blaubot.android.BlaubotAndroid;
import eu.hgross.blaubot.android.BlaubotAndroidFactory;
import eu.hgross.blaubot.core.Blaubot;
import eu.hgross.blaubot.core.IBlaubotDevice;
import eu.hgross.blaubot.core.ILifecycleListener;
import eu.hgross.blaubot.messaging.BlaubotMessage;
import eu.hgross.blaubot.messaging.IBlaubotChannel;
import eu.hgross.blaubot.messaging.IBlaubotMessageListener;

public class SatelliteActivity extends AppCompatActivity {
  private static final String TAG = "SatelliteActivity";
  public static final String NAME = "DistUIName";
  public static final UUID MY_UUID = UUID.fromString("e72cd6c5-2c05-42ca-8f77-91d90649c970");

  protected static Blaubot mBlaubot;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_satellite);

    mBlaubot = BlaubotAndroidFactory.createEthernetBlaubot(MY_UUID);
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
        channel.publish("Init: Hello world, from satellite!".getBytes());
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
          channel.publish("Hello world, from satellite!".getBytes());
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
}
