import { Point2d } from "../coordinates/point-2d"
import { TileReceiver } from "./tile-receiver";

export class TileProvider {
    private tileCache: Record<string, TileRequest>;

    constructor() {
        this.tileCache = {};
    }

    public retrieveTile(mapName: string, mapVersion: string, zoomLevel: number, location: Point2d, 
            receiver: TileReceiver, data: any): void {
        let key = this.createKey(mapName, zoomLevel, location);
        let tileRequest: TileRequest;

        if ( key in this.tileCache ) {
            tileRequest = this.tileCache[key];

            if (tileRequest.isLoaded())
                receiver.receiveTile(location, tileRequest.getImage(), data);
            else
                tileRequest.setReceiver(receiver, data);
        } else {
            let url = `/${mapName}_SEC_${mapVersion}/${zoomLevel}/${location.x}_${location.y}.png`;
            tileRequest = new TileRequest(receiver, location, data, url);
            this.tileCache[key] = tileRequest
        }
    }

    private createKey(mapName: string, zoomLevel: number, location: Point2d): string {
        return `${mapName}|${zoomLevel}|${location.x}_${location.y}`;
    }
}

class TileRequest {
    private loaded: boolean;
    private tileImage: HTMLImageElement;
    private receiver: TileReceiver | null;
    private location: Point2d | null;
    private data: any | null;

    constructor(receiver: TileReceiver, location: Point2d, data: any, url: string) {
        this.loaded = false;
        this.receiver = receiver;
        this.location = location;
        this.data = data;

        let thisObj = this;

        this.tileImage = new Image();
        this.tileImage.addEventListener("load", function() {
            thisObj.loaded = true;
            if (thisObj.receiver != null && thisObj.location != null) {
                thisObj.receiver.receiveTile(thisObj.location, thisObj.tileImage, thisObj.data);
                thisObj.receiver = null;
                thisObj.location = null;
                thisObj.data = null;
            }
        });
        this.tileImage.src = url;
    }

    public setReceiver(receiver: TileReceiver, data: any) {
        this.receiver = receiver;
        this.data = data;
    }

    public isLoaded(): boolean {
        return this.loaded;
    }

    public getImage(): HTMLImageElement {
        return this.tileImage;
    }
}
