package com.erhannis.android.distributedui;

import android.support.v4.app.Fragment;

import java8.util.Objects;

/**
 * Created by erhannis on 10/14/17.
 */

public class FragmentHandle {
  public final Class<? extends Fragment> clazz;
  public final String name;

  public FragmentHandle() {
    this.clazz = null;
    this.name = null;
  }

  public FragmentHandle(Class<? extends Fragment> clazz, String name) {
    this.clazz = clazz;
    this.name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof FragmentHandle)) {
      return false;
    }
    return Objects.equals(this.clazz, ((FragmentHandle)obj).clazz) && Objects.equals(this.name, ((FragmentHandle)obj).name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.clazz, this.name);
  }
}
