package com.example.plugins;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.sockets.StartServerSocket;

import java.util.ArrayList;
import java.util.List;

public class WifiDirectReceiver extends BroadcastReceiver {
    private static final String TAG = "wfdReceiver";
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final WifiDirectPlugin plugin;
    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private boolean serverStart = false;

    public WifiDirectReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectPlugin plugin) {
        this.manager = manager;
        this.channel = channel;
        this.plugin = plugin;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                boolean enabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);

                Log.d(TAG, enabled ? "WifiDirect habilitado" : "WifiDirect deshabilitado");
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if (manager != null) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    manager.requestPeers(channel, peerList -> {
                        peers.clear();
                        peers.addAll(peerList.getDeviceList());

                        if (peers.isEmpty()) {
                            Log.d("peers", "No se encontraron dispositivos");
                        }

                        // Enviar lista de peers al plugin
                        plugin.onListPeers(peers);
                    });
                }
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo != null && networkInfo.isConnected()) {
                    Log.d(TAG, "Interfaz P2P asociada, solicitando detalles del grupo...");

                    // Consultar el estado del grupo P2P
                    manager.requestConnectionInfo(channel, info -> {
                        if (info != null && info.groupFormed) {
                            if (info.isGroupOwner) {
                                Log.d(TAG, "Soy el Group Owner");
                                if (!serverStart) {
                                    serverStart = true;
                                    new StartServerSocket(context, plugin).start();
                                }
                            } else {
                                Log.d(TAG, "Preparando cliente");
                                plugin.onResSocket(true);
                            }
                        } else {
                            Log.d(TAG, "El grupo P2P aún no está completamente formado. Esperando...");
                        }
                    });

                } else {
                    Log.d(TAG, "Desconectado del grupo P2P");
                    serverStart = false;
                    plugin.onDisconnected();
                }
                break;
            default:
                Log.d(TAG, "Acción desconocida: " + action);
        }
    }
}
