import { execSync } from 'child_process'
import { SubMapDescription } from "./sub-map-description";
import { existsSync } from 'fs';

export class TileCache {
    private generatedFiles: Record<string, Record<number, string>> = {}     //mapname, zoom, root directory

    constructor() {
        execSync(`cp ../geotiff-map-exploder/maps/metadata.json ./maps`)
        if ( existsSync("./maps/cache") ) {
            execSync(`rm -rf ./maps/cache`)
        }
        execSync(`mkdir ./maps/cache`)
    }

    public getPathForTile(subMapDescription: SubMapDescription, targetZoom: number, x: number, y: number): string {
        // Return the path if it's been generated
        if ( this.generatedFiles[subMapDescription.name] !== undefined && 
            this.generatedFiles[subMapDescription.name][targetZoom] !== undefined ) {

            return `${this.generatedFiles[subMapDescription.name][targetZoom]}/${x}_${y}.png`
        }

        // Otherwise generate the file
        let pngFilePath = `./maps/${subMapDescription.name}_SEC_${subMapDescription.version}_WEB_CROPPED_RGB.png`
        if ( !existsSync(pngFilePath) ) {
            console.log(`Setting up map ${subMapDescription.name}`)
            execSync(`python3 ../geotiff-map-exploder/setup_map.py -use-defaults ${subMapDescription.name}`)
        }
        console.log(`Exploding map ${subMapDescription.name}`)
        execSync(`python3 ../geotiff-map-exploder/explode_maps.py ${subMapDescription.name} ${subMapDescription.version} relative ${targetZoom}`)
        console.log(`Explosion complete. Cleaning up.`)
        execSync(`mv ./maps/tiles/* ./maps/cache`)
        execSync(`rm -rf ./maps/tiles`)
        execSync(`rm -f ./maps/*.png.*`)
        execSync(`rm -f ./maps/*.tif`)

        let rootDir = `./maps/cache/${subMapDescription.name}_SEC_${subMapDescription.version}/${targetZoom}`
        if ( this.generatedFiles[subMapDescription.name] === undefined ) {
            this.generatedFiles[subMapDescription.name] = {}
        }
        this.generatedFiles[subMapDescription.name][targetZoom] = rootDir

        return `${rootDir}/${x}_${y}.png`
    }

    public dispose(): void {
        execSync(`rm -rf ./maps/cache`)
        execSync(`rm -f ./maps/*.zip`)
    }
}