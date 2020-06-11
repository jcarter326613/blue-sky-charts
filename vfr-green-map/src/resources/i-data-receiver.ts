
import { PointWebMercator } from 'coordinates'

export interface IDataReceiver {
    receiveData(location: PointWebMercator, data: any, dataAgeSeconds: number, immediate: boolean): void
}