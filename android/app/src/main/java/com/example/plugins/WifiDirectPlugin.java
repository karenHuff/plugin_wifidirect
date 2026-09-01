package com.example.plugins;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

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
                strings={ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }
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
        manager = (WifiP2pManager) getContext().getSystemService(Context.WIFI_P2P_SERVICE);

        if (manager == null) return;

        channel = manager.initialize(getContext(), getContext().getMainLooper(), null);

        // Eventos de escucha del broadcastReceiver
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        Log.d(TAG, "Plugin inicializado correctamente");
        connection = new WifiDirectConnection(manager, channel, this);

        checkWiFi();
    }

    /* Métodos del ciclo de vida */
    @Override
    protected void handleOnResume() {
        super.handleOnResume();
        if (manager != null && receiver == null) {
            receiver = new WifiDirectReceiver(manager, channel, this);
            getContext().registerReceiver(receiver, intentFilter);

            Log.d(TAG, "Receiver registrado correctamente");
        }
    }

    @Override
    protected void handleOnPause() {
        super.handleOnPause();
        if (receiver != null) {
            try {
                getContext().unregisterReceiver(receiver);
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "Receiver no registrado previo a pause" + ex.getMessage());
            }

            receiver = null;
        }
    }

    public boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return getPermissionState("wifi") == PermissionState.GRANTED;
        }
        return getPermissionState("location") == PermissionState.GRANTED;
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

    /* Listeners emitidos hacia JavaScript */
    public void onListPeers(List<WifiP2pDevice> peers) {
        List<JSObject> peerList = new ArrayList<>();
        for (WifiP2pDevice device : peers) {
            JSObject jsObject = new JSObject();
            jsObject.put("deviceName", device.deviceName);
            jsObject.put("deviceAddress", device.deviceAddress);
            jsObject.put("status", device.status);
            peerList.add(jsObject);
        }

        JSObject ret = new JSObject();
        ret.put("listPeers", peerList);
        notifyListeners("listPeers", ret);
    }

    public void onClientStarted() {
        JSObject ret = new JSObject();
        ret.put("status", true);
        notifyListeners("isClient", ret);
    }

    public void onContentJSON(String contentJSON) {
        JSObject ret = new JSObject();
        ret.put("file", contentJSON);
        notifyListeners("file", ret);
    }

    public void onDisconnected() {
        JSObject ret = new JSObject();
        ret.put("status", "Desconectado del grupo P2P");
        notifyListeners("disconnected", ret);
    }

    /* Plugin methods */
    @PluginMethod
    public void startDiscovery(PluginCall call) {
        if (!checkPermissions()) {
            requestAllPermissions(call, "permissionCallback");
            return;
        }
        connection.startDiscovery(call);
    }

    @PluginMethod
    public void connectTo(PluginCall call) {
        if (!checkPermissions()) {
            requestAllPermissions(call, "permissionCallback");
            return;
        }
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

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        if (checkPermissions()) {
            String methodName = call.getMethodName();
            if ("startDiscovery".equals(methodName)) {
                startDiscovery(call);
            } else if ("connectTo".equals(methodName)) {
                connectTo(call);
            }
        } else {
            call.reject("Permisos de ubicación o Wi-Fi denegados");
        }
    }
}
