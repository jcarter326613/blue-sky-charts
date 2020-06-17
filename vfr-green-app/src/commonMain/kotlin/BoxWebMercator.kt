import kotlin.js.JsName
import kotlin.math.max;
import kotlin.math.min;

class BoxWebMercator(topLeftX: Double = 0.0, topLeftY: Double = 0.0, bottomRightX: Double = 0.0, bottomRightY: Double = 0.0) {
    var topLeft: PointWebMercator
        private set;
    var bottomRight: PointWebMercator
        private set;

    init {
        this.topLeft = PointWebMercator(topLeftX, topLeftY);
        this.bottomRight = PointWebMercator(bottomRightX, bottomRightY);
    }

    public fun getWidth(): Double {
        return this.bottomRight.x - this.topLeft.x
    }

    public fun getHeight(): Double {
        return this.bottomRight.y - this.topLeft.y
    }

    /**
     * Returns the union of the two boxes.  An real number range is allowed.
     * @param o
     */
    @JsName("union")
    public fun union(o: BoxWebMercator): BoxWebMercator? {
        val newBox = BoxWebMercator();
        newBox.topLeft.x = max(this.topLeft.x, o.topLeft.x);
        newBox.topLeft.y = max(this.topLeft.y, o.topLeft.y);
        newBox.bottomRight.x = min(this.bottomRight.x, o.bottomRight.x);
        newBox.bottomRight.y = min(this.bottomRight.y, o.bottomRight.y);

        if (newBox.bottomRight.x < newBox.topLeft.x || newBox.bottomRight.y < newBox.topLeft.y) {
            return null;
        }

        return newBox;
    }
}