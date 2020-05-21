import { PointWebMercator } from 'coordinates'

export class MultiGrid {
    private baseZoom: number;
    private fileGrid: Array<Array<Array<any>>>;

    constructor(baseZoom: number = 2) {
        this.baseZoom = baseZoom;
        this.fileGrid = new Array<Array<Array<any>>>();
        let cellsAcross = 2 ** baseZoom;
        for ( let y = 0; y < cellsAcross; y++ ) {
            let yArray = new Array<Array<any>>();
            for ( let x = 0; x < cellsAcross; x++ ) {
                yArray.push(new Array());
            }
            this.fileGrid.push(yArray);
        }
    }

    /**
     * Adds the object at the location specified in all the appropriate zoom cells.  It will overwrite existing
     * data in a cell if its priority is higher.
     * @param webMercatorLocation 
     * @param data 
     * @param priority 
     */
    public addObject( webMercatorLocation: PointWebMercator, data: any): void {
        // Get the proper cell for this zoom level
        let thisCellPoint = webMercatorLocation.getCellForZoom(this.baseZoom);
        let thisCellX = thisCellPoint.x;
        let thisCellY = thisCellPoint.y;

        // Put this data in the cell if its priority is higher
        let cellContents = this.fileGrid[thisCellY][thisCellX];
        cellContents.push(data);
    }

    /**
     * Iterates through all the files that should be written out
     * @param callback Receives the files and associated contents, one at a time.
     */
    public forEach(callback: (fileName: string, data: string) => void): void {
        for (let currentY = 0; currentY < this.fileGrid.length; currentY++) {
            let thisGridY = this.fileGrid[currentY];
            for (let currentX = 0; currentX < thisGridY.length; currentX++) {
                callback(`${currentX}_${currentY}.json`, JSON.stringify(thisGridY[currentX]));
            }
        }
    }
}
