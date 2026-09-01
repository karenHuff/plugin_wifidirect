package com.example.sockets;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StartClientSocket {
    private static final String TAG = "StartClientSocket";
    public static final int PORT = 8881;
    private static final int TIMEOUT_MS = 5000;
    private final WeakReference<Context> contextRef;
    private final String filePath;
    private final String hostAddress;

    private ExecutorService executor;

    public StartClientSocket(Context context, String filePath, String hostAddress) {
        this.contextRef = new WeakReference<>(context.getApplicationContext());
        this.filePath = filePath;
        this.hostAddress = hostAddress;
    }

    public synchronized void start() {
        if (filePath == null || filePath.trim().isEmpty()) {
            Log.e(TAG, "No se ha proporcionado una ruta de archivo válida.");
            return;
        }

        executor = Executors.newSingleThreadExecutor();
        executor.execute(this::runClient);
    }

    private void runClient() {
        Context context = contextRef.get();
        if (context == null) {
            Log.e(TAG, "El contexto de la aplicación ya no está disponible.");
            return;
        }

        Uri fileUri = Uri.parse(filePath);
        ContentResolver cr = context.getContentResolver();

        String fileName = getFileName(cr, fileUri);
        long fileSize = getFileSize(cr, fileUri);

        if (fileName == null || fileSize <= 0) {
            Log.e(TAG, "No se pudo obtener el nombre o tamaño del archivo a enviar.");
            return;
        }

        // Socket con timeout explícito
        try (Socket socket = new Socket()) {
            socket.bind(null);
            socket.connect(new InetSocketAddress(hostAddress, PORT), TIMEOUT_MS);
            Log.d(TAG, "Conectado al servidor GO: " + hostAddress + ":" + PORT);

            try (InputStream inputStream = cr.openInputStream(fileUri);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                if (inputStream == null) {
                    Log.e(TAG, "No se pudo abrir el InputStream para la URI: " + filePath);
                    return;
                }

                byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);

                // 1. Escribir la longitud del nombre (2 bytes)
                dos.writeShort((short) nameBytes.length);

                // 2. Escribir los bytes del nombre
                dos.write(nameBytes);

                // 3. Escribir el tamaño total del archivo (8 bytes)
                dos.writeLong(fileSize);

                // 4. Escribir el contenido del archivo
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalSent = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                }

                dos.flush();
                Log.d(TAG, "Archivo " + fileName + " enviado exitosamente (" + totalSent + " bytes).");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error de red/I/O en el cliente socket: " + e.getMessage(), e);
        } finally {
            shutdownExecutor();
        }
    }

    private synchronized void shutdownExecutor() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /* Métodos Auxiliares de Metadatos */
    private String getFileName(ContentResolver cr, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = cr.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error consultando el nombre en ContentResolver: " + ex.getMessage());
            }
        }

        if (result == null && uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    private long getFileSize(ContentResolver cr, Uri uri) {
        long size = 0;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = cr.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index != -1 && !cursor.isNull(index)) {
                        size = cursor.getLong(index);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error consultando tamaño en ContentResolver: " + e.getMessage());
            }

            if (size <= 0) {
                try (android.content.res.AssetFileDescriptor afd = cr.openAssetFileDescriptor(uri, "r")) {
                    if (afd != null) {
                        size = afd.getLength();
                    }
                } catch (Exception ignored) {}
            }
        }

        if (size <= 0 && uri.getPath() != null) {
            try {
                File file = new File(uri.getPath());
                if (file.exists()) {
                    size = file.length();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error consultando el archivo en sistema de archivos: " + e.getMessage());
            }
        }

        return size;
    }
}