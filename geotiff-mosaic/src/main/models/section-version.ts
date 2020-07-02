import { ChangeSet } from './change-set'
import { FileExtent } from './file-extent'

export class SectionVersion {
    public effectiveDate: string | undefined
    public fileExtent: FileExtent | undefined
    public maxZoom: number | undefined
    public tileWidth: number | undefined
    public version: string | undefined
    public imageWidth: number | undefined
    public imageWidthScale: number | undefined
    public imageHeight: number | undefined
    public imageHeightScale: number | undefined
    public changeSet: Record<string, ChangeSet> | undefined
}