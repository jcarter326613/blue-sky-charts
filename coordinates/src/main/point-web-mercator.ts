import {Point2d} from './point-2d'

export class PointWebMercator extends Point2d {
    public static readonly MAX_X_MERCATOR: number = 256;
    public static readonly MAX_Y_MERCATOR: number = 256;

    constructor(x: number = 0, y: number = 0) {
        super(x, y);
    }

    public getCellForZoom(zoomLevel: number): Point2d {

        let cellsAcross = 2 ** zoomLevel;
        let xPerCell = PointWebMercator.MAX_X_MERCATOR / cellsAcross;
        let cellX = Math.floor(this.x / xPerCell);

        let yPerCell = PointWebMercator.MAX_Y_MERCATOR / cellsAcross;
        let cellY = Math.floor(this.y / yPerCell);

        if ( this.x == PointWebMercator.MAX_X_MERCATOR ) {
            cellX--;
        }
        if ( this.y == PointWebMercator.MAX_Y_MERCATOR ) {
            cellY--;
        }

        let cellLocation = new Point2d(cellX, cellY);
        return cellLocation;
    }
}