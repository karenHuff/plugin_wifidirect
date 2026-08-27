import { Capacitor } from "@capacitor/core";
import {
   CapacitorSQLite,
   SQLiteConnection
} from "@capacitor-community/sqlite";
import { Preferences } from '@capacitor/preferences';

let sqlite = null;
let db = null;
const NAME_DB = "prueba"; // Nombre de la base de datos
const VERSION_DB = "1"; // Versión de la base de datos

async function initDB() {
   try {
      if (!sqlite) {
         sqlite = new SQLiteConnection(CapacitorSQLite);
      }

      const platform = Capacitor.getPlatform();

      if (platform !== 'android') {
         console.log("No puedo funcionar aquí");
         return;
      }
      
      const { value: versions } = await Preferences.get({ key: 'dbCopied' });

      // Copiar base de datos desde assets solo una vez
      if (versions !== VERSION_DB) {
         await sqlite.copyFromAssets();
         await Preferences.set({ key: 'dbCopied', value: VERSION_DB });
         console.log("Base de datos copiada desde assets.");
      }
      
      // Verificar si ya existe la conexion
      const isConn = (await sqlite.isConnection(NAME_DB, false)).result;
      if (isConn) {
         await sqlite.closeConnection(NAME_DB, false);
      }

      // Crear conexión a la base de datos
      db = await sqlite.createConnection(NAME_DB, false, 'no-encryption', 1);
      await db.open();

      // Verificar que se haya establecido la conexión
      const isOpen = (await db.isDBOpen()).result;
      if (isOpen) {
         console.log('Conexión establecida.');
         return db;
      }      
   } catch (error) {
      console.error('Ocurrió un error:', error);
   }
}

export default initDB;