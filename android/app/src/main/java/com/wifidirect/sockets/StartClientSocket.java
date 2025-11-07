package com.wifidirect.sockets;

import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import android.database.Cursor;
import android.provider.OpenableColumns;

import java.io.*;
import java.net.*;

public class StartClientSocket extends Thread {
   public static final int PORT = 8881;
   //public static final int TIMEOUT = 5000;

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
      try {
         Socket socket = new Socket(hostAddress, PORT);
         Log.d("socket", "Conectado al servidor...");

         if (filePath == null) {
            Log.e("Client", "selecciona un archivo");
            return;
         }

         Uri fileUri = Uri.parse(filePath);
         ContentResolver cr = context.getContentResolver();

         try (InputStream inputStream = cr.openInputStream(fileUri);
              DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            if (inputStream == null) {
               Log.e("Client", "No se pudo abrir el archivo desde URI: " + filePath);
               return;
            }

            String fileName = getFileName(fileUri);
            dos.writeUTF(fileName);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
               dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
            Log.d("Client", "Archivo enviado: " + fileName);
         }

         socket.close();
      } catch (IOException e) {
         Log.e("Client", "Error en el cliente: " + e.getMessage());
      }
   }

   private String getFileName(Uri uri) {
      String result = null;
      if ("content".equals(uri.getScheme())) {
         try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
               int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
               result = cursor.getString(nameIndex);
            }
         }
      }

      if (result == null) {
         result = uri.getLastPathSegment();
      }

      return result;
   }
}