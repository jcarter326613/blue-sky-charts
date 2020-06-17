import kotlin.math.floor;
import kotlin.math.pow;

class PointWebMercator(x: Double = 0.0, y: Double = 0.0) : Point2d(x, y) {
    public fun getCellForZoom(zoomLevel: Double): Point2d {
        var cellsAcross = (2.0).pow(zoomLevel);
        var xPerCell = PointWebMercatorStatic.MAX_X_MERCATOR / cellsAcross;
        var cellX = floor(this.x / xPerCell);

        var yPerCell = PointWebMercatorStatic.MAX_Y_MERCATOR / cellsAcross;
        var cellY = floor(this.y / yPerCell);

        if ( this.x == PointWebMercatorStatic.MAX_X_MERCATOR.toDouble() ) {
            cellX--;
        }
        if ( this.y == PointWebMercatorStatic.MAX_Y_MERCATOR.toDouble() ) {
            cellY--;
        }

        return Point2d(cellX, cellY);
    }
}