'use strict';

import { Loader } from './loader'

export const handler = async () => {
    let loader = new Loader()
    await loader.syncChangedMaps()
}