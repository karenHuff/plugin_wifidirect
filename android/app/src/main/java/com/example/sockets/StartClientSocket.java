package com.example.sockets;

import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import android.database.Cursor;
import android.provider.OpenableColumns;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class StartClientSocket extends Thread {
    public static final int PORT = 8881;

    private final Context context;
    private final String filePath;
    private final String hostAddress;

    public StartClientSocket(Context context, String filePath, String hostAddress) {
        this.context = context.getApplicationContext();
        this.filePath = filePath;
        this.hostAddress = hostAddress;
    }

    @Override
    public void run() {
        if (filePath == null) {
            Log.e("file", "Selecciona un archivo");
            return;
        }

        try (Socket socket = new Socket(hostAddress, PORT)){
            Log.d("socket", "Conectado al servidor...");

            Uri fileUri = Uri.parse(filePath);
            ContentResolver cr = context.getContentResolver();

            // obtener nombre del archivo
            String fileName = getFileName(cr, fileUri);
            long fileSize = getFileSize(cr, fileUri);

            try (InputStream inputStream = cr.openInputStream(fileUri);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                if (inputStream == null) {
                    Log.e("file", "No se pudo abrir el archivo desde URI: " + filePath);
                    return;
                }

                byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
                int nameLength = nameBytes.length;

                // longitud del nombre
                dos.writeShort((short) nameLength);

                // nombre del archivo
                dos.write(nameBytes);

                // tamaño del archivo
                dos.writeLong(fileSize);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long total = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    total += bytesRead;
                }

                dos.flush();
                Log.d("client", "Archivo enviado: " + filePath);
            }
        } catch (IOException e) {
            Log.e("client", "Error en el cliente: " + e.getMessage());
        }
    }

    /* Métodos auxiliares */
    private String getFileName(ContentResolver cr, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try {
                Cursor cursor = cr.query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ex) {
                Log.d("Error", "Ocurrió un error" + ex.getMessage());
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
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
                Log.e("client", "Error consultando tamaño en ContentResolver: " + e.getMessage());
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
                Log.e("client", "Error consultando el archivo físico: " + e.getMessage());
            }
        }

        return size;
    }
}