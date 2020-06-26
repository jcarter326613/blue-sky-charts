'use strict';

import { Generator } from './generator'

export const handler = async () => {
    let generator = new Generator()
    generator.generateMosaics()
}