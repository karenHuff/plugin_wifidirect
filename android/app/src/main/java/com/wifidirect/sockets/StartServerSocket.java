package com.wifidirect.sockets;

import android.os.Build;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.provider.MediaStore;
import android.util.Log;
import android.net.Uri;

import java.net.*;
import java.io.*;
import java.lang.*;

public class StartServerSocket extends Thread {
   private static final int PORT = 8881;
   private final Context context;
   public boolean statusFalg = false;

   public StartServerSocket(Context context) {
      this.context = context;
   }

   @Override
   public void run() {
      try (ServerSocket serverSocket = new ServerSocket(PORT)) {
         Log.d("socket", "Servidor a la escucha en el puerto: " + PORT);
         Socket client = serverSocket.accept();
         Log.d("socket", "Cliente conectado");

         DataInputStream dis = new DataInputStream(client.getInputStream());
         String fileName = dis.readUTF();

         if (fileName != null && !fileName.contains("..")) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            ContentResolver resolver = context.getContentResolver();
            Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri fileUri = resolver.insert(collection, values);

            if (fileUri != null) {
               try (OutputStream out = resolver.openOutputStream(fileUri)) {
                  byte[] buffer = new byte[4096];
                  int bytesRead;
                  while ((bytesRead = dis.read(buffer)) != -1) {
                     out.write(buffer, 0, bytesRead);
                  }
                  out.flush();
               }

               values.clear();
               values.put(MediaStore.Downloads.IS_PENDING, 0);
               resolver.update(fileUri, values, null, null);

               statusFalg = true; // indicar si se recibió el archivo
               Log.d("socket", "Archivo recibido correctamente: " + fileName);
               
               //Toast.makeText(context, "Archivo recibido", Toast.LENGTH_SHORT).show();
            } else {
               Log.e("server", "No se pudo crear el archivo en MediaStore");
            }
         } else {
            Log.e("server", "Nombre de archivo inválido o nulo");
         }

         dis.close();
         client.close();

      } catch (IOException e) {
         Log.e("server", "Error en el servidor: " + e.getMessage());
      }
   }
}
