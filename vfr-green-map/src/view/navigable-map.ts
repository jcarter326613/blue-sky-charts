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
    private mouseDownPointGlobal2d: Point2d;
    private mouseDownPointGlobalRadial: PointRadial;
    private mouseDownPoint2d: Point2d;
    private originalOriginGlobal2d: Point2d;
    private originalOriginGlobalRadial: PointRadial;

    // html element variables
    private containerWidth: number;
    private containerHeight: number;
    private containerDiv: JQuery<HTMLElement>;

    constructor(elementId: string) {
        super();

        this.mouseDownPointGlobal2d = new Point2d();
        this.mouseDownPointGlobalRadial = new PointRadial();
        this.mouseDownPoint2d = new Point2d();
        this.originalOriginGlobal2d = new Point2d();
        this.originalOriginGlobalRadial = new PointRadial();

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
        canvasObjHtml.css("border-width", "5px");
        canvasObjHtml.css("border-color", "black");
        canvasObjHtml.css("border-style", "solid");
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

        this.mapViews.forEach(mapPosition => {
            if ( context == null )
                return;
            context.save();
            let radialPosition: PointRadial = mapPosition.getPosition();
            let position = CoordinateConverstion.convertPointRadialToPoint2d(radialPosition);
            context.translate(position.x, position.y);

            mapPosition.getSubMapView().render(context);
            context.restore();
        })

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

        let origin2d = CoordinateConverstion.convertPointRadialToPoint2d(this.origin);
        let x2d = event.offsetX - (this.containerWidth / 2) + origin2d.x;
        let y2d = event.offsetY - (this.containerHeight / 2) + origin2d.y;
        
        this.mouseDownPointGlobal2d = new Point2d(x2d, y2d);
        this.mouseDownPointGlobalRadial = CoordinateConverstion.convertPoint2dToPointRadial(this.mouseDownPointGlobal2d);
        this.mouseDownPoint2d = new Point2d(event.offsetX, event.offsetY);
        this.originalOriginGlobal2d = CoordinateConverstion.convertPointRadialToPoint2d(this.origin);
        this.originalOriginGlobalRadial = this.origin.clone();
        this.isDragging = true;
    }

    private mouseMove(event: JQuery.Event): void {
        if (this.isDragging) {
            if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
                return;
    
            let xTravel = event.offsetX - this.mouseDownPoint2d.x;
            let yTravel = event.offsetY - this.mouseDownPoint2d.y;
            let newMouseDownPointGlobal2d = new Point2d(this.mouseDownPointGlobal2d.x + xTravel,
                this.mouseDownPointGlobal2d.y + yTravel);
            let newMouseDownPointGlobalRadial = CoordinateConverstion.convertPoint2dToPointRadial(newMouseDownPointGlobal2d);
            let angleDelta = newMouseDownPointGlobalRadial.getAngle() - this.mouseDownPointGlobalRadial.getAngle();
            let radiusDelta = newMouseDownPointGlobalRadial.getRadius() - this.mouseDownPointGlobalRadial.getRadius();

            let newOriginGlobalRadial = new PointRadial(this.originalOriginGlobalRadial.getAngle() + angleDelta,
                this.originalOriginGlobalRadial.getRadius() + radiusDelta);
            if (newOriginGlobalRadial.getRadius() < 0)
                newOriginGlobalRadial.setRadius(0);
            this.origin = newOriginGlobalRadial;

            this.render();
        }
    }

    private mouseUp(event: JQuery.Event): void {
        this.isDragging = false;
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