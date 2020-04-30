import * as $ from 'jquery'
import { CanvasElement } from './canvas-element';
import { CoordinateConverstion } from '../coordinates/coordinate-conversion'
import { Point2d } from '../coordinates/point-2d'
import { PointRadial } from '../coordinates/point-radial'
import { SubMapView } from './sub-map-view';

export class NavigableMap extends CanvasElement {
    // Map state variables
    private context: CanvasRenderingContext2D | null;
    private mapViews: Array<SubMapPosition>;
    private origin: PointRadial;

    // Mouse event variables
    private isDragging: boolean;
    private mouseDownPoint: Point2d;
    private originalOrigin: PointRadial;

    // html element variables
    private containerWidth: number;
    private containerHeight: number;
    private containerDiv: JQuery<HTMLElement>;

    constructor(elementId: string) {
        super();

        this.mouseDownPoint = new Point2d();
        this.originalOrigin = new PointRadial();
        this.origin = new PointRadial();
        this.mapViews = new Array<SubMapPosition>();
        this.isDragging = false;

        let jQueryElement = $("#" + elementId);
        if (jQueryElement.length != 1) {
            throw new Error("Must specify a unique html element id to place the map in.");
        }
        this.containerDiv = jQueryElement;

        // Create canvas object and place it in the div
        let canvasObj = document.createElement("canvas");
        let canvasObjHtml = $(canvasObj);
        this.containerDiv.append(canvasObjHtml);

        this.containerWidth = 400;
        this.containerHeight = 300;
        canvasObjHtml.attr("width", "400px");
        canvasObjHtml.attr("height", "300px");
        this.context = canvasObj.getContext("2d");

        this.addMouseListeners();
        this.initializeMapModel();
        this.render();
    }

    private addMouseListeners(): void {
        this.containerDiv.mousedown((event: JQuery.Event) => this.mouseDown(event));
        this.containerDiv.mousemove((event: JQuery.Event) => this.mouseMove(event));
        this.containerDiv.mouseup((event: JQuery.Event) => this.mouseUp(event));
    }

    private initializeMapModel(): void {
        this.mapViews.push(new SubMapPosition(new SubMapView()));
        this.mapViews.push(new SubMapPosition(new SubMapView(), new PointRadial(0.1, 25)));
    }

    public render(context: CanvasRenderingContext2D | null = null): void {
        // Get the context objet and clear the drawing area
        if (context == null) {
            context = this.context;
            if ( context == null )
                return;
        }
        context.clearRect(0, 0, this.containerWidth, this.containerHeight);

        // Call the base render function
        super.render(context);

        //Setup the view transformation so that the radial origin is in the center of the viewing area
        context.save();
        context.translate(this.containerWidth / 2, this.containerHeight / 2);

        let origin2d = CoordinateConverstion.convertPointRadialToPoint2d(this.origin);
        context.translate(origin2d.x, origin2d.y);

        context.fillRect(-10, -5, 20, 10);
        /*
        this.mapViews.forEach(mapPosition => {
            if ( context == null )
                return;
            context.save();
            let radialPosition: PointRadial = mapPosition.getPosition();
            let position = CoordinateConverstion.convertPointRadialToPoint2d(radialPosition, new PointRadial());
            context.translate(position.x, position.y);

            mapPosition.getSubMapView().render(context);
            context.restore();
        })
        */

        context.restore();
    }

    /* Mouse handler events */
    private mouseDown(event: JQuery.Event): void {
        if ( event == undefined || event.offsetX == undefined || event.offsetY == undefined )
            return;

        // Detect if the mouse event is outside the canvas
        if (event.offsetX < 0 || this.containerWidth < event.offsetX ||
            event.offsetY < 0 || this.containerHeight < event.offsetY) {
            return;
        }

        // Continue with starting a drag state
        event.stopPropagation();
        event.preventDefault();

        this.mouseDownPoint = new Point2d();
        this.mouseDownPoint.x = event.offsetX;
        this.mouseDownPoint.y = event.offsetY;
        this.originalOrigin = this.origin.clone();
        this.isDragging = true;
    }

    private mouseMove(event: JQuery.Event): void {
        if (this.isDragging) {
            if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
                return;
    
            let originalOrigin2d = CoordinateConverstion.convertPointRadialToPoint2d(this.originalOrigin);
            this.origin.setRadius(this.originalOrigin.getRadius() + (event.offsetY - this.mouseDownPoint.y));
            this.origin.setAnglePercentage(this.originalOrigin.getAngle() + (event.offsetX - this.mouseDownPoint.x) / 200);

            //this.origin.x = this.originalOrigin.x + (event.offsetX - this.mouseDownPoint.x)
            //this.origin.y = this.originalOrigin.y + (event.offsetY - this.mouseDownPoint.y)
            this.render();
        }
    }

    private mouseUp(event: JQuery.Event): void {
        if (this.isDragging) {
            if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
                return;
    
            //this.origin.x = this.originalOrigin.x + (event.offsetX - this.mouseDownPoint.x);
            //this.origin.y = this.originalOrigin.y + (event.offsetY - this.mouseDownPoint.y);
            this.isDragging = false;
            this.render();
        }
    }
};

class SubMapPosition {
    private subMapView: SubMapView;
    private centerLocation: PointRadial;

    constructor(subMap: SubMapView, centerLocation: PointRadial = new PointRadial()) {
        this.subMapView = subMap;
        this.centerLocation = centerLocation;
    }

    public setPosition(location: PointRadial): void {
        this.centerLocation = location;
    }

    public getPosition(): PointRadial {
        return this.centerLocation;
    }

    public getSubMapView(): SubMapView {
        return this.subMapView;
    }
}