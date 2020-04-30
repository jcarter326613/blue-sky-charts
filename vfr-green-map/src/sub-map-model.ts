
export class SubMapModel {
    private originX: number;
    private originY: number;
    private pixelXHeight: number;
    private pixelYHeight: number;
    private pixelWidth: number;
    private pixelHeight: number;

    constructor(originX: number, originY: number, pixelXHeight: number, pixelYHeight: number, pixelWidth: number, pixelHeight: number) {
        this.originX = originX;
        this.originY = originY;
        this.pixelXHeight = pixelXHeight;
        this.pixelYHeight = pixelYHeight;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
    }
}