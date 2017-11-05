package com.erhannis.android.distributeduitest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.erhannis.android.distributeduitest.starnetwork.StarService;

import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java8.util.function.BiConsumer;
import java8.util.function.Consumer;

public class FragmentManagerActivity extends AppCompatActivity {
  private static final String TAG = "FragmentManagerActivity";

  protected RecyclerView rvFragments;
  protected FragmentManagerAdapter mFragmentManagerAdapter;

  //<editor-fold desc="Bind service">
  private boolean mIsBound = false;
  private UiMovementService mBoundService;

  private final ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mBoundService = ((UiMovementService.LocalBinder)service).getService();
      mFragmentManagerAdapter.updateData(mBoundService.getFragmentLocations(), mBoundService.getLocations());
      Log.d(TAG, "Connected to UiMovementService");
    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundService = null;
      Log.d(TAG, "Disconnected from UiMovementService");
    }
  };

  void doBindService() {
    boolean bound = bindService(new Intent(FragmentManagerActivity.this, UiMovementService.class), mConnection, Context.BIND_AUTO_CREATE);
    System.out.println("bound: " + bound);
    mIsBound = true;
  }

  void doUnbindService() {
    if (mIsBound) {
      unbindService(mConnection);
      mIsBound = false;
    }
  }
  //</editor-fold>

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_fragment_manager);
    rvFragments = (RecyclerView) findViewById(R.id.rvFragments);

    mFragmentManagerAdapter = new FragmentManagerAdapter(new ListOrderedMap<FragmentHandle, String>(), new ArrayList<String>(), new BiConsumer<FragmentHandle, String>() {
      @Override
      public void accept(FragmentHandle fragmentHandle, String s) {
        if (mIsBound) {
          mBoundService.relocateFragment(fragmentHandle, s);
        }
      }
    });
    rvFragments.setAdapter(mFragmentManagerAdapter);

    doBindService();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    doUnbindService();
  }

  public static class FragmentManagerAdapter extends RecyclerView.Adapter<FragmentManagerAdapter.ViewHolder> {
    private ListOrderedMap<FragmentHandle, String> mDataset;
    private ArrayList<String> mLocations;
    private final BiConsumer<FragmentHandle, String> mOnLocationSelected;

    public static class ViewHolder extends RecyclerView.ViewHolder {
      public TextView tvFragmentName;
      public Spinner spinLocation;

      public ViewHolder(LinearLayout v, TextView tvFragmentName, Spinner spinLocation) {
        super(v);
        this.tvFragmentName = tvFragmentName;
        this.spinLocation = spinLocation;
      }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public FragmentManagerAdapter(ListOrderedMap<FragmentHandle, String> myDataset, List<String> locations, BiConsumer<FragmentHandle, String> onLocationSelected) {
      mDataset = new ListOrderedMap<FragmentHandle, String>();
      mDataset.putAll(myDataset);
      mLocations = new ArrayList<String>(locations);
      mOnLocationSelected = onLocationSelected;
    }

    public synchronized void updateData(ListOrderedMap<FragmentHandle, String> myDataset, List<String> locations) {
      mDataset = new ListOrderedMap<FragmentHandle, String>();
      mDataset.putAll(myDataset);
      mLocations = new ArrayList<String>(locations);
      this.notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public synchronized FragmentManagerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      LinearLayout v = (LinearLayout)LayoutInflater.from(parent.getContext()).inflate(R.layout.snippet_fragment_location, parent, false);
      TextView tvFragmentName = (TextView)v.findViewById(R.id.tvFragmentName);
      Spinner spinLocation = (Spinner)v.findViewById(R.id.spinLocation);
      ArrayAdapter<CharSequence> adapter = new ArrayAdapter(parent.getContext(), android.R.layout.simple_spinner_item, mLocations);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      spinLocation.setAdapter(adapter);
      ViewHolder vh = new ViewHolder(v, tvFragmentName, spinLocation);
      return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public synchronized void onBindViewHolder(ViewHolder holder, int position) {
      //TODO Need to re-set spinner adapter here?
      final FragmentHandle fh = mDataset.get(position);
      holder.tvFragmentName.setText(fh.name);
      String loc = mDataset.getValue(position);
      holder.spinLocation.setOnItemSelectedListener(null);
      //TODO Not sure if this works
      holder.spinLocation.setSelection(-1);
      for (int i = 0; i < holder.spinLocation.getCount(); i++) {
        if (loc != null && loc.equals(holder.spinLocation.getItemAtPosition(i))) {
          holder.spinLocation.setSelection(i);
          break;
        }
      }
      holder.spinLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
          mOnLocationSelected.accept(fh, (String)parent.getItemAtPosition(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
          mOnLocationSelected.accept(fh, null);
        }
      });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public synchronized int getItemCount() {
      return mDataset.size();
    }
  }
}
