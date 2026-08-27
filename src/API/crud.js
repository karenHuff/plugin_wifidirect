import apiDB from './apiDB';

export const getProyecto = async () => {
   try {
      const config = {
         method: 'GET',
         requestType: 'proyecto'
      }

      const data = await apiDB(config);
      return data.data.values;
   } catch (error) {
      console.error(error);
   }
}

export const getIDproyecto = async () => {
   try {
      const config = {
         method: 'GET',
         requestType: 'proyecto',
         body: { id: 1 }
      }

      const data = await apiDB(config);
      return data.data.values;
   } catch (error) {
      console.error(error);
   }
}

export const getControl = async () => {
   try {
      const config = {
         method: 'GET',
         requestType: 'control'
      }

      const response = await apiDB(config);
      return response.data.values;
   } catch (error) {
      console.error(error);
   }
}

export const addRegistro = async (config) => {
   try {
      const data = await apiDB(config);
      return data;
   } catch (error) {
      console.error(error);
   }
}

export const insertData = async (dataPry, data, setData) => {
   try {
      if (dataPry.length === 0) {
         return;
      }

      const config = {
         method: 'POST',
         requestType: 'proyecto',
         body: dataPry[0]
      }

      // Si no  hay registros
      if (data.length === 0) {
         const response = await addRegistro(config);
         setData(prevData => [...prevData, dataPry[0]]);
         console.log(response.message);
      } else {
         // Verificar que no exista el registro en la tabla principal
         const id_pry = dataPry[0].id_proyecto;
         const result = data.filter(row => row.id_proyecto === id_pry);
         
         if (result.length === 0) {
            const response = await addRegistro(config);
            setData(prevData => [...prevData, dataPry[0]]);
            console.log(response.message);
         }
      }
   } catch (error) {
      console.error('Ocurrió un error:', error);
   }
}