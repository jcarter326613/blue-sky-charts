import { Box2d } from './box-2d'
import { BoxGeo } from './box-geo'
import { BoxWebMercator } from './box-web-mercator'
import { Point2d } from './point-2d'
import { PointGeo } from './point-geo'
import { PointWebMercator } from './point-web-mercator'

export class CoordinateConversion {
    public static readonly MAX_LONGITUDE: number = 180
    public static readonly MIN_LONGITUDE: number = -180
    public static readonly MAX_LATITUDE: number = 85
    public static readonly MIN_LATITUDE: number = -85

    private static maxMercator: BoxWebMercator | undefined

    /**
     * Converts the given longitude and latitude to a Web Mercator projection where the upper left is (0,0) and the lower right is (256, 256)
     * https://en.wikipedia.org/wiki/Web_Mercator_projection#Formulas
     * @param geoPoint 
     */
    public static convertPointGeoToPointWebMercator(geoPoint: PointGeo): PointWebMercator {
        let newPoint = new PointWebMercator()

        let longitude = geoPoint.longitude
        let latitude = geoPoint.latitude

        if ( latitude > CoordinateConversion.MAX_LATITUDE ) {
            latitude = CoordinateConversion.MAX_LATITUDE
        } else if ( latitude < CoordinateConversion.MIN_LATITUDE ) {
            latitude = CoordinateConversion.MIN_LATITUDE
        } 
        
        if ( longitude > CoordinateConversion.MAX_LONGITUDE ) {
            longitude = CoordinateConversion.MAX_LONGITUDE
        } else if ( longitude < CoordinateConversion.MIN_LONGITUDE ) {
            longitude = CoordinateConversion.MIN_LONGITUDE
        }

        newPoint.x = longitude * 20037508.34 / 180
        newPoint.y = Math.log(Math.tan((90 + latitude) * Math.PI / 360)) * (20037508.34 / Math.PI)

        return newPoint
    }

    /**
     * Converts the given x,y coordinates to latitude and longitude.  Min x and y are (0,0) and max is (256, 256).
     * @param point2d 
     */
    public static convertPointWebMercatorToPointGeo(point2d: PointWebMercator): PointGeo {
        let longitude = point2d.x * 180 / 20037508.34
        let latitude = Math.atan(Math.exp(point2d.y * Math.PI / 20037508.34)) * 360 / Math.PI - 90

        if ( latitude > CoordinateConversion.MAX_LATITUDE ) {
            latitude = CoordinateConversion.MAX_LATITUDE
        } else if ( latitude < CoordinateConversion.MIN_LATITUDE ) {
            latitude = CoordinateConversion.MIN_LATITUDE
        } 
        
        if ( longitude > CoordinateConversion.MAX_LONGITUDE ) {
            longitude = CoordinateConversion.MAX_LONGITUDE
        } else if ( longitude < CoordinateConversion.MIN_LONGITUDE ) {
            longitude = CoordinateConversion.MIN_LONGITUDE
        }

        return new PointGeo(longitude, latitude)
    }

    private static getMaxMercator(): BoxWebMercator {
        if ( CoordinateConversion.maxMercator === undefined ) {
            CoordinateConversion.maxMercator = CoordinateConversion.convertBoxGeoToBoxMercator(new BoxGeo(
                new PointGeo(CoordinateConversion.MIN_LONGITUDE, CoordinateConversion.MAX_LATITUDE),
                new PointGeo(CoordinateConversion.MAX_LONGITUDE, CoordinateConversion.MIN_LATITUDE)))
        }
        return CoordinateConversion.maxMercator
    }

    public static convertPointMercatorToPoint2d(pointMercator: PointWebMercator): Point2d {
        let maxMercator = CoordinateConversion.getMaxMercator()
        return new Point2d(pointMercator.x, maxMercator.getTopLeft().y - pointMercator.y)
    }

    public static convertPoint2dToPointMercator(point2d: Point2d): PointWebMercator {
        let maxMercator = CoordinateConversion.getMaxMercator()
        return new PointWebMercator(point2d.x, maxMercator.getTopLeft().y - point2d.y)
    }

    public static convertBoxMercatorToBoxGeo(boxMercator: BoxWebMercator): BoxGeo {
        let boxGeo = new BoxGeo(CoordinateConversion.convertPointWebMercatorToPointGeo(boxMercator.getTopLeft()),
            CoordinateConversion.convertPointWebMercatorToPointGeo(boxMercator.getBottomRight()));
        return boxGeo;
    }

    public static convertBoxMercatorToBox2d(boxMercator: BoxWebMercator): Box2d {
        let topLeft = CoordinateConversion.convertPointMercatorToPoint2d(boxMercator.getTopLeft())
        let bottomRight = CoordinateConversion.convertPointMercatorToPoint2d(boxMercator.getBottomRight())
        return new Box2d(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    public static convertBox2dToBoxMercator(box2d: Box2d): BoxWebMercator {
        let topLeft = CoordinateConversion.convertPoint2dToPointMercator(box2d.getUpperLeft())
        let bottomRight = CoordinateConversion.convertPoint2dToPointMercator(box2d.getLowerRight())
        return new BoxWebMercator(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    /**
     * Assumes the 2d box is actually web mercator coordinates with origin in the upper left
     * @param boxMercator 
     */
    public static convertBox2dToBoxGeo(box2d: Box2d): BoxGeo {
        return this.convertBoxMercatorToBoxGeo(this.convertBox2dToBoxMercator(box2d));
    }

    public static convertBoxGeoToBoxMercator(boxGeo: BoxGeo): BoxWebMercator {
        let topLeft = CoordinateConversion.convertPointGeoToPointWebMercator(boxGeo.getTopLeft());
        let bottomRight = CoordinateConversion.convertPointGeoToPointWebMercator(boxGeo.getBottomRight());
        let boxMercator = new BoxWebMercator(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
        return boxMercator;
    }
}
