import { useEffect, useState } from 'react';
import './App.css';
import { WifiDirect } from './capacitor-wifiDirect';

function App() {
  const [devices, setDevices] = useState([]);
  const [resDis, setResDis] = useState()
  const [data, setData] = useState([]);

  useEffect(() => {
    // Lista de dispositivos encontrados
    WifiDirect.addListener('listPeers', (info) => {
      const obj = JSON.parse(info.listPeers);
      setDevices(obj);
    });

    // Resultado de conexión
    WifiDirect.addListener('connected', info => {
      console.log('Dispositivo conectado ', info);
    });

    // Desconexión de dispositvos
    WifiDirect.addListener('disconnected', (info) => {
      console.log('Desconectado del grupo P2P:', info);

      // Reiniciar búsqueda automáticamente
      try {
        console.log('Reiniciando descubrimiento de peers...');
        WifiDirect.startDiscovery();
      } catch (error) {
        console.error('Error reiniciando descubrimiento:', error);
      }
    });

    // Gestión de errores
    WifiDirect.addListener('connectionFailed', (info) => {
      console.error('Error de conexión:', info.error);
    });

    // Eventos de escucha para cliente y servidor
    WifiDirect.addListener('socket', async (info) => {
      if (info.status) {
        await handleSendFile();
      }
    });

    // Resultado transferencia de archivo
    WifiDirect.addListener('file', async (info) => {
      // Insertar registros en la base de datos local
      await stringToJSON(info);
    });

    return () => {
      WifiDirect.removeAllListeners();
    };
  }, []);

  // Iniciar descubrimiento de peers
  const startDiscovery = async () => {
    try {
      console.log('Iniciando descubrimiento de peers...');
      const result = await WifiDirect.startDiscovery();
      console.log(result.status);
      setResDis(true);
    } catch (error) {
      console.error('Error iniciando descubrimiento:', error);
    }
  };

  // Conectar dispositivo seleccionado
  const handleConnetDevice = (device) => {
    WifiDirect.connectTo({ deviceAddress: device });
  }

  const handleSendFile = async () => {
    try {
      // Recuperar datos de la consulta

      // Iniciar transferencia de archivos
      //const resTransfer = await WifiDirect.startTransfer({ file: file.path });
      //console.log("Estado del servidor", resTransfer.server);
    } catch (error) {
      console.error("Error al abrir archivo:", error.message);
    }
  };

  // Desconectar dispostivos
  const handleCloseConnection = () => {
    WifiDirect.closeConnection()
      .then(info => console.log(info))
      .catch(error => console.log(error));
  }

  const stringToJSON = async (info) => {
    try {
      let data = JSON.parse(info.file);
      const values = data.tables[0].values;

      for (const data of values) {
        const config = {
          method: 'POST',
          requestType: 'patito',
          body: {
            id_proyecto: data[0],
            nom_proyecto: data[1],
            estatus: '0'
          }
        }

        // Insertar registros en la base de datos
        const result = await addRegistro(config);
        if (result.status === 200) {
          // actualizar estado
          setData(prevData => [...prevData, {
            id_proyecto: data[0],
            nom_proyecto: data[1],
            estatus: '0'
          }]);
          console.log(result.message);
        }
      }
    } catch (error) {
      console.error('Ocurrió un error: ', error);
    }
  }

  return (
    <div className="App">
      <h3>Prueba de Conexión Wi-Fi Direct</h3>

      <button onClick={async () => startDiscovery()}>Iniciar búsqueda</button>

      {resDis && (
        <>
          <h4>Dispositivos encontrados:</h4>
          {devices.map(row => {
            const isAvailable = row.status !== 0;
            return (
              <div
                key={row.deviceAddress}
                onClick={isAvailable ? () => handleConnetDevice(row.deviceAddress) : undefined}
                onDoubleClick={handleCloseConnection}
              >
                <h5>device: {row.deviceName}</h5>
                <h5>deviceAddress: {row.deviceAddress}</h5>
                <h5>status: {row.status}</h5>
              </div>
            )
          })}
        </>
      )}
    </div>
  );
}

export default App;
