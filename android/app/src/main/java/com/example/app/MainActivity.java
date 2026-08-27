package com.example.app;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;
import com.example.plugins.EchoPlugin;
import com.example.plugins.WifiDirectPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(EchoPlugin.class);
        registerPlugin(WifiDirectPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
