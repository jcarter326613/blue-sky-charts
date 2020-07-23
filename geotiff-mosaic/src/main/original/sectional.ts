'use strict';

import { Loader } from './loader'
import { MapType } from './map-type'

export const handler = async () => {
    let loader = new Loader(MapType.Sectional)
    await loader.syncChangedMaps()
}