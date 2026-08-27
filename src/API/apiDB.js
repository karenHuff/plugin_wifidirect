import initDB from "../backend/connection";

async function apiDB(params) {
   try {
      const { method, requestType, body } = params;

      if (!requestType) {
         return { message: 'Faltan datos en la solicitud.' };
      }

      let db = await initDB();

      switch (method) {
         case 'GET':
            if (!body) {
               const data = await db.query(`SELECT * FROM ${requestType}`);
               return { status: 200, data };
            } else {
               const { id } = body;
               const data = await db.query(`SELECT * FROM ${requestType} WHERE id = ${id}`);
               return {status: 200, data};
            }
                     
         case 'POST':
            if (requestType === 'proyecto') {
               return await addProyecto(db, body);
            } else if (requestType === 'patito') {
               return await addPatito(db, body);
            } else {
               return { message: 'Error en la solicitud.' };
            }

         default:
            return { message: 'Método no permititdo.' };
      }
   } catch (error) {
      console.error('Ocurrió un error:', error.message);
   }
}

async function addProyecto (db, body) {
   try {
      const { id_proyecto, nom_proyecto, estatus } = body;
      const insert = "INSERT INTO proyecto(id_proyecto, nom_proyecto, estatus) VALUES (?, ?, ?);";
      await db.run(insert, [id_proyecto, nom_proyecto, estatus]);

      const stmt = "INSERT INTO control(id_proyecto, iden_user, version_pry) VALUES (?, ?, ?);";
      await db.run(stmt, [id_proyecto, '', estatus]);

      return { message: 'Registro creado', status: 200 };
   } catch (error) {
      console.error('Error al insertar el registro:', error);
   }
}

async function addPatito (db, body) {
   try {
      const { id_proyecto, nom_proyecto, estatus } = body;
      const insert = "INSERT INTO proyecto(id_proyecto, nom_proyecto, estatus) VALUES (?, ?, ?);";
      await db.run(insert, [id_proyecto, nom_proyecto, estatus]);

      return { message: 'Registro creado', status: 200 };
   } catch (error) {
      console.error('Error al insertar registro: ', error);
   }
}

export default apiDB;