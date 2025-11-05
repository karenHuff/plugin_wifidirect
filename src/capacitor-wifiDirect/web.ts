import { WebPlugin } from '@capacitor/core';

import type { WifiDirectPlugin } from './definitions';

export class WifiDirectPluginWeb extends WebPlugin implements WifiDirectPlugin {
   async echo(options: { value: string }): Promise<{ value: string }> {
      console.log('ECHO', options);
      return options;
   }
}