import { execSync } from 'child_process'
import { readFileSync, writeFileSync, existsSync, mkdirSync, fstat, readdirSync } from 'fs'
import { PreviousOriginals } from '../models/previous_originals'
import { SectionVersionList } from '../models/section-version-list'

export class Loader {
    private OUTPUT_DIRECTORY = "./output"
    private PEVIOUS_UPLOADS_FILE = "./data/previous_uploads.json"
    private SUBSECTION_META_FILE = "../geotiff-map-exploder/maps/metadata.json"

    public syncChangedMaps(): void {
        // Clean the output directory
        if (existsSync(this.OUTPUT_DIRECTORY)) {
            execSync(`rm -rf ${this.OUTPUT_DIRECTORY}`)
        }
        mkdirSync(this.OUTPUT_DIRECTORY);

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
            if ( mapValue.versions != null ) {
                for (let version of Object.keys(mapValue.versions)) {
                    if (configuration.maps === undefined || version !in configuration.maps[map]) {
                        if (!(map in neededMapVersions)) {
                            neededMapVersions[map] = []
                        }
                        neededMapVersions[map].push(version)
                    }
                }
            }
        }

        //For each version,
        for ( let mapName of Object.keys(neededMapVersions) ) {
            let versionList = neededMapVersions[mapName]
            for ( let version of versionList ) {
                //Ensure png present
                let pngFilePath = `./maps/${mapName}_SEC_${version}.tif`
                if ( !existsSync(pngFilePath) ) {
                    let pngGeoFilePath = `../geotiff-map-exploder/maps/${mapName}_SEC_${version}.tif`
                    if ( !existsSync(pngGeoFilePath) ) {
                        console.log(`Setting up map ${mapName} version ${version}`)
                        execSync(`python3 ../geotiff-map-exploder/setup_map.py -use-defaults ${mapName} ${version} sectional`)
                    } else {
                        execSync(`cp ${pngGeoFilePath} ./maps/`)
                    }
                }

                //Explode the map
                console.log(`Exploding map ${mapName} version ${version}`)
                if ( existsSync("./maps/tiles") ) {
                    execSync("rm -rf ./maps/tiles")
                }
                execSync(`python3 ../geotiff-map-exploder/explode_maps.py ${mapName} ${version} relative sectional`)

                //Prep the output directory
                let mapOutputDirectory = `${this.OUTPUT_DIRECTORY}/${mapName}_SEC_${version}`
                mkdirSync(mapOutputDirectory)

                //Convert all the artifacts to jpg files
                let tileMapDirectory = `./maps/tiles/${mapName}_SEC_${version}`
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
                    
                    console.log("rgsg")
                }

                console.log("rgsg")

                //Sync the folder up to AWS

            }
        }

        //Write out the new metadata file

        //Sync the metadata file to AWS
    }
}