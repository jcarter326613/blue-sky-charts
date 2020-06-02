
import { Box2d, Point2d } from 'coordinates'

export interface ITileReceiver {
    receiveTile(location: Point2d, subsection: Box2d, tile: HTMLImageElement, data: any, immediate: boolean): void
}
