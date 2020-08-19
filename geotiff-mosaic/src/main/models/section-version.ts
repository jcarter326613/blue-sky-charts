import { ChangeSet } from './change-set'
import { FileExtent } from './file-extent'
import { ProjectionLcc } from './projection-lcc'
import { ProjectionWebMercator } from './projection-web-mercator'

export class SectionVersion {
    public effectiveDate: string | undefined
    public expirationDate: string | undefined
    public fileExtent: FileExtent | undefined
    public mosaicFileExtent: FileExtent | undefined
    public maxZoom: number | undefined
    public mosaicMaxZoom: number | undefined
    public tileWidth: number | undefined
    public version: string | undefined
    public imageWidth: number | undefined
    public mosaicImageWidth: number | undefined
    public imageHeight: number | undefined
    public mosaicImageHeight: number | undefined
    public changeSet: ChangeSet | undefined
    public originalProjectionBounds: Array<Number> | undefined
    public originalProjectionData: ProjectionLcc | undefined
    public projectionWebMercator: ProjectionWebMercator | undefined
    public projectionLcc: ProjectionLcc | undefined
}