package com.example.plugins;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.WpsInfo;
import android.util.Log;

import com.example.sockets.StartClientSocket;
import com.getcapacitor.PluginCall;
import com.getcapacitor.JSObject;

public class WifiDirectConnection {
    private static final String TAG = "wfdConnection";
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final WifiDirectPlugin plugin;

    public WifiDirectConnection(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectPlugin plugin) {
        this.manager = manager;
        this.channel = channel;
        this.plugin = plugin;
    }

    @SuppressWarnings("MissingPermission")
    public void startDiscovery(PluginCall call) {
        if (manager == null || channel == null) {
            call.reject("WifiP2pManager", "Manager no inicializado");
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
                call.reject("error", "Fallo al iniciar descubrimiento: " + getReasonText(reason));
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    public void connectTo(PluginCall call) {
        String deviceAddress = call.getString("deviceAddress");

        if (deviceAddress == null || deviceAddress.isEmpty()) {
            call.reject("Dirección del dispositivo no proporcionada");
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15; // prioridad para ser propietario del grupo

        Log.d(TAG, "Intentando conectar a: " + deviceAddress);

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                JSObject ret = new JSObject();
                ret.put("deviceAddress", deviceAddress);
                call.resolve(ret);
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Fallo al conectar" + reason);
                call.reject("Fallo al conectar con: " + deviceAddress + ":" + getReasonText(reason));
            }
        });
    }

    public void startTransfer(PluginCall call) {
        String filePath = call.getString("file");

        if (filePath == null || filePath.isEmpty()) {
            Log.e("WifiDirect", "Ruta de archivo no proporcionada");
            call.reject("Ruta de archivo no proprcionada");
            return;
        }

        Log.d("transfer", "Iniciando transferencia");

        manager.requestConnectionInfo(channel, info -> {
            if (info.groupFormed && !info.isGroupOwner) {
                String hostAddres = info.groupOwnerAddress.getHostAddress();
                // iniciar cliente
                StartClientSocket client = new StartClientSocket(plugin.getContext(), filePath, hostAddres);
                client.start();

                JSObject ret = new JSObject();
                ret.put("status", "Iniciando transferencia");
                call.resolve(ret);
            } else {
                call.reject("El dispositivo no está conectado como cliente P2P");
            }
        });
    }

    public void closeConnection(PluginCall call) {
        if (manager == null || channel == null) return;

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Eliminando grupo");
                JSObject ret = new JSObject();
                ret.put("status", "Grupo P2P cerrado correctamente");
                call.resolve(ret);
            }

            @Override
            public void onFailure(int reason) {
                Log.e("closeConnection", "Error al intentar eliminar el GO: " + reason);
                call.reject("Error al cerrar el grupo P2P:" + getReasonText(reason));
            }
        });
    }

    private String getReasonText(int reason) {
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED: return "P2P No Soportado";
            case WifiP2pManager.BUSY: return "Sistema Ocupado";
            case WifiP2pManager.ERROR: return "Error interno";
            default: return "Razón desocnocida (" + reason + ")";
        }
    }
}
