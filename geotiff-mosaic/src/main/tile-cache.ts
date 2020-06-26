import { SubMapDescription } from "./sub-map-description";

export class TileCache {
    /*
    public generate(mapNames: string[], zoom: number): void {

    }
    */

    public getPathForTile(subMapDescription: SubMapDescription, targetZoom: number, x: number, y: number) {
        return `./${subMapDescription.name}/${targetZoom}/${x}_${y}.png`
    }

    public dispose(): void {

    }
}