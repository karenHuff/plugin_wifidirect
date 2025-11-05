package com.wifidirect.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

import com.wifidirect.plugins.WifiDirectPlugin;

public class MainActivity extends BridgeActivity {
   @Override
   public void onCreate(Bundle savedInstanceState) {
      registerPlugin(WifiDirectPlugin.class);
      super.onCreate(savedInstanceState);
   }
}
