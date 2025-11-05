import { useEffect, useState } from 'react';
import './App.css';
import { WifiDirect } from './capacitor-wifiDirect';

function App() {
  const [devices, setDevices] = useState([]);

  useEffect(() => {
    WifiDirect.addListener('discovering', (info) => {
      console.log('Descubriendo dispositivos...', info);
    });

    WifiDirect.addListener('disconnected', (info) => {
      console.log('Desconectado del grupo P2P:', info);
    });

    WifiDirect.addListener('connectionFailed', (info) => {
      console.error('Error de conexión:', info.error);
    });

    WifiDirect.addListener('error', (info) => {
      console.error('Error general:', info.error);
    });

    WifiDirect.addListener('listPeers', (info) => {
      console.log("peers: ", info.listPeers);
      const obj = JSON.parse(info.listPeers);
      setDevices(obj);
    });

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

    return () => {
      WifiDirect.removeAllListeners();
    };
  }, []);

  const handleConnetDevice = (device) => {
    WifiDirect.connectTo({ deviceAddress: device })
      .then((info) => {
        console.log('connect', info.connected);
      })
      .catch((error) => 
        console.error('Error: ', error.connectionFailed)
      );
  }

  return (
    <div className="App">
      <h3>Prueba de Conexión Wi-Fi Direct</h3>

      {devices.length > 0 ? devices.map(row => (
        <>
          <h4>Dispositivos encontrados: </h4>

          <div onClick={() => handleConnetDevice(row.deviceAddress)}>
            <h5>device: {row.deviceName} </h5>
            <h5>deviceAddress: {row.deviceAddress} </h5>
            <h5>status: {row.status} </h5>
          </div>
        </>
      )) : (
        <h4>Buscando dispositivos...</h4>
      )}
    </div>
  );
}

export default App;