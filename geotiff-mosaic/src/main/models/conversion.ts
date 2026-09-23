import { BoxGeo, PointGeo } from 'coordinates'
import { FileExtent } from './file-extent'
import { PointGeoModel } from './point-geo-model'

export class Conversion {
    public static convertFileExtentToBoxGeo(fileExtent: FileExtent): BoxGeo | undefined {
        if ( fileExtent.topLeft === undefined || fileExtent.topLeft.latitude === undefined || fileExtent.topLeft.longitude === undefined ||
            fileExtent.bottomRight === undefined || fileExtent.bottomRight.latitude === undefined || fileExtent.bottomRight.longitude === undefined ) {
            return undefined
        }
        return new BoxGeo(new PointGeo(fileExtent.topLeft.longitude, fileExtent.topLeft.latitude), 
            new PointGeo(fileExtent.bottomRight.longitude, fileExtent.bottomRight.latitude))
    }

    public static convertBoxGeoToFileExtent(box: BoxGeo): FileExtent {
        let extent = new FileExtent()
        extent.topLeft = new PointGeoModel()
        extent.topLeft.latitude = box.getTopLeft().latitude
        extent.topLeft.longitude = box.getTopLeft().longitude

        extent.bottomRight = new PointGeoModel()
        extent.bottomRight.latitude = box.getBottomRight().latitude
        extent.bottomRight.longitude = box.getBottomRight().longitude

        return extent
    }
}