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

public class SatelliteActivity extends AppCompatActivity {
  private static final String TAG = "SatelliteActivity";
  public static final String NAME = "DistUIName";
  public static final UUID MY_UUID = UUID.fromString("e72cd6c5-2c05-42ca-8f77-91d90649c970");

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_satellite);

    findViewById(R.id.btnListenForConnection).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
          System.err.println("Device does not support bluetooth");
          return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
          System.err.println("Bluetooth is not enabled");
          //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
          return;
        }

        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket serverSocket = null;
        try {
          // MY_UUID is the app's UUID string, also used by the client code.
          serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
          Log.e(TAG, "Socket's listen() method failed", e);
        }

        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
          try {
            socket = serverSocket.accept();
          } catch (IOException e) {
            Log.e(TAG, "Socket's accept() method failed", e);
            break;
          }

          if (socket != null) {
            // A connection was accepted. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(socket);
            try {
              serverSocket.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
            break;
          }
        }
      }
    });
  }

  protected void manageMyConnectedSocket(final BluetoothSocket socket) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          socket.connect();
          InputStream is = socket.getInputStream();
          OutputStream os = socket.getOutputStream();
          BufferedReader br = new BufferedReader(new InputStreamReader(is));
          String line;
          while ((line = br.readLine()) != null) {
            System.out.println("Read line: " + line);
          }
          System.out.println("Read EOF");
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
}
