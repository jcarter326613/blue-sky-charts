import { execSync } from 'child_process'
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'fs'
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

        // Grab all the versions we haven't uploaded from the geotiff exploder metadata file
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
        console.log("test")

        //For each version,
            //Explode the map

            //Convert all the artifacts to jpg files

            //Sync the folder up to AWS

            //Write out the new metadata file

            //Sync the metadata file to AWS
    }
}