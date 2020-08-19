import { execSync } from 'child_process'
import { readFileSync, writeFileSync, existsSync, mkdirSync, fstat, readdirSync } from 'fs'
import { PreviousOriginals } from '../models/previous-originals'
import { ProjectionLcc } from '../models/projection-lcc'
import { ProjectionExtents } from '../models/projection-extents'
import { SectionVersion } from '../models/section-version'
import { SectionVersionList } from '../models/section-version-list'
import { exit } from 'process'
import { MapType } from './map-type'

export class Loader {
    private OUTPUT_DIRECTORY = "./output"
    private PEVIOUS_UPLOADS_FILE = "./data/previous_uploads.json"
    private SUBSECTION_META_FILE = "../geotiff-map-exploder/maps/metadata.json"
    private TERMINAL_METADATA_FILE = "./data/terminal_metadata.json"
    private SECTIONAL_METADATA_FILE = "./data/sectional_metadata.json"
    private METADATA_FILE_OUTPUT = "./output/metadata.json"

    private type: MapType
    public static skipTileGeneration: Boolean = false

    constructor(type: MapType) {
        this.type = type

        execSync(`cp ../geotiff-map-exploder/maps/metadata.json ./maps`)
        if ( existsSync("./maps/cache") ) {
            execSync(`rm -rf ./maps/cache`)
        }
        execSync(`mkdir ./maps/cache`)
    }

    public syncChangedMaps(): void {
        // Clean the output directory
        if (existsSync(this.OUTPUT_DIRECTORY)) {
            execSync(`rm -rf ${this.OUTPUT_DIRECTORY}`)
        }
        mkdirSync(this.OUTPUT_DIRECTORY);

        // Load the previous metadata file
        let outputMetadata: Record<string, SectionVersionList>
        try {
            let pathToRead: string
            switch(this.type) {
                case MapType.Sectional:
                    pathToRead = this.SECTIONAL_METADATA_FILE
                    break
                case MapType.Terminal:
                    pathToRead = this.TERMINAL_METADATA_FILE
                    break
                default:
                    console.error("Bad map type")
                    exit(1)
            }
            let rawdata = readFileSync(pathToRead)
            outputMetadata = JSON.parse(rawdata.toString())
        } catch (error) {
            outputMetadata = {}
        }

        // Load the data files which tells us all the uploaded versions of each map
        let configuration: PreviousOriginals
        try {
            let previousUploadsFile = this.PEVIOUS_UPLOADS_FILE
            let rawdata = readFileSync(previousUploadsFile)
            configuration = JSON.parse(rawdata.toString())
        } catch (error) {
            configuration = new PreviousOriginals()
        }

        // Get all the versions of maps in the metadata
        let subsectionMetaFile = this.SUBSECTION_META_FILE
        let rawdata = readFileSync(subsectionMetaFile)
        let subSectionVersions: Record<string, SectionVersionList> = JSON.parse(rawdata.toString())

        // Search for all the versions we haven't uploaded from the geotiff exploder metadata file
        let neededMapVersions: Record<string, Array<string>> = {}
        for (let map of Object.keys(subSectionVersions)) {
            let mapValue = subSectionVersions[map]
            if ( mapValue.versions != null && 
                (
                    (this.type == MapType.Sectional && (mapValue.type === undefined || mapValue.type == "Sectional")) ||
                    (this.type == MapType.Terminal && mapValue.type == "TerminalArea") 
                )) {
                for (let version of Object.keys(mapValue.versions)) {
                    if (configuration.maps === undefined || version !in configuration.maps[map]) {
                        if (!(map in neededMapVersions)) {
                            neededMapVersions[map] = []
                            neededMapVersions[map].push(version)
                        } else if (parseInt(neededMapVersions[map][0]) < parseInt(version)) {
                            neededMapVersions[map] = []
                            neededMapVersions[map].push(version)
                        }
                    }
                }
            }
        }

        //For each version,
        let mapTypeAbbreviation: string
        let mapTypeLong: string
        if ( this.type == MapType.Sectional ) {
            mapTypeAbbreviation = "SEC"
            mapTypeLong = "sectional"
        } else if ( this.type == MapType.Terminal ) {
            mapTypeAbbreviation = "TAC"
            mapTypeLong = "terminal"
        } else {
            console.error("Bad map type")
            exit(1)
        }

        let outputVersionId = (new Date()).toISOString().replace(/\..+/, "").replace(":", "-").replace(":", "-")
        for ( let mapName of Object.keys(neededMapVersions) ) {
            let outputMapName = mapName
            if ( this.type == MapType.Terminal ) {
                outputMapName = mapName.substring(0, mapName.length - "_terminal".length)
            }

            if ( !(mapName in outputMetadata) ) {
                outputMetadata[outputMapName] = new SectionVersionList()
                outputMetadata[outputMapName].versions = {}
            } 
            if ( outputMetadata[outputMapName].versions === undefined ) {
                outputMetadata[outputMapName].versions = {}
            }
            let versionList = neededMapVersions[mapName]
            for ( let version of versionList ) {
                //Make sure this should be updated
                let thisOutputVersionId = outputVersionId
                let outputVersions = outputMetadata[outputMapName].versions
                let inputVerions = subSectionVersions[mapName].versions
                if (outputVersions === undefined || inputVerions === undefined) {
                    console.error("Broken code")
                    exit(1)
                }
                let duplicateDiscovered = false
                for (let outputVersionKey in outputVersions) {
                    if (outputVersions[outputVersionKey].effectiveDate?.substring(0, 10) == inputVerions[version].effectiveDate?.substring(0, 10)) {
                        duplicateDiscovered = true
                        thisOutputVersionId = outputVersionKey
                        break
                    }
                }

                // Explode the tiles
                if (!duplicateDiscovered && !Loader.skipTileGeneration) {
                    //Ensure png present
                    let pngFilePath = `./maps/${mapName}_${mapTypeAbbreviation}_${version}.tif`
                    if ( !existsSync(pngFilePath) ) {
                        let pngGeoFilePath = `../geotiff-map-exploder/maps/${mapName}_${mapTypeAbbreviation}_${version}.tif`
                        if ( !existsSync(pngGeoFilePath) ) {
                            console.log(`Setting up map ${mapName} version ${version}`)
                            execSync(`python3 ../geotiff-map-exploder/setup_map.py -use-defaults ${outputMapName} ${version} ${mapTypeLong}`)
                        } else {
                            execSync(`cp ${pngGeoFilePath} ./maps/`)
                        }
                    }

                    //Explode the map
                    console.log(`Exploding map ${mapName} version ${version}`)
                    if ( existsSync("./maps/tiles") ) {
                        execSync("rm -rf ./maps/tiles")
                    }
                    execSync(`python3 ../geotiff-map-exploder/explode_maps.py ${mapName} ${version} relative ${mapTypeLong}`)

                    //Prep the output directory
                    let mapOutputDirectory = `${this.OUTPUT_DIRECTORY}/${outputMapName}`
                    mkdirSync(mapOutputDirectory)
                    mapOutputDirectory = `${mapOutputDirectory}/${thisOutputVersionId}`
                    mkdirSync(mapOutputDirectory)

                    //Convert all the artifacts to jpg files
                    let tileMapDirectory = `./maps/tiles/${mapName}_${mapTypeAbbreviation}_${version}`
                    let zoomDirectories = readdirSync(tileMapDirectory)
                    for ( let zoomDirectory of zoomDirectories ) {
                        let fillZoomDirectory = `${mapOutputDirectory}/${zoomDirectory}`
                        mkdirSync(fillZoomDirectory)
                        let imageFiles = readdirSync(`${tileMapDirectory}/${zoomDirectory}`)
                        for ( let imageFile of imageFiles ) {
                            let lastDotIndex = imageFile.lastIndexOf(".")
                            let jpegPath = `${fillZoomDirectory}/${imageFile.substring(0, lastDotIndex)}.jpg`
                            let originalPath = `${tileMapDirectory}/${zoomDirectory}/${imageFile}`
                            execSync(`convert ${originalPath} -quality 90 ${jpegPath}`)
                        }
                    }
                }

                //Update the metadata output file
                outputVersions[thisOutputVersionId] = new SectionVersion()
                outputVersions[thisOutputVersionId].effectiveDate = inputVerions[version].effectiveDate
                outputVersions[thisOutputVersionId].expirationDate = inputVerions[version].expirationDate
                outputVersions[thisOutputVersionId].imageHeight = inputVerions[version].imageHeight
                outputVersions[thisOutputVersionId].imageWidth = inputVerions[version].imageWidth
                outputVersions[thisOutputVersionId].maxZoom = inputVerions[version].maxZoom
                outputVersions[thisOutputVersionId].tileWidth = inputVerions[version].tileWidth
                outputVersions[thisOutputVersionId].version = thisOutputVersionId

                // Validate the projection
                let originalProjectionLcc = inputVerions[version].originalProjectionData
                if ( originalProjectionLcc === undefined ) {
                    console.error("Projection undefined")
                    exit(1)
                }
                let projectionLcc = new ProjectionLcc()
                projectionLcc.lat0 = originalProjectionLcc.lat_0
                projectionLcc.lat1 = originalProjectionLcc.lat_1
                projectionLcc.lat2 = originalProjectionLcc.lat_2
                projectionLcc.lon0 = originalProjectionLcc.lon_0
                projectionLcc.x0 = originalProjectionLcc.x_0
                projectionLcc.y0 = originalProjectionLcc.y_0

                if (originalProjectionLcc.datum != "NAD83" ||
                    originalProjectionLcc.no_defs != true ||
                    originalProjectionLcc.proj != "lcc" || 
                    originalProjectionLcc.units != "m") {
                    console.error("Bad projection")
                    exit(1)
                }
                outputVersions[thisOutputVersionId].projectionLcc = projectionLcc

                // Validate the projection extents
                let originalExtents = inputVerions[version].originalProjectionBounds
                if ( originalExtents == null || originalExtents.length != 4 ) {
                    console.error("Bad projection extents")
                    exit(1)
                }

                projectionLcc.extents = new ProjectionExtents()
                projectionLcc.extents.left = originalExtents[0]
                projectionLcc.extents.top = originalExtents[1]
                projectionLcc.extents.right = originalExtents[2]
                projectionLcc.extents.bottom = originalExtents[3]
            }
        }

        //Write out the new metadata file
        let rawOutputdata = JSON.stringify(outputMetadata)
        writeFileSync(this.METADATA_FILE_OUTPUT, rawOutputdata)
        switch(this.type) {
            case MapType.Sectional:
                writeFileSync(this.SECTIONAL_METADATA_FILE, rawOutputdata)
                break
            case MapType.Terminal:
                writeFileSync(this.TERMINAL_METADATA_FILE, rawOutputdata)
                break
            default:
                console.error("Bad map type")
                exit(1)
        }
    }
}
