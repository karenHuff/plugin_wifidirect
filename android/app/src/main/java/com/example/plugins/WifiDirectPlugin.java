package com.example.plugins;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "WifiDirect",
permissions = {
        @Permission(
                alias="wifi",
                strings={ Manifest.permission.NEARBY_WIFI_DEVICES }
        ),
        @Permission(
                alias="location",
                strings={ Manifest.permission.ACCESS_FINE_LOCATION }
        )
})
public class WifiDirectPlugin extends Plugin {
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private IntentFilter intentFilter;
    private static final String TAG = "wfdPlugin";
    private WifiDirectReceiver receiver;
    private WifiDirectConnection connection;

    @Override
    public void load() {
        manager = (WifiP2pManager) getActivity().getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager != null) {
            channel = manager.initialize(getActivity(), getActivity().getMainLooper(), null);

            // Eventos de escucha del broadcastReceiver
            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

            Log.d(TAG, "Plugin inicializado correctamente");
            connection = new WifiDirectConnection(manager, channel, this);

            if (!checkPermissions()) {
                Toast.makeText(getActivity(), "Solicitando permisos", Toast.LENGTH_SHORT).show();
                return;
            }

            checkWiFi();
        } else {
            Log.e(TAG, "WifiP2pManager no disponible");
        }
    }

    /* Métodos del ciclo de vida */
    @Override
    protected void handleOnResume() {
        super.handleOnResume();

        if (manager == null || channel == null) {
            Log.e(TAG, "Manager o Channel no inicializados");
            return;
        }

        if (receiver == null) {
            receiver = new WifiDirectReceiver(manager, channel, this);
            getActivity().registerReceiver(receiver, intentFilter);
            Log.d(TAG, "Receiver registrado correctamente");
        }
    }

    @Override
    protected void handleOnPause() {
        super.handleOnPause();
        try {
            if (receiver != null) {
                getActivity().unregisterReceiver(receiver);
                receiver = null;
                Log.d(TAG, "Receiver desregistrado");
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver ya estaba desregistrado");
        }
    }

    public boolean checkPermissions() {
        List<String> requirePermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && getActivity()
                .checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {

            requirePermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            Log.w("permissions", "Permiso solicitado");
        }

        if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requirePermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            Log.w("permissions", "Permiso de ubicación solicitado");
        }

        if (!requirePermissions.isEmpty()) {
            getActivity().requestPermissions(
                    requirePermissions.toArray(new String[0]), 100);
            return false;
        }

        return true;
    }

    /*  verificar estado de wifi */
    private void checkWiFi() {
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            Toast.makeText(getActivity(), "Activa el Wi-Fi", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Wi-Fi deshabilitado");
        }
    }

    /* Eventos hacia JavaScript */
    public void onPeerConnectionFailed(String message) {
        JSObject ret = new JSObject();
        ret.put("error", message);
        notifyListeners("connectionFailed", ret);
    }

    public void onDisconnected() {
        JSObject ret = new JSObject();
        ret.put("status", "Desconectado del grupo P2P");
        notifyListeners("disconnected", ret);
    }

    public void onResSocket(Boolean flag) {
        JSObject ret = new JSObject();
        ret.put("status", flag);
        notifyListeners("socket", ret);
    }

    // Enviar lista de dispositivos al front
    public void onListPeers(List<WifiP2pDevice> peers) {
        List<JSObject> peerList = new ArrayList<>();
        JSObject ret = new JSObject();

        for (WifiP2pDevice device : peers) {
            JSObject jsObject = new JSObject();
            jsObject.put("deviceName", device.deviceName);
            jsObject.put("deviceAddress", device.deviceAddress);
            jsObject.put("status", device.status);
            peerList.add(jsObject);
        }

        ret.put("listPeers", peerList);
        notifyListeners("listPeers", ret);
    }

    public void onContentJSON(String contentJSON) {
        JSObject ret = new JSObject();
        ret.put("file", contentJSON);
        notifyListeners("file", ret);
    }

    /* Plugin methods */
    @PluginMethod
    public void startDiscovery(PluginCall call) {
        connection.startDiscovery(call);
    }

    @PluginMethod
    public void connectTo(PluginCall call) {
        connection.connectTo(call);
    }

    @PluginMethod
    public void startTransfer(PluginCall call) {
        connection.startTransfer(call);
    }

    @PluginMethod
    public void closeConnection(PluginCall call) {
        connection.closeConnection(call);
    }
}
