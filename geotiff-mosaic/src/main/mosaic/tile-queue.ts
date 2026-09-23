
import { BoxGeo, CoordinateConversion, PointGeo, Point2d, BoxWebMercator } from 'coordinates'
import { ChangeSet } from '../models/change-set'
import { Conversion } from '../models/conversion'
import { exit } from 'process'
import { Heap } from 'ts-heap'
import { MetadataManager } from './metadata-manager'
import { SectionVersion } from '../models/section-version'
import { SectionVersionList } from '../models/section-version-list'
import { TileDescription } from './tile-description'
import { SubMapDescription } from './sub-map-description'
import { TileCache } from './tile-cache'

export class TileQueue {
    private queue: Heap<TileDescription>
    private changeSet: ChangeSet = new ChangeSet()
    private changeSetDate: Date | undefined
    private changeSetDateString: string | undefined
    private changeSetKeys: Record<string, Date> = {}
    private changeSetFilterDate: Date

    public latestEffective: Date | undefined
    public earliestExpiration: Date | undefined

    public constructor (maps: Array<string>, metadata: Record<string, SectionVersion>, metadataManager: MetadataManager,
        tileCache: TileCache, zoom: number, changeSetFilterDate: Date) {

        this.changeSetFilterDate = changeSetFilterDate

        // Load all the configs for each map and determine the mosaic rectangular extents
        let extents = this.getExtents(maps, metadata)
        let extentsMercator = CoordinateConversion.convertBoxGeoToBoxMercator(extents)

        // Create the list of tiles needed to cover that extent
        let tilesAcross = 2 ** zoom

        this.queue =
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
                    extentsMercator.getBottomRight().y + (y+1) * (extentsMercator.getHeight() / tilesAcross),
                    extentsMercator.getTopLeft().x + (x+1) * (extentsMercator.getWidth() / tilesAcross),
                    extentsMercator.getBottomRight().y + y * (extentsMercator.getHeight() / tilesAcross))

                // Get the sections that overlap that extent
                let overlaps: Array<SubMapDescription> = []
                for ( let sectionName in metadata ) {
                    let section = metadata[sectionName]
                    if ( section.mosaicFileExtent === undefined || section.mosaicMaxZoom === undefined || section.tileWidth === undefined ||
                        section.version === undefined ) {
                        continue
                    }
                    let sectionExtentGeo = Conversion.convertFileExtentToBoxGeo(section.mosaicFileExtent)
                    if ( sectionExtentGeo === undefined ) {
                        continue
                    }

                    let sectionExtentMercator = CoordinateConversion.convertBoxGeoToBoxMercator(sectionExtentGeo)
                    let sectionOverlapMercator = sectionExtentMercator.intersection(tileExtent)
                    if ( sectionOverlapMercator != null && sectionOverlapMercator.getWidth() > 0 && sectionOverlapMercator.getHeight() > 0 ) {
                        let subMapDescription = new SubMapDescription(sectionName, sectionExtentMercator, section.mosaicMaxZoom, 
                            section.tileWidth, section.version)
                        if ( section.effectiveDate !== undefined ) {
                            this.addToChangeSet(zoom, x, y, section.effectiveDate)
                        }
                        overlaps.push(subMapDescription)
                    }

                    //Update the effective to expiration dates
                    let effectiveDate = metadataManager.extractDate(section.effectiveDate)
                    if (this.latestEffective === undefined || (effectiveDate !== undefined && this.latestEffective < effectiveDate)) {
                        this.latestEffective = effectiveDate
                    }

                    let expirationDate = metadataManager.extractDate(section.expirationDate)
                    if (this.earliestExpiration === undefined || (expirationDate !== undefined && this.earliestExpiration > expirationDate)) {
                        this.earliestExpiration = expirationDate
                    }
                }

                // Record the tile needs
                let newTileDescription = new TileDescription(tileExtent, tileCache, zoom, x, tilesAcross - y - 1)
                newTileDescription.setSubTileOverlaps(overlaps)
                this.queue.add(newTileDescription)
            }
        }
    }

    public hasNext(): Boolean {
        return !this.queue.isEmpty
    }

    public pop(): TileDescription | undefined {
        return this.queue.pop()
    }

    public getChangeSet(): Record<string, ChangeSet> {
        if ( this.changeSetDateString !== undefined ) {
            let retVal: Record<string, ChangeSet> = {}
            retVal[this.changeSetDateString] = this.changeSet
            return retVal
        } else {
            return {}
        }
    }

    private addToChangeSet(zoom: number, x: number, y: number, effectiveDateString: string): void {
        // Check the effective date
        let effectiveDate = this.convertStringToDate(effectiveDateString)

        if ( this.changeSetFilterDate > effectiveDate ) {
            return
        }

        if ( this.changeSetDate === undefined || this.changeSetDate < effectiveDate ) {
            this.changeSetDate = effectiveDate
        }

        //Add the tile to the changeset with the specified effective date
        let key = `${zoom}_${x}_${y}`
        this.changeSetKeys[key] = effectiveDate
        let lookup = this.changeSet.tiles
        if ( lookup === undefined ) {
            lookup = {}
            this.changeSet.tiles = lookup
        }
        if ( !(zoom in lookup) ) {
            lookup[zoom] = {}
        }
        if ( !(x in lookup[zoom]) ) {
            lookup[zoom][x] = []
        }
        lookup[zoom][x].push(y)
    }

    private convertStringToDate(s: string): Date {
        return new Date(parseInt(s.substring(0, 4)), parseInt(s.substring(5,7)) - 1, parseInt(s.substring(8,10)))
    }

    private getExtents(subMaps: string[], subSectionMetadata: Record<string, SectionVersion>): BoxGeo {
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
    
            if ( metadata.mosaicFileExtent === undefined || 
    
                metadata.mosaicFileExtent.topLeft === undefined || 
                metadata.mosaicFileExtent.topLeft.latitude === undefined || 
                metadata.mosaicFileExtent.topLeft.longitude === undefined ||
    
                metadata.mosaicFileExtent.bottomRight === undefined ||
                metadata.mosaicFileExtent.bottomRight.latitude === undefined || 
                metadata.mosaicFileExtent.bottomRight.longitude === undefined ) {
    
                console.error(`Metadata for map ${map} missing file extents`)
                exit(1)
            }
    
            // Update the extents
            if ( maxLatitude === undefined || maxLatitude < metadata.mosaicFileExtent.topLeft.latitude ) {
                maxLatitude = metadata.mosaicFileExtent.topLeft.latitude
            }
            if ( maxLongitude === undefined || maxLongitude < metadata.mosaicFileExtent.bottomRight.longitude ) {
                maxLongitude = metadata.mosaicFileExtent.bottomRight.longitude
            }
            if ( minLatitude === undefined || minLatitude > metadata.mosaicFileExtent.bottomRight.latitude ) {
                minLatitude = metadata.mosaicFileExtent.bottomRight.latitude
            }
            if ( minLongitude === undefined || minLongitude > metadata.mosaicFileExtent.topLeft.longitude ) {
                minLongitude = metadata.mosaicFileExtent.topLeft.longitude
            }
        }
    
        return new BoxGeo(new PointGeo(minLongitude, maxLatitude), new PointGeo(maxLongitude, minLatitude))
    }
}