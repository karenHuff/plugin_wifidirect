package com.example.sockets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.plugins.WifiDirectPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StartServerSocket {
    private static final String TAG = "StartServerSocket";
    public static final int PORT = 8881;

    private final WeakReference<Context> contextRef;
    private final WeakReference<WifiDirectPlugin> pluginRef;

    private ExecutorService executor;
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;

    public StartServerSocket(Context context, WifiDirectPlugin plugin) {
        this.contextRef = new WeakReference<>(context.getApplicationContext());
        this.pluginRef = new WeakReference<>(plugin);
    }

    public synchronized void start() {
        if (isRunning) {
            Log.w(TAG, "El servidor ya se encuentra en ejecución.");
            return;
        }

        isRunning = true;
        executor = Executors.newSingleThreadExecutor();
        executor.execute(this::runServer);
    }

    private void runServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            Log.d(TAG, "Servidor a la escucha en el puerto: " + PORT);

            while (isRunning && !Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                Log.d(TAG, "Cliente conectado desde: " + client.getInetAddress().getHostAddress());

                handleClientConnection(client);
            }
        } catch (IOException e) {
            if (isRunning) {
                Log.e(TAG, "Error en el servidor: " + e.getMessage(), e);
            } else {
                Log.d(TAG, "ServerSocket cerrado de manera limpia.");
            }
        } finally {
            stop();
        }
    }

    private void handleClientConnection(Socket client) {
        try (DataInputStream dis = new DataInputStream(client.getInputStream())) {

            // 1. Leer la longitud del nombre
            short nameLengthShort = dis.readShort();
            int nameLength = nameLengthShort & 0xFFFF; // Mascarar para evitar valores negativos de short

            // 2. Leer el nombre del archivo
            byte[] nameBytes = new byte[nameLength];
            dis.readFully(nameBytes);
            String fileName = new String(nameBytes, StandardCharsets.UTF_8);

            // 3. Leer el tamaño del archivo
            long fileSize = dis.readLong();

            Log.d(TAG, "Recibiendo archivo: " + fileName + " | Tamaño: " + fileSize + " bytes");

            // 4. Leer exactamente fileSize bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            long totalRead = 0;
            int bytesRead;

            while (totalRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                baos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }

            String jsonContent = baos.toString("UTF-8");
            Log.d(TAG, "Contenido JSON recibido exitosamente.");

            // 5. Enviar el resultado al Plugin de Capacitor
            WifiDirectPlugin plugin = pluginRef.get();
            if (plugin != null) {
                new Handler(Looper.getMainLooper()).post(() -> plugin.onContentJSON(jsonContent));
            }

        } catch (IOException e) {
            Log.e(TAG, "Error procesando los datos recibidos del cliente: " + e.getMessage(), e);
        } finally {
            try {
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException ignored) {}
        }
    }

    public synchronized void stop() {
        isRunning = false;

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error al cerrar ServerSocket: " + e.getMessage());
            }
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        Log.d(TAG, "Servidor Socket detenido.");
    }
}