import { useEffect, useState } from "react"
import { Directory, Filesystem } from '@capacitor/filesystem';
import { Capacitor } from '@capacitor/core';
import WifiDirect from './plugins/wifidirectPlugin';
import { getControl } from "./API/crud";

function App() {
  const [devices, setDevices] = useState([]);
  const [resDis, setResDis] = useState(false);
  const getPlatform = Capacitor.getPlatform();

  useEffect(() => {
    if (getPlatform !== 'android') {
      console.log("No soy android :(");
      return;
    }

    // Lista de dispositivos encontrados
    WifiDirect.addListener('listPeers', (info) => {
      const obj = JSON.parse(info.listPeers);
      setDevices(obj);
    });

    // Desconexión de dispositvos
    WifiDirect.addListener('disconnected', async (info) => {
      console.log('Desconectado del grupo P2P:', info);

      // Reiniciar búsqueda automáticamente
      try {
        console.log('Reiniciando descubrimiento de peers...');
        await WifiDirect.startDiscovery();
      } catch (error) {
        console.error('Error reiniciando descubrimiento:', error);
      }
    });

    // Eventos de escucha para client
    WifiDirect.addListener('isClient', async (info) => {
      if (info.status) {
        await handleSendFile();
      }
    });

    // Resultado transferencia de archivo
    WifiDirect.addListener('file', async (info) => {
      // leer contenido del archivo
      alert("archivo recibido");
      alert("leyendo contenido del archivo recibido: " + info.file);

      handleCloseConnection();
    });

    return () => {
      WifiDirect.remove();
    };
  }, []);

  // Iniciar descubrimiento de peers
  const startDiscovery = async () => {
    try {
      await WifiDirect.startDiscovery();
      setResDis(true);
    } catch (error) {
      console.error('Error iniciando descubrimiento:', error);
    }
  };

  const handleConnectDevice = async (device) => {
    await WifiDirect.connectTo({ deviceAddress: device });
  }

  const handleSendFile = async () => {
    try {
      const response = await getControl();

      let cadData = response.map(row =>
        Object.keys(row).map(item => row[item])
      );

      const dataJson = {
        "values": cadData
      };

      const data = JSON.stringify(dataJson, null, 2);

      await Filesystem.writeFile({
        path: 'archivo.json',
        data: data,
        directory: Directory.Library,
        encoding: 'utf8'
      });

      const readFile = await Filesystem.getUri({
        path: 'archivo.json',
        directory: Directory.Library
      });

      const resTransfer = await WifiDirect.startTransfer({ file: readFile.uri });
      console.log("Estado del servidor", resTransfer.server);
    } catch (error) {
      console.error("Error al abrir archivo:", error.message);
    }
  };

  // Desconectar dispostivos
  const handleCloseConnection = async () => {
    await WifiDirect.closeConnection()
  }

  return (
    <div className="App">
      <h3>Prueba de Conexión Wi-Fi Direct</h3>

      <button onClick={async () => startDiscovery()}>Iniciar descubrimiento</button>

      {resDis && (
        <>
          <h4>Dispositivos encontrados:</h4>
          {devices.map(row => {
            const isAvailable = row.status !== 0;
            return (
              <div
                key={row.deviceAddress}
                onClick={isAvailable ? async () => handleConnectDevice(row.deviceAddress) : null}
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

export default App