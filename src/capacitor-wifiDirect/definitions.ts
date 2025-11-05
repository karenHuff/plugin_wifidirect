export interface WifiDirectPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  startDiscovery(): Promise<{ value: string }>;
}