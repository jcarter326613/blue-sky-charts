import { ChangeSet } from './change-set'
import { FileExtent } from './file-extent'
import { ProjectionLcc } from './projection-lcc'

export class SectionVersion {
    public effectiveDate: string | undefined
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
    public changeSet: Record<string, ChangeSet> | undefined
    public originalProjectionBounds: Array<Number> | undefined
    public originalProjectionData: ProjectionLcc | undefined
    public projectionLcc: ProjectionLcc | undefined
}