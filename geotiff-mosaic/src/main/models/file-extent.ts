import { PointGeoModel } from "./point-geo-model"

export class FileExtent {
    public bottomRight: PointGeoModel | undefined
    public topLeft: PointGeoModel | undefined
}