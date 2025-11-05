package com.wifidirect.plugins;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.*;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "WifiDirect")
public class WifiDirectPlugin extends Plugin {

   private WifiP2pManager manager;
   private WifiP2pManager.Channel channel;
   private WifiDirectListener receiver;
   private IntentFilter intentFilter;
   private JSObject ret = new JSObject();
   private boolean isConnecting = false; // evita reconexiones múltiples

   @Override
   public void load() {
      manager = (WifiP2pManager) getActivity().getSystemService(Context.WIFI_P2P_SERVICE);
      if (manager != null) {
         channel = manager.initialize(getActivity(), getActivity().getMainLooper(), null);

         intentFilter = new IntentFilter();
         intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
         intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
         intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
         intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

         Log.d("WifiDirect", "Plugin inicializado correctamente");

         // Verificar permiso antes de seguir
         if (!checkPermissions()) {
            Log.w("WifiDirect", "Permisos no concedidos todavía");
            Toast.makeText(getActivity(), "permisos no concedidos", Toast.LENGTH_SHORT).show();
            return;
         }

         // Verificar si Wi-Fi está habilitado
         WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext()
               .getSystemService(Context.WIFI_SERVICE);
         if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            Toast.makeText(getActivity(), "Activa el Wi-Fi", Toast.LENGTH_SHORT).show();
            return;
         }

         // Veificar si Wi-Fi direct está habilitado

      } else {
         Log.e("WifiDirect", "WifiP2pManager no disponible");
      }
   }

   @Override
   protected void handleOnResume() {
      super.handleOnResume();

      if (manager == null || channel == null) {
         Log.e("WifiDirect", "Manager o Channel no inicializados");
         return;
      }

      if (receiver == null) {
         receiver = new WifiDirectListener(manager, channel, this);
         getActivity().registerReceiver(receiver, intentFilter);
         Log.d("WifiDirect", "Receiver registrado correctamente");
      }
   }

   @Override
   protected void handleOnPause() {
      super.handleOnPause();
      try {
         if (receiver != null) {
            getActivity().unregisterReceiver(receiver);
            receiver = null;
            Log.d("WifiDirect", "Receiver desregistrado");
         }
      } catch (IllegalArgumentException e) {
         Log.w("WifiDirect", "Receiver registrado");
      }
   }

   // Método para verificar permisos de ubicación
   private boolean checkPermissions() {
      List<String> requirePermissions = new ArrayList<>();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
         if (getActivity()
               .checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            requirePermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);

            Log.w("WifiDirect", "Permiso concedido");
         }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
         if (getActivity()
               .checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requirePermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

            Log.w("WifiDirect", "Permiso de ubicación solicitado");
         }
      }

      if (!requirePermissions.isEmpty()) {
         getActivity().requestPermissions(
               requirePermissions.toArray(new String[0]), 100);
         return false;
      }

      return true;
   }

   // Método principal llamado desde JavaScript
   @PluginMethod
   public void startDiscovery(PluginCall call) {
      if (manager == null || channel == null) {
         ret.put("WifiP2pManager", "Manager no inicializado");
         call.resolve(ret);
         return;
      }

      manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
         @Override
         public void onSuccess() {
            ret.put("status", "Descubrimiento iniciado correctamente");
            notifyListeners("discovering", ret);
            call.resolve(ret);
         }

         @Override
         public void onFailure(int reason) {
            ret.put("error", "Fallo al iniciar descubrimiento: " + reason);
            notifyListeners("error", ret);
            call.reject(ret.toString());
         }
      });
   }

   // Plugin para conectar dispositivo seleccionado
   @PluginMethod
   public void connectTo(PluginCall call) {
      isConnecting = true;
      String deviceAddress = call.getString("deviceAddress");
      // onPeerConnectionFailed("data: " + deviceAddress);

      WifiP2pConfig config = new WifiP2pConfig();
      config.deviceAddress = deviceAddress;
      config.wps.setup = WpsInfo.PBC;

      Log.d("WifiDirect", "Intentando conectar a: " + deviceAddress);

      manager.connect(channel, config, new WifiP2pManager.ActionListener() {
         @Override
         public void onSuccess() {
            Log.d("WifiDirect", "Conexión iniciada con: " + deviceAddress);
            ret.put("deviceAddress", deviceAddress);
            notifyListeners("connected", ret);
         }

         @Override
         public void onFailure(int reason) {
            Log.e("WifiDirect", "Fallo al conectar" + reason);
            onPeerConnectionFailed("Error al conectar: " + reason);
            isConnecting = false;
         }
      });
   }

   // Eventos hacia JavaScript
   public void onPeerConnected(WifiP2pDevice device) {
      ret.put("deviceName", device.deviceName);
      ret.put("deviceAddress", device.deviceAddress);
      notifyListeners("connected", ret);
   }

   public void onPeerConnectionFailed(String message) {
      ret.put("error", message);
      notifyListeners("connectionFailed", ret);
   }

   public void onDisconnected() {
      isConnecting = false;
      ret.put("status", "Desconectado del grupo P2P");
      notifyListeners("disconnected", ret);
   }

   public void onListPeers(List<WifiP2pDevice> peers) {
      List<JSObject> peerList = new ArrayList<>();

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
}