package com.erhannis.android.distributeduitest;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MainActivity extends AppCompatActivity implements ButtonsFragment.ButtonsFragmentCallback {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.btnMoveFragment).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
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
    });
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
    System.out.println("(MainActivity).buttonClicked(): " + s);
  }
}
