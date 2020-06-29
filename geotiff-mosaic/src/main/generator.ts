
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'fs'
import { createCanvas, loadImage } from 'canvas'
import { Config } from './models/config'
import { SectionMetadata } from './models/section-metadata'
import { TileQueue } from './tile-queue'
import { TileCache } from './tile-cache'
import { TileDescription } from './tile-description'

export class Generator {
    private MAP_CONFIGURATION_FILE = "./data/config.json"
    private SUBSECTION_META_FILE = "../geotiff-map-exploder/maps/metadata.json"
    private OUTPUT_DIRECTORY = "./output"

    public async generateMosaics(): Promise<void> {
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
            let tileCache = new TileCache()
            let tileQueue = new TileQueue(imageConfiguration.subMaps, subSectionMetadata, tileCache)
    
            // For each zoom level
            for ( let zoom = 0; zoom < tileQueue.getMaxZoom(); zoom++ ) {
                // For each tile to generate, sorted in order of the alphabetical order of dependents
                while ( tileQueue.hasNext(zoom) ) {
                    let tile = tileQueue.pop(zoom);
                    if ( tile === undefined ) {
                        continue
                    }

                    // Compose the tile with world map drawn first, then each section in alphabetical order
                    await this.createTile(tile, tileCache, zoom)
                } 
            }
            tileCache.dispose()
        }
    }

    private async createTile(tile: TileDescription, tileCache: TileCache, zoom: number): Promise<void> {
        // Create a bitmap to draw on
        let canvas = createCanvas(tile.widthPixels, tile.heightPixels)

        // Draw each image onto the bitmap in the correct position
        let context = canvas.getContext("2d")
        for ( let i = 0; i < tile.getNumSubMaps(); i++ ) {
            let subTileExtents = tile.getSubTileDestinations2d(i)
            let subTileImagePaths = tile.getImagePaths(i)
            let subTileExtentSections2d = tile.getSubTileSources2d(i)

            // For each image, load the image off the path and draw it on the parent tile
            for ( let j = 0; j < subTileImagePaths.length; j++ ) {
                let image = await loadImage(subTileImagePaths[j])
                let destinationExtent = subTileExtents[j]
                let sourceExtent2d = subTileExtentSections2d[j]

                context.drawImage(
                    image, 
                    sourceExtent2d.getUpperLeft().x, 
                    sourceExtent2d.getUpperLeft().y, 
                    sourceExtent2d.getLowerRight().x, 
                    sourceExtent2d.getLowerRight().y, 
                    destinationExtent.getUpperLeft().x, 
                    destinationExtent.getUpperLeft().y, 
                    destinationExtent.getLowerRight().x, 
                    destinationExtent.getLowerRight().y)
            }
        }

        // Write the tile out to disk
        let jpegStream = canvas.createJPEGStream()
        if (!existsSync(this.OUTPUT_DIRECTORY)){
            mkdirSync(this.OUTPUT_DIRECTORY);
        }
        let zoomDirectory = `${this.OUTPUT_DIRECTORY}/${tile.zoom}`
        if (!existsSync(zoomDirectory)){
            mkdirSync(zoomDirectory);
        }
        let filePath = `${zoomDirectory}/${tile.x}_${tile.y}.jpg`
        writeFileSync(filePath, jpegStream.read())
    }
}