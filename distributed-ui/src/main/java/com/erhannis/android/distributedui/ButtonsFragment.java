package com.erhannis.android.distributedui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class ButtonsFragment extends Fragment {

  private DistributedUiActivity mListener;

  public ButtonsFragment() {

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_buttons, container, false);
    TextView tvFragmentTextView = (TextView)v.findViewById(R.id.tvFragmentTextView);
    final EditText etFragmentEditText = (EditText)v.findViewById(R.id.etFragmentEditText);
    Button btnFragmentButton = (Button)v.findViewById(R.id.btnFragmentButton);
    btnFragmentButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mListener != null) {
          mListener.sendToHub("buttonClicked", etFragmentEditText.getText().toString());
        }
      }
    });

    return v;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof DistributedUiActivity && ((DistributedUiActivity)context).implementsInterface(ButtonsFragmentCallback.class)) {
      mListener = (DistributedUiActivity) context;
    } else {
      throw new RuntimeException(context.toString() + " must report implementation of ButtonsFragmentCallback");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  public static interface ButtonsFragmentCallback {
    public void buttonClicked(String str);
  }
}
