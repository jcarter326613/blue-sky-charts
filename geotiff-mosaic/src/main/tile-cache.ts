import { exec } from 'child_process'
import { SubMapDescription } from "./sub-map-description";
import { exit } from 'process';

export class TileCache {
    private generatedFiles: Record<string, Record<number, string>> = {}     //mapname, zoom, root directory

    public getPathForTile(subMapDescription: SubMapDescription, targetZoom: number, x: number, y: number) {
        // Return the path if it's been generated
        if ( this.generatedFiles[subMapDescription.name] !== undefined && 
            this.generatedFiles[subMapDescription.name][targetZoom] !== undefined ) {

            return `${this.generatedFiles[subMapDescription.name][targetZoom]}/${x}_${y}.png`
        }

        // Otherwise generate the file
        console.log(`Setting up map ${subMapDescription.name}`)
        exec(`python3 ../geotiff-map-exploder/setup_map.py -use-defaults ${subMapDescription.name}`, (error, stdout, stderr) => {
            if ( error ) {
                console.error(error)
                exit(1)
            }
        })
        console.log(`Exploding map ${subMapDescription.name}`)
        exec(`python3 ../geotiff-map-exploder/setup_map.py -use-defaults ${subMapDescription.name}`, (error, stdout, stderr) => {
            if ( error ) {
                console.error(error)
                exit(1)
            }
        })
        console.log(`Explosion complete`)
    }

    public dispose(): void {

    }
}