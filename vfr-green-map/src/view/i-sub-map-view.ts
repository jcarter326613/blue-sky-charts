import { Box2d, BoxWebMercator } from 'coordinates'
import { SubMapModel } from '../models/sub-map-model'

export interface ISubMapView {
    /**
     * Initializes the view
     * @param model 
     * @param name 
     * @returns A BoxGeo representing the full extent of the map.  undefined if this map should be discarded from view because of a
     *  bad configuration.
     */
    initialize(model: SubMapModel, name: string): BoxWebMercator | undefined;

    /**
     * Some original width and height in any unit which is used determine the region parameter into the render function.
     */
    getOriginalWidth(): number;
    getOriginalHeight(): number;

    /**
     * Draws or queues the drawing of this sub map.  The number of pixels to be drawn are the region width * height * scale
     * @param context 
     * @param region The region in relation to the original width and height
     * @param scale All aspects of the region are multiplied by this value
     */
    render(context: CanvasRenderingContext2D, region: Box2d, scale: number): void;
}