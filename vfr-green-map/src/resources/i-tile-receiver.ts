
import { Point2d } from 'coordinates'

export interface ITileReceiver {
    receiveTile(location: Point2d, tile: HTMLImageElement, data: any): void
}