export interface WifiDirectPlugin {
  startDiscovery(): Promise<{ value: string }>;
  startTransfer(options: { file: string, device: string}): Promise<{ value: string }>;
  startServerSocket(): Promise<{ value: string }>;
  connectTo(options: { deviceAddress: string }): Promise<{ value: string }>;
  closeConnection(): Promise<{ value: string }>;
}