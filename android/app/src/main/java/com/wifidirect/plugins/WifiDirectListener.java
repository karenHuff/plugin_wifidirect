package com.wifidirect.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.*;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WifiDirectListener extends BroadcastReceiver {

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final WifiDirectPlugin plugin;
    private final List<WifiP2pDevice> peers = new ArrayList<>();

    public WifiDirectListener(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectPlugin plugin) {
        this.manager = manager;
        this.channel = channel;
        this.plugin = plugin;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                manager.requestPeers(channel, peerList -> {
                    peers.clear();
                    peers.addAll(peerList.getDeviceList());
                    
                    // Mostrar lista de dispostivos disponibles
                    plugin.onListPeers(peers);
                });
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    Log.d("WifiDirect", "Conectado al grupo P2P");
                } else {
                    Log.d("WifiDirect", "Desconectado del grupo P2P");
                    plugin.onDisconnected();
                    //isConnecting = false;
                }
                break;
        }
    }
}