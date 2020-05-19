
import { Point2d } from 'coordinates'

export interface IDataReceiver {
    receiveData(location: Point2d, data: any): void
}