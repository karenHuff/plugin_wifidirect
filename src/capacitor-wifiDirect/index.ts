import { registerPlugin } from '@capacitor/core';

import type { WifiDirectPlugin } from './definitions';

const WifiDirect = registerPlugin<WifiDirectPlugin>('WifiDirect', {
  web: () => import('./web').then((m) => new m.WifiDirectPluginWeb()),
});

export * from './definitions';
export { WifiDirect };