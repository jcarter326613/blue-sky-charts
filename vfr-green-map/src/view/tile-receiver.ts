
import { Point2d } from '../coordinates/point-2d'

export interface TileReceiver {
    receiveTile(location: Point2d, tile: HTMLImageElement, data: any): void
}