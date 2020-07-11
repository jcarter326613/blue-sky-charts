import { execSync } from 'child_process'
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'fs'
import { PreviousOriginals } from '../models/previous_originals'
import { SectionVersionList } from '../models/section-version-list'

export class Loader {
    private PEVIOUS_UPLOADS_FILE = "./data/previous_uploads.json"
    private OUTPUT_DIRECTORY = "./output"

    public syncChangedMaps(): void {
        // Clean the output directory
        if (existsSync(this.OUTPUT_DIRECTORY)) {
            execSync(`rm -rf ${this.OUTPUT_DIRECTORY}`)
        }
        mkdirSync(this.OUTPUT_DIRECTORY);

        // Load the data files which tells us all the uploaded versions of each map
        let previousUploadsFile = this.PEVIOUS_UPLOADS_FILE
        let rawdata = readFileSync(previousUploadsFile)
        let configuration: PreviousOriginals = JSON.parse(rawdata.toString())

        // Get all the versions of maps in the metadata
        let subsectionMetaFile = this.SUBSECTION_META_FILE
        rawdata = readFileSync(subsectionMetaFile)
        let subSectionVersions: Record<string, SectionVersionList> = JSON.parse(rawdata.toString())

        // Grab all the versions we haven't uploaded from the geotiff exploder metadata file


        //For each version,
            //Explode the map

            //Convert all the artifacts to jpg files

            //Sync the folder up to AWS

            //Write out the new metadata file

            //Sync the metadata file to AWS
    }
}