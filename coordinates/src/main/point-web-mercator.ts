import {Point2d} from './point-2d'

export class PointWebMercator extends Point2d {
    private static MAX_X_MERCATOR: number = 256;
    private static MAX_Y_MERCATOR: number = 256;

    constructor(x: number = 0, y: number = 0) {
        super(x, y);
    }

    public getCellForZoom(zoomLevel: number): Point2d {
        let cellsAcross = 2 ** zoomLevel;
        let xPerCell = PointWebMercator.MAX_X_MERCATOR / cellsAcross;
        let cellX = Math.floor(this.x / xPerCell);

        let yPerCell = PointWebMercator.MAX_Y_MERCATOR / cellsAcross;
        let cellY = Math.floor(this.y / yPerCell);

        let cellLocation = new Point2d(cellX, cellY);
        return cellLocation;
    }
}