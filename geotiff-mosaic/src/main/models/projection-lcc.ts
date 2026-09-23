import { ProjectionExtents } from "./projection-extents"

export class ProjectionLcc {
    public datum: String | undefined
    public no_defs: Boolean | undefined
    public proj: String | undefined
    public units: String | undefined
    public extents: ProjectionExtents | undefined
    public lat0: Number | undefined
    public lat1: Number | undefined
    public lat2: Number | undefined
    public lon0: Number | undefined
    public x0: Number | undefined
    public y0: Number | undefined
    public lat_0: Number | undefined
    public lat_1: Number | undefined
    public lat_2: Number | undefined
    public lon_0: Number | undefined
    public x_0: Number | undefined
    public y_0: Number | undefined
}