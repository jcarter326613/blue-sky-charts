
import { BoxGeo, CoordinateConversion, PointGeo, Point2d, BoxWebMercator } from 'coordinates'
import { FileExtent } from './models/file-extent'
import { exit } from 'process'
import { Heap } from 'ts-heap'
import { SectionMetadata } from './models/section-metadata'
import { TileDescription } from './tile-description'
import { SubMapDescription } from './sub-map-description'
import { TileCache } from './tile-cache'

export class TileQueue {
    private queue: Array<Heap<TileDescription>>

    public constructor (maps: Array<string>, metadata: Record<string, SectionMetadata>, tileCache: TileCache) {
        this.queue = []

        // Load all the configs for each map and determine the mosaic rectangular extents
        let extents = this.getExtents(maps, metadata)
        let extentsMercator = CoordinateConversion.convertBoxGeoToBoxMercator(extents)

        // Get the max zoom we need to be able to go to to show a full resolution tile of all maps
        let maxZoom = this.getMaxZoom()
    
        // Create the list of tiles needed to cover that extent
        let currentZoom = 0
        while ( currentZoom <= maxZoom ) {
            let tilesAcross = 2 ** currentZoom

            this.queue[currentZoom] =
                new Heap<TileDescription>((a: TileDescription, b: TileDescription) => {
                    let aMaps = a.getDependencyList()
                    let bMaps = b.getDependencyList()

                    let aKey = aMaps.join("|")
                    let bKey = bMaps.join("|")

                    return aKey.localeCompare(bKey)
                })
            
            // For each tile
            for ( let x = 0; x < tilesAcross; x++ ) {
                for ( let y = 0; y < tilesAcross; y++ ) {
                    // Get the mercator extents for this zoom level
                    let tileExtent = new BoxWebMercator(
                        extentsMercator.getTopLeft().x + x * (extentsMercator.getWidth() / tilesAcross), 
                        extentsMercator.getTopLeft().y + y * (extentsMercator.getHeight() / tilesAcross),
                        extentsMercator.getTopLeft().x + (x+1) * (extentsMercator.getWidth() / tilesAcross),
                        extentsMercator.getTopLeft().y + (y+1) * (extentsMercator.getHeight() / tilesAcross))

                    // Get the sections that overlap that extent
                    let overlaps: Array<SubMapDescription> = []
                    for ( let sectionName in metadata ) {
                        let section = metadata[sectionName]
                        if ( section.fileExtent === undefined || section.maxZoom === undefined || section.tileWidth === undefined ||
                            section.version === undefined ) {
                            continue
                        }
                        let sectionExtentGeo = this.convertFileExtentToBoxGeo(section.fileExtent)
                        if ( sectionExtentGeo === undefined ) {
                            continue
                        }

                        let sectionExtentMercator = CoordinateConversion.convertBoxGeoToBoxMercator(sectionExtentGeo)
                        let sectionOverlapMercator = sectionExtentMercator.union(tileExtent)
                        if ( sectionOverlapMercator != null && sectionOverlapMercator.getWidth() > 0 && sectionOverlapMercator.getHeight() > 0 ) {
                            let subMapDescription = new SubMapDescription(sectionName, sectionExtentMercator, section.maxZoom, 
                                section.tileWidth, section.version)
                            overlaps.push(subMapDescription)
                        }
                    }

                    // Record the tile needs
                    let newTileDescription = new TileDescription(tileExtent, tileCache, currentZoom, x, y)
                    newTileDescription.setSubTileOverlaps(overlaps)
                    this.queue[currentZoom].add(newTileDescription)
                }
            }

            currentZoom++
        }
    }

    public getMaxZoom(): number {
        return 1
    }

    public hasNext(zoom: number): Boolean {
        return !this.queue[zoom].isEmpty
    }

    public pop(zoom: number): TileDescription | undefined {
        return this.queue[zoom].pop()
    }

    private getExtents(subMaps: string[], subSectionMetadata: Record<string, SectionMetadata>): BoxGeo {
        let maxLatitude: number | undefined
        let maxLongitude: number | undefined
        let minLatitude: number | undefined
        let minLongitude: number | undefined
    
        for ( let map of subMaps ) {
            // Validate the metadata
            let metadata = subSectionMetadata[map]
            if ( metadata === undefined ) {
                console.error(`Could not find map ${map} in metadata`)
                exit(1)
            }
    
            if ( metadata.fileExtent === undefined || 
    
                metadata.fileExtent.topLeft === undefined || 
                metadata.fileExtent.topLeft.latitude === undefined || 
                metadata.fileExtent.topLeft.longitude === undefined ||
    
                metadata.fileExtent.bottomRight === undefined ||
                metadata.fileExtent.bottomRight.latitude === undefined || 
                metadata.fileExtent.bottomRight.longitude === undefined ) {
    
                console.error(`Metadata for map ${map} missing file extents`)
                exit(1)
            }
    
            // Update the extents
            if ( maxLatitude === undefined || maxLatitude < metadata.fileExtent.topLeft.latitude ) {
                maxLatitude = metadata.fileExtent.topLeft.latitude
            }
            if ( maxLongitude === undefined || maxLongitude < metadata.fileExtent.bottomRight.longitude ) {
                maxLongitude = metadata.fileExtent.bottomRight.longitude
            }
            if ( minLatitude === undefined || maxLatitude < metadata.fileExtent.bottomRight.latitude ) {
                minLatitude = metadata.fileExtent.bottomRight.latitude
            }
            if ( minLongitude === undefined || maxLatitude < metadata.fileExtent.topLeft.longitude ) {
                minLongitude = metadata.fileExtent.topLeft.longitude
            }
        }
    
        return new BoxGeo(new PointGeo(minLongitude, maxLatitude), new PointGeo(maxLongitude, minLatitude))
    }

    private convertFileExtentToBoxGeo(fileExtent: FileExtent): BoxGeo | undefined {
        if ( fileExtent.topLeft === undefined || fileExtent.topLeft.latitude === undefined || fileExtent.topLeft.longitude === undefined ||
            fileExtent.bottomRight === undefined || fileExtent.bottomRight.latitude === undefined || fileExtent.bottomRight.longitude === undefined ) {
            return undefined
        }
        return new BoxGeo(new PointGeo(fileExtent.topLeft.longitude, fileExtent.topLeft.latitude), 
            new PointGeo(fileExtent.bottomRight.longitude, fileExtent.bottomRight.latitude))
    }
}