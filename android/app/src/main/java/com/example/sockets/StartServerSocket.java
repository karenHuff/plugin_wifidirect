package com.example.sockets;

import android.content.Context;
import android.util.Log;

import java.net.*;
import java.io.*;
import java.lang.*;

import com.example.plugins.WifiDirectPlugin;

public class StartServerSocket extends Thread {
    private static final int PORT = 8881;
    private final Context context;
    private WifiDirectPlugin plugin;

    public StartServerSocket(Context context, WifiDirectPlugin plugin) {
        this.context = context.getApplicationContext();
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            Log.d("server", "Servidor a la escucha en el puerto: " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                Log.d("server", "Cliente conectado");

                DataInputStream dis = new DataInputStream(client.getInputStream());
                String fileName = dis.readUTF();

                if (fileName != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = dis.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }

                    String jsonContent = baos.toString("UTF-8");

                    plugin.onContentJSON(jsonContent);
                }

                dis.close();
                client.close();
            }

        } catch (IOException e) {
            Log.e("server", "Error en el servidor: " + e.getMessage());
        }
    }
}