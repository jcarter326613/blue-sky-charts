
import { readFileSync } from 'fs'
import { createCanvas } from 'canvas'
import { BoxGeo, CoordinateConversion, PointGeo, Point2d, BoxWebMercator } from 'coordinates'
import { Config } from './models/config'
import { FileExtent } from './models/file-extent'
import { exit } from 'process'
import { SectionMetadata } from './models/section-metadata'
import { TileQueue } from './tile-queue'
import { TileCache } from './tile-cache'
import { TileDescription } from './tile-description'

export class Generator {
    private TILE_DIMENSIONS_PIXELS = 1024
    private MAP_CONFIGURATION_FILE = "./data/config.json"
    private SUBSECTION_META_FILE = "../geotiff-map-exploder/maps/metadata.json"

    public generateMosaics(): void {
        // Load the configuration
        let mapConfigurationFile = this.MAP_CONFIGURATION_FILE
        let rawdata = readFileSync(mapConfigurationFile)
        let configuration: Config = JSON.parse(rawdata.toString())
    
        if ( configuration.sections === undefined ) {
            console.log("No sections to load")
            return
        }
    
        // Load the map subsection metadata
        let subsectionMetaFile = this.SUBSECTION_META_FILE
        rawdata = readFileSync(subsectionMetaFile)
        let subSectionMetadata: Record<string, SectionMetadata> = JSON.parse(rawdata.toString())
        
        // For each mosaic image to make
        for ( let imageConfigurationName in configuration.sections ) {
            let imageConfiguration = configuration.sections[imageConfigurationName]
            if ( imageConfiguration.subMaps === undefined ) {
                continue
            }
    
            // Create a lookup of all the tiles to generate and which sections are needed to create them
            let tileQueue = new TileQueue(imageConfiguration.subMaps, subSectionMetadata)
    
            // Track the max number of dependents for any tile and setup a disk cache which caps out at that many maps exploded on disk
 
            // For each zoom level
            let tileCache = new TileCache()
            for ( let zoom = 0; zoom < tileQueue.getMaxZoom(); zoom++ ) {
                // For each tile to generate, sorted in order of the alphabetical order of dependents
                while ( tileQueue.hasNext(zoom) ) {
                    let tile = tileQueue.pop(zoom);

                    // Ensure the correct tiles have been generated
                    tileCache.generate(tile.getDependencyList(), zoom)

                    // Compose the tile with world map drawn first, then each section in alphabetical order
                    this.createTile(tile, tileCache)
                } 
            }
            tileCache.dispose()
        }
    }

    private createTile(tile: TileDescription, tileCache: TileCache): void {
        // Get the ordered list of image files we need to load
        //let tilePartList: Array<TileDescription> = []
        //for 
        //let tileExtent = tile.getExtent()

        // Create a bitmap to draw on
        let canvas = createCanvas(tile.widthPixels, tile.heightPixels)

        // Draw each image onto the bitmap in the correct position
        canvas.getContext("2d").drawImage()

        // Write the tile out to disk
    }
}