package com.erhannis.android.distributeduitest.starnetwork;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.erhannis.android.distributeduitest.FragmentManagerActivity;
import com.erhannis.android.distributeduitest.R;
import com.erhannis.android.distributeduitest.SatelliteActivity;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import eu.hgross.blaubot.android.BlaubotAndroidFactory;
import eu.hgross.blaubot.core.Blaubot;
import eu.hgross.blaubot.core.IBlaubotDevice;
import eu.hgross.blaubot.core.ILifecycleListener;
import eu.hgross.blaubot.messaging.BlaubotMessage;
import eu.hgross.blaubot.messaging.IBlaubotChannel;
import eu.hgross.blaubot.messaging.IBlaubotMessageListener;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public class StarService extends StackableLocalService<StarService> {
    private static final String TAG = "StarService";

    public static final UUID STAR_NET_UUID = UUID.fromString("e72cd6c5-2c05-42ca-8f77-91d90649c970");

    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    //TODO This is kindof a weird value
    public static int NOTIFICATION = R.string.local_service_started;

    protected static final Kryo mKryo = new Kryo();
    protected Blaubot mBlaubot;
    protected IBlaubotChannel mChannel;

    public StarService() {
        super();
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();

        //TODO Look into security, pairing, etc.
        mBlaubot = BlaubotAndroidFactory.createEthernetBlaubot(STAR_NET_UUID);
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
                //TODO Does this duplicate subscriptions?
                mChannel.subscribe(new IBlaubotMessageListener() {
                    @Override
                    public void onMessage(BlaubotMessage blaubotMessage) {
                        //TODO Split into many channels?
                        try {
                            //System.out.println("Got message: " + new String(blaubotMessage.getPayload()) + " : " + blaubotMessage);
                            Input in = new Input(blaubotMessage.getPayload());
                            Object msg = mKryo.readClassAndObject(in);
                            if (msg instanceof StarMessage) {
                                if (msg instanceof StarMessageToHub) {
                                    for (Consumer<Object> hub : mHubs) {
                                        try {
                                            hub.accept(((StarMessageToHub)msg).payload);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error in hub callback", e);
                                        }
                                    }
                                } else if (msg instanceof StarMessageToSatellite) {
                                    String target = ((StarMessageToSatellite)msg).satellite;
                                    if ((target == null) || getId().equals(target)) {
                                        for (Consumer<Object> satellite : mSatellites) {
                                            try {
                                                satellite.accept(((StarMessageToSatellite)msg).payload);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error in satellite callback", e);
                                            }
                                        }
                                    }
                                } else if (msg instanceof StarMessageBroadcast) {
                                    for (Consumer<Object> hub : mHubs) {
                                        try {
                                            hub.accept(((StarMessageBroadcast)msg).payload);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error in hub callback", e);
                                        }
                                    }
                                    for (Consumer<Object> satellite : mSatellites) {
                                        try {
                                            satellite.accept(((StarMessageBroadcast)msg).payload);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error in satellite callback", e);
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "Unknown StarMessage type!  " + msg);
                                }
                            } else {
                                Log.e(TAG, "Invalid message" + msg);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing message", e);
                        }
                    }
                });
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

        doBindServices();
    }

    @Override
    protected void onAllConnected() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        doUnbindServices();

        mBlaubot.stopBlaubot();

        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    public String getId() {
        return mBlaubot.getOwnDevice().getUniqueDeviceID();
    }

    public List<String> getOtherDevices() {
        return StreamSupport.stream(mBlaubot.getConnectionManager().getConnectedDevices()).map(new Function<IBlaubotDevice, String>() {
            @Override
            public String apply(IBlaubotDevice iBlaubotDevice) {
                return iBlaubotDevice.getUniqueDeviceID();
            }
        }).collect(Collectors.<String>toList());
    }

    public List<String> getAllDevices() {
        List<String> devs = getOtherDevices();
        if (!devs.contains(getId())) {
            devs.add(0, getId());
        }
        return devs;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, FragmentManagerActivity.class), 0); //TODO set

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
        mNM.notify(NOTIFICATION, notification);
    }

    protected final Set<Consumer<Object>> mHubs = Collections.newSetFromMap(new ConcurrentHashMap<Consumer<Object>, Boolean>());
    protected final Set<Consumer<Object>> mSatellites = Collections.newSetFromMap(new ConcurrentHashMap<Consumer<Object>, Boolean>());

    public void registerAsHub(Consumer<Object> msgCallback) {
        //TODO Make sure there isn't already a hub...wait, then why do I have a set
        mHubs.add(msgCallback);
    }

    public void unregisterAsHub(Consumer<Object> msgCallback) {
        mHubs.remove(msgCallback);
    }

    public void registerAsSatellite(Consumer<Object> msgCallback) {
        //TODO Hang on, there's probably only one local satellite, either
        mSatellites.add(msgCallback);
    }

    public void unregisterAsSatellite(Consumer<Object> msgCallback) {
        mSatellites.remove(msgCallback);
    }

    public void sendToHub(Object msg) {
        StarMessageToHub sMsg = new StarMessageToHub(msg);
        broadcast(sMsg);
    }

    //TODO TODO Do
    public Object sendToHubAndWait(Object msg) {
        return null;
    }

    public void sendToSatellites(Object msg) {
        sendToSatellite(null, msg);
    }

    //TODO TODO Do
    public Map<String, Object> sendToSatellitesAndWait(Object msg) {
        return null;
    }

    public void sendToSatellite(String satellite, Object msg) {
        StarMessageToSatellite sMsg = new StarMessageToSatellite(satellite, msg);
        broadcast(sMsg);
    }

    //TODO TODO Do
    public Object sendToSatelliteAndWait(Object satellite, Object msg) {
        return null;
    }

    public void sendToAll(Object msg) {
        StarMessageBroadcast sMsg = new StarMessageBroadcast(msg);
        broadcast(sMsg);
    }

    protected void broadcast(StarMessage sMsg) {
        //TODO Error if no channel?
        if (mChannel != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Output out = new Output(baos);
                mKryo.writeClassAndObject(out, sMsg);
                out.flush();
                out.flush();
                baos.flush();
                baos.close();
                mChannel.publish(baos.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}