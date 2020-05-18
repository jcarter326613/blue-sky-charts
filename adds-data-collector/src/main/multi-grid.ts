import { PointWebMercator } from 'coordinates'

export class MultiGrid {
    private maxZoom: number;
    private minZoom: number;
    private grids: Array<Array<Array<MultiGridEntry | null>>>;

    constructor(maxZoom: number = 0, minZoom: number = 0) {
        this.maxZoom = maxZoom;
        this.minZoom = minZoom;
        this.grids = new Array<Array<Array<MultiGridEntry | null>>>();
    }

    /**
     * Adds the object at the location specified in all the appropriate zoom cells.  It will overwrite existing
     * data in a cell if its priority is higher.
     * @param webMercatorLocation 
     * @param data 
     * @param priority 
     */
    public addObject( webMercatorLocation: PointWebMercator, data: any, priority: number ): void {
        let newEntry = new MultiGridEntry(data, priority);
        for (let currentZoom = this.minZoom; currentZoom <= this.maxZoom; currentZoom++) {
            // Make sure we have the current zoom in the grid
            let cellsAcross = 2 ** currentZoom;
            while ( this.grids.length < currentZoom + 1 ) {
                this.grids.push(new Array<Array<MultiGridEntry | null>>());
            }
            let thisGridZoomLevel = this.grids[currentZoom];

            // Get the proper cell for this zoom level
            let thisCellPoint = webMercatorLocation.getCellForZoom(currentZoom);
            let thisCellX = thisCellPoint.x;
            let thisCellY = thisCellPoint.y;

            // Make sure the grid has the proper cell
            while ( thisGridZoomLevel.length < thisCellY + 1 ) {
                thisGridZoomLevel.push(new Array<MultiGridEntry | null>());
            }
            let thisGridZoomLevelY = thisGridZoomLevel[thisCellY];
            while ( thisGridZoomLevelY.length < thisCellX + 1 ) {
                thisGridZoomLevelY.push(null);
            }

            // Put this data in the cell if its priority is higher
            let cellContents = thisGridZoomLevelY[thisCellX];
            if (cellContents === null || cellContents.priority < priority) {
                thisGridZoomLevelY[thisCellX] = new MultiGridEntry(data, priority);
            }
        }
    }

    public forEach(callback: (zoomLevel: number, fileName: string, data: string) => void): void {
        for (let currentZoom = 0; currentZoom < this.grids.length; currentZoom++) {
            let thisGridZoom = this.grids[currentZoom];
            for (let currentY = 0; currentY < thisGridZoom.length; currentY++) {
                let thisGridY = thisGridZoom[currentY];
                for (let currentX = 0; currentX < thisGridY.length; currentX++) {
                    let data = thisGridY[currentX];
                    if ( data != null ) {
                        callback(currentZoom, `${currentX}_${currentY}.json`, JSON.stringify(data.data));
                    } else {
                        callback(currentZoom, `${currentX}_${currentY}.json`, "{}");
                    }
                }
            }
        }
    }
}

class MultiGridEntry {
    public data: any;
    public priority: number;

    constructor(data: any, priority: number) {
        this.data = data;
        this.priority = priority;
    }
}