import { useEffect, useState } from 'react';
import './App.css';
import { WifiDirect } from './capacitor-wifiDirect';
import { FilePicker } from '@capawesome/capacitor-file-picker';
import { Directory, Encoding, Filesystem } from '@capacitor/filesystem';

function App() {
  const [devices, setDevices] = useState([]);
  const [connected, setConnected] = useState(null);
  const [client, setClient] = useState(false);
  const [uriDB, setUriDB] = useState(null);

  useEffect(() => {
    // Iniciar descubrimiento de peers
    const start = async () => {
      try {
        console.log('Iniciando descubrimiento de peers...');
        const result = await WifiDirect.startDiscovery();
        console.log('Resultado del descubrimiento:', result.status);
      } catch (error) {
        console.error('Error iniciando descubrimiento:', error);
      }
    };

    start();

    // Resultado de descubrimiento
    WifiDirect.addListener('discovering', (info) => {
      console.log('Descubriendo dispositivos...', info);
    });

    // Resultado de conexión
    WifiDirect.addListener('connected', info => {
      console.log('Dispositivo conectado ', info);
    });

    // Desconexión de dispositvos
    WifiDirect.addListener('disconnected', (info) => {
      console.log('Desconectado del grupo P2P:', info);
      setConnected(null);
      
      // Reiniciar búsqueda automáticamente
      try {
        console.log('Reiniciando descubrimiento de peers...');
        WifiDirect.startDiscovery();
      } catch (error) {
        console.error('Error reiniciando descubrimiento:', error);
      }
    });

    // Lista de dispositivos encontrados
    WifiDirect.addListener('listPeers', (info) => {
      console.log("peers: ", info.listPeers);
      const obj = JSON.parse(info.listPeers);
      console.log(obj);
      setDevices(obj);

      const connect = obj.find(device => device.status === 0);
      if (connect) {
        setConnected(connect);
      }
    });

    // Gestión de errores
    WifiDirect.addListener('connectionFailed', (info) => {
      console.error('Error de conexión:', info.error);
    });

    WifiDirect.addListener('error', (info) => {
      console.error('Error general:', info.error);
    });
    // Fin

    // Resultado cerrar conexión
    WifiDirect.addListener('closeConnection', (info) => {
      console.log('Cerrando conexión entre dispositivos', info);
    });

    //Resultado de eliminar grupo
    WifiDirect.addListener('groupRemoved', (info) => {
      console.log("Eliminando grupo... ", info);
    });
    
    // Eventos de escucha para cliente y servidor
    WifiDirect.addListener('socket', (info) => {
      console.log('socket info... ', info);

      if (info.status) {
        setClient(true);
      }
    });

    // Resultado transferencia de archivo
    WifiDirect.addListener('file', (info) => {    
      alert("Archivo recibido");
      console.log('recibiendo uri: ', info);
      volcadoDatos(info.file);
    });

    return () => {
      WifiDirect.removeAllListeners();
    };
  }, []);

  // Conectar dispositivo seleccionado
  const handleConnetDevice = (device) => {
    WifiDirect.connectTo({ deviceAddress: device });
  }

  const handleOpenFile = async () => {
    try {
      // Abrir explorador de archivos
      const result = await FilePicker.pickFiles({
        types: ['/*']
      });
      const file = result.files[0];

      console.log("Iniciando conexión para transferir");
      // Iniciar transferencia de archivos
      const resTransfer = await WifiDirect.startTransfer({ file: file.path });
      console.log("Estado del servidor", resTransfer.server);
    } catch (error) {
      console.error("Error al abrir archivo:", error.message);
    }
  };

  // Desconectar dispostivos
  const handleCloseConnection = () => {
    WifiDirect.closeConnection()
      .then(info => console.log(info))
      .catch(error => console.log(error));
    setConnected(null);
    setClient(false);
  }

  const volcadoDatos = async (uri) => {
   
    /*fetch(uri)
      .then(data => {
        if (!data) {
          console.warn("error al recuperar archivo");
        }
        return data.json();
      })
      .then(data => {
        console.log(data.prueba);
      })
      .catch(error => {
        console.error("Ocurrió un error: ", error);
      });*/
  }

  return (
    <div className="App">
      <h3>Prueba de Conexión Wi-Fi Direct</h3>

      {devices.length > 0 ? (
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
      ) : (
        <h4>Buscando dispositivos...</h4>
      )}

      {connected !== null && client && (
        // Mostrar botón al dipositivo cliente
        <div onClick={() => handleOpenFile()}> transferir </div>
      )}
    </div>
  );
}

export default App;