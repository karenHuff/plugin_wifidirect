package com.example.plugins;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.WpsInfo;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.sockets.StartClientSocket;
import com.getcapacitor.PluginCall;
import com.getcapacitor.JSObject;

public class WifiDirectConnection {
    private static final String TAG = "wfdPlugin";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final WifiDirectPlugin plugin;

    public WifiDirectConnection(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectPlugin plugin) {
        this.manager = manager;
        this.channel = channel;
        this.plugin = plugin;
    }
    
    public void startDiscovery(PluginCall call) {
        if (manager == null || channel == null) {
            call.reject("WifiP2pManager", "Manager no inicializado");
            return;
        }

        if (ActivityCompat.checkSelfPermission(plugin.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(plugin.getContext(), Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                JSObject ret = new JSObject();
                ret.put("status", "Descubrimiento iniciado correctamente");
                call.resolve(ret);
            }

            @Override
            public void onFailure(int reason) {
                call.reject("error", "Fallo al iniciar descubrimiento: " + reason);
            }
        });
    }

    public void connectTo(PluginCall call) {
        String deviceAddress = call.getString("deviceAddress");

        if (deviceAddress == null || deviceAddress.isEmpty()) {
            call.reject("Dirección del dispositivo no proporcionada");
            return;
        }

        WifiP2pGroup group = new WifiP2pGroup();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            group.getNetworkId();
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        // prioridad para ser propietario del grupo
        config.groupOwnerIntent = 15;

        Log.d(TAG, "Intentando conectar a: " + deviceAddress);

        if (ActivityCompat.checkSelfPermission(plugin.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(plugin.getContext(), Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                JSObject ret = new JSObject();
                Log.d(TAG, "Solicitud de conexión enviada a: " + deviceAddress);
                ret.put("deviceAddress", deviceAddress);
                call.resolve(ret);
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Fallo al conectar" + reason);
                plugin.onPeerConnectionFailed("Error al conectar: " + reason);
                call.reject("Error al conectar: " + reason);
            }
        });
    }

    public void startTransfer(PluginCall call) {
        String filePath = call.getString("file");

        if (filePath == null || filePath.isEmpty()) {
            Log.e("WifiDirect", "Ruta de archivo no proporcionada");
            return;
        }

        Log.d("trsnafer", "Iniciando transferencia");

        try {
            manager.requestConnectionInfo(channel, info -> {
                if (info.groupFormed && !info.isGroupOwner) {
                    String hostAddress = info.groupOwnerAddress.getHostAddress();
                    // iniciar cliente
                    new StartClientSocket(plugin.getContext(), filePath, hostAddress).start();
                } else {
                    Log.e("Error", "Intentado ser servidor...");
                }
            });
        } catch (Exception ex) {
            Log.e("Error", "Ocurrió un error" + ex.getMessage());
        }
    }

    public void closeConnection(PluginCall call) {
        if (manager == null || channel == null) return;

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Eliminando grupo");
                JSObject ret = new JSObject();
                ret.put("status", "Grupo eliminado correctamente");
                call.resolve(ret);
            }

            @Override
            public void onFailure(int reason) {
                Log.e("closeConnection", "Error al intentar eliminar el GO: " + reason);
                call.reject("Erro al eliminar grupo" + reason);
            }
        });
    }
}
