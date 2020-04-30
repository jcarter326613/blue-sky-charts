import { Color } from '../color'
import { CanvasElement } from './canvas-element'

export class SubMapView extends CanvasElement {
    private backgroundColor: Color;
    
    constructor( ) {
        super();
        this.backgroundColor = new Color();
    }

    public setBackgroundColor(color: Color): void {
        this.backgroundColor = color;
    }

    public render(context: CanvasRenderingContext2D) {
        super.render(context);
        context.fillRect(0, 0, 50, 50);
    }
}