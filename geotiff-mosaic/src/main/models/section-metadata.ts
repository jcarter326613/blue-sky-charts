import { FileExtent } from './file-extent'

export class SectionMetadata {
    public fileExtent: FileExtent | undefined
    public maxZoom: number | undefined
    public tileWidth: number | undefined
    public version: string | undefined
    public imageWidth: number | undefined
    public imageHeight: number | undefined
}