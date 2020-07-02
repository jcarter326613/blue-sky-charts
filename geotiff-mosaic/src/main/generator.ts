
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'fs'
import { ChangeSet } from './models/change-set'
import { execSync } from 'child_process'
import { exit } from "process"
import { createCanvas, loadImage } from 'canvas'
import { Config } from './models/config'
import { SectionVersion } from './models/section-version'
import { SectionVersionList } from './models/section-version-list'
import { TileQueue } from './tile-queue'
import { TileCache } from './tile-cache'
import { TileDescription } from './tile-description'
import { BoxGeo, CoordinateConversion, PointWebMercator, BoxWebMercator, PointGeo } from 'coordinates'
import { Conversion } from './models/conversion'
import { MetadataManager } from './metadata-manager'

export class Generator {
    private MAP_CONFIGURATION_FILE = "./data/config.json"
    private SUBSECTION_META_FILE = "../geotiff-map-exploder/maps/metadata.json"
    private OUTPUT_DIRECTORY = "./output"

    public async generateMosaics(): Promise<void> {
        // Clean the output directory
        if (existsSync(this.OUTPUT_DIRECTORY)) {
            execSync(`rm -rf ${this.OUTPUT_DIRECTORY}`)
        }
        mkdirSync(this.OUTPUT_DIRECTORY);

        let metadataManager = new MetadataManager()
        let mosaicVersion = (new Date()).toISOString().replace(/\..+/, "").replace(":", "-").replace(":", "-")

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
        let subSectionVersions: Record<string, SectionVersionList> = JSON.parse(rawdata.toString())
        let subSectionMetadata = metadataManager.extractCurrentVersions(subSectionVersions)

        // For each mosaic image to make
        let newSubSectionMetadata: Record<string, SectionVersionList> = {}
        for ( let imageConfigurationName in configuration.sections ) {
            let imageConfiguration = configuration.sections[imageConfigurationName]
            if ( imageConfiguration.subMaps === undefined ) {
                continue
            }

            // Figure out the max zoom
            let maxZoom: number | undefined
            let mosaicExtentsMercator = this.getMosaicExtentsMercator(imageConfiguration.subMaps, subSectionMetadata)
            for ( let subTileName of imageConfiguration.subMaps ) {
                // Get the max resolution of the sub map
                let metadata = subSectionMetadata[subTileName]
                if ( metadata.imageHeight === undefined || metadata.imageWidth === undefined || 
                    metadata.tileWidth === undefined || metadata.maxZoom === undefined || metadata.fileExtent === undefined) {
                    continue
                }
                let extentsBoxGeo = Conversion.convertFileExtentToBoxGeo(metadata.fileExtent)
                if ( extentsBoxGeo === undefined ) {
                    continue
                }
                let extentsMercator = CoordinateConversion.convertBoxGeoToBoxMercator(extentsBoxGeo)
                let multiplier = 1
                if ( metadata.imageHeight > metadata.imageWidth ) {
                    multiplier = metadata.imageWidth / metadata.imageHeight
                }
                let maxZoomPixelWidth = metadata.tileWidth * multiplier * (2 ** metadata.maxZoom)
                let maxZoomResolution = maxZoomPixelWidth / extentsMercator.getWidth()
                
                // Get the corresponding zoom for the large tile
                let targetPixelWidth = maxZoomResolution * mosaicExtentsMercator.getWidth()
                let targetZoom = Math.ceil(Math.log2(targetPixelWidth / TileDescription.TILE_DIMENSIONS_PIXELS))
                if ( maxZoom === undefined || maxZoom > targetZoom ) {
                    maxZoom = targetZoom
                }
            }
            if ( maxZoom === undefined ) {
                continue
            }

            // Start setting up the metadata for the mosaic tile
            let newSectionData = new SectionVersion()
            newSectionData.maxZoom = maxZoom
            newSectionData.tileWidth = TileDescription.TILE_DIMENSIONS_PIXELS
            newSectionData.version = mosaicVersion
            let changeSet: Record<string, ChangeSet> = {}

            // For each zoom level
            for ( let zoom = 0; zoom <= maxZoom; zoom++ ) {
                console.log(`Starting zoom level ${zoom}`)

                // Create a lookup of all the tiles to generate and which sections are needed to create them
                let tileCache = new TileCache()
                let tileQueue = new TileQueue(imageConfiguration.subMaps, subSectionMetadata, metadataManager, tileCache, zoom)

                // Update the changeset
                let newChangeSet = tileQueue.getChangeSet()
                for ( let effectiveDate of Object.keys(newChangeSet) ) {
                    if ( !(effectiveDate in changeSet) ) {
                        changeSet[effectiveDate] = new ChangeSet()
                        changeSet[effectiveDate].tiles = []
                    }
                    let newTiles = newChangeSet[effectiveDate].tiles
                    let oldTiles = changeSet[effectiveDate].tiles
                    if ( oldTiles === undefined ) {
                        oldTiles = {}
                        changeSet[effectiveDate].tiles = oldTiles
                    }
                    if ( newTiles != undefined ) {
                        for ( let zoom of Object.keys(newTiles) ) {
                            let zoomNum = parseInt(zoom)
                            if ( !(zoomNum in oldTiles) ) {
                                oldTiles[zoomNum] = {}
                            }
                            
                            for ( let x of Object.keys(newTiles[zoomNum])) {
                                let xNum = parseInt(x)
                                if ( !(xNum in oldTiles[zoomNum]) ) {
                                    oldTiles[zoomNum][xNum] = []
                                }
                                for ( let y of newTiles[zoomNum][xNum] ) {
                                    let hasY = false
                                    for ( let oldY of oldTiles[zoomNum][xNum] ) {
                                        if ( oldY == y ) {
                                            hasY = true
                                            break
                                        }
                                    }
                                    if ( !hasY ) {
                                        oldTiles[zoomNum][xNum].push(y)
                                    }
                                }
                            }
                        }
                    }
                }
    
                // For each tile to generate, sorted in order of the alphabetical order of dependents
                while ( tileQueue.hasNext() ) {
                    let tile = tileQueue.pop();
                    if ( tile === undefined ) {
                        continue
                    }
                    if ( zoom == 0 ) {
                        newSectionData.fileExtent = 
                            Conversion.convertBoxGeoToFileExtent(CoordinateConversion.convertBoxMercatorToBoxGeo(tile.tileExtent))

                        newSectionData.imageWidth = tile.tileExtent.getWidth() * maxZoom
                        newSectionData.imageHeight = tile.tileExtent.getHeight() * maxZoom
                    }

                    // Compose the tile with world map drawn first, then each section in alphabetical order
                    await this.createTile(tile, imageConfigurationName, mosaicVersion)
                }

                tileCache.dispose()
            }

            // Save the metadata for this new tile
            if ( newSectionData.changeSet === undefined ) {
                newSectionData.changeSet = {}
            }
            for ( let effectiveDate of Object.keys(changeSet) ) {
                if ( !(effectiveDate in newSectionData.changeSet) ) {
                    newSectionData.changeSet[effectiveDate] = changeSet[effectiveDate]
                }
            }

            let versionList = new SectionVersionList()
            versionList.versions = {}
            versionList.versions[newSectionData.version] = newSectionData
            newSubSectionMetadata[imageConfigurationName] = versionList
        }

        // Write out the new metadata
        let newMetadataString = JSON.stringify(newSubSectionMetadata)
        writeFileSync("./output/metadata.json", newMetadataString)
        writeFileSync("./data/metadata.json", newMetadataString)
    }

    private getMosaicExtentsMercator(imageConfiguration: string[], subSectionMetadata: Record<string, SectionVersion>): BoxWebMercator {
        let startLatitude: number | undefined
        let endLatitude: number | undefined
        let startLongtude: number | undefined
        let endLongtude: number | undefined
        for ( let sectionName of imageConfiguration ) {
            let sectionMetadata = subSectionMetadata[sectionName]

            if ( sectionMetadata === undefined || sectionMetadata.fileExtent === undefined ||
                sectionMetadata.fileExtent.topLeft === undefined || 
                sectionMetadata.fileExtent.topLeft.latitude === undefined || sectionMetadata.fileExtent.topLeft.longitude === undefined ||
                sectionMetadata.fileExtent.bottomRight === undefined || 
                sectionMetadata.fileExtent.bottomRight.latitude === undefined || sectionMetadata.fileExtent.bottomRight.longitude === undefined ) {
                console.error(`Error loading map ${sectionName} from metadata.json`)
                exit(1)
            }

            if ( endLatitude === undefined || sectionMetadata.fileExtent.topLeft.latitude > endLatitude ) {
                endLatitude = sectionMetadata.fileExtent.topLeft.latitude
            }
            if ( startLatitude === undefined || sectionMetadata.fileExtent.bottomRight.latitude < startLatitude ) {
                startLatitude = sectionMetadata.fileExtent.bottomRight.latitude
            }
            if ( endLongtude === undefined || sectionMetadata.fileExtent.bottomRight.longitude > endLongtude ) {
                endLongtude = sectionMetadata.fileExtent.bottomRight.longitude
            }
            if ( startLongtude === undefined || sectionMetadata.fileExtent.topLeft.longitude < startLongtude ) {
                startLongtude = sectionMetadata.fileExtent.topLeft.longitude
            }
        }

        return CoordinateConversion.convertBoxGeoToBoxMercator(new BoxGeo(
            new PointGeo(startLongtude, endLatitude), new PointGeo(endLongtude, startLatitude)
        ))
    }

    private async createTile(tile: TileDescription, mapName: string, mapVersion: string): Promise<void> {
        // Create a bitmap to draw on
        let canvas = createCanvas(tile.widthPixels, tile.heightPixels)
        let context = canvas.getContext("2d")

        // Draw the background onto it
        let shadowImage = await loadImage("./data/world-shadow.png") // TODO: replace the world shadow with another image, preferably higher resolution
        let entireAreaBoxGeo = new BoxGeo(
            new PointGeo(CoordinateConversion.MIN_LONGITUDE, CoordinateConversion.MAX_LATITUDE),
            new PointGeo(CoordinateConversion.MAX_LONGITUDE, CoordinateConversion.MIN_LATITUDE))
        let entireAreaBoxMercator = CoordinateConversion.convertBoxGeoToBoxMercator(entireAreaBoxGeo)
        let entireAreaBox2d = CoordinateConversion.convertBoxMercatorToBox2d(entireAreaBoxMercator)
        let tileExtent2d = CoordinateConversion.convertBoxMercatorToBox2d(tile.tileExtent)
        let percentageMultiplier = shadowImage.width / entireAreaBoxMercator.getWidth()

        context.drawImage(shadowImage, 
            (tileExtent2d.getUpperLeft().x - entireAreaBox2d.getUpperLeft().x) * percentageMultiplier,
            (tileExtent2d.getUpperLeft().y - entireAreaBox2d.getUpperLeft().y) * percentageMultiplier,
            tileExtent2d.getWidth() * percentageMultiplier,
            tileExtent2d.getHeight() * percentageMultiplier,
            0, 0, tile.widthPixels, tile.heightPixels)

        // Draw each image onto the bitmap in the correct position
        for ( let i = 0; i < tile.getNumSubMaps(); i++ ) {
            let subTileExtents = tile.getSubTileDestinations2d(i)
            let subTileImagePaths = tile.getImagePaths(i)

            // For each image, load the image off the path and draw it on the parent tile
            for ( let j = 0; j < subTileImagePaths.length; j++ ) {
                let image = await loadImage(subTileImagePaths[j])
                let destinationExtent2d = subTileExtents[j]

                context.drawImage(
                    image, 
                    0, 0, image.naturalWidth, image.naturalHeight,
                    Math.floor(destinationExtent2d.getUpperLeft().x), 
                    Math.floor(destinationExtent2d.getUpperLeft().y), 
                    Math.ceil(destinationExtent2d.getWidth()), 
                    Math.ceil(destinationExtent2d.getHeight()))
            }
        }

        // Write the tile out to disk
        if (!existsSync(this.OUTPUT_DIRECTORY)){
            mkdirSync(this.OUTPUT_DIRECTORY);
        }
        let areaDirectory = `${this.OUTPUT_DIRECTORY}/${mapName}`
        if (!existsSync(areaDirectory)){
            mkdirSync(areaDirectory);
        }
        let versionDirectory = `${areaDirectory}/${mapVersion}`
        if (!existsSync(versionDirectory)){
            mkdirSync(versionDirectory);
        }
        let zoomDirectory = `${versionDirectory}/${tile.zoom}`
        if (!existsSync(zoomDirectory)){
            mkdirSync(zoomDirectory);
        }
        let pngPath = `${zoomDirectory}/${tile.x}_${tile.y}.png`
        let stream = canvas.toBuffer()
        writeFileSync(pngPath, stream)

        // Convert the png to a jpeg and clean up the png
        let jpegPath = `${zoomDirectory}/${tile.x}_${tile.y}.jpg`
        execSync(`convert ${pngPath} -quality 90 ${jpegPath}`)
        execSync(`rm ${pngPath}`)
    }
}