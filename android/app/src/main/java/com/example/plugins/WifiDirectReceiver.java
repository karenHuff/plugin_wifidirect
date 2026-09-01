package com.example.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.example.sockets.StartServerSocket;

import java.util.ArrayList;
import java.util.List;

public class WifiDirectReceiver extends BroadcastReceiver {
    private static final String TAG = "wfdReceiver";
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final WifiDirectPlugin plugin;

    public WifiDirectReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectPlugin plugin) {
        this.manager = manager;
        this.channel = channel;
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                boolean enabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);

                Log.d(TAG, enabled ? "WifiDirect habilitado" : "WifiDirect deshabilitado");
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if (manager != null) {
                    manager.requestPeers(channel, peerList -> {
                        List<WifiP2pDevice> devices = new ArrayList<>(peerList.getDeviceList());
                        // Enviar lista de peers al plugin
                        plugin.onListPeers(devices);
                    });
                }
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                StartServerSocket server = new StartServerSocket(plugin.getContext(), plugin);
                if (networkInfo != null && networkInfo.isConnected()) {
                    // Consultar el estado del grupo P2P
                    manager.requestConnectionInfo(channel, info -> {
                        if (info != null && info.groupFormed) {
                            if (info.isGroupOwner) {
                                Log.d(TAG, "Soy el Group Owner");
                                // iniciar servidor

                                server.start();
                            } else {
                                Log.d(TAG, "Preparando cliente");
                                plugin.onClientStarted();
                            }
                        } else {
                            Log.d(TAG, "El grupo P2P aún no está completamente formado. Esperando...");
                        }
                    });
                } else {
                    Log.d(TAG, "Desconectado del grupo P2P");
                    server.stop();
                    plugin.onDisconnected();
                }
                break;
            default:
                Log.d(TAG, "Acción desconocida: " + action);
        }
    }
}
