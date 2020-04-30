import * as $ from 'jquery'
import { CanvasElement } from './canvas-element';
import { CoordinateConverstion } from '../coordinates/coordinate-conversion'
import { Point2d } from '../coordinates/point-2d'
import { PointRadial } from '../coordinates/point-radial'
import { SubMapView } from './sub-map-view';

export class NavigableMap extends CanvasElement {
    private containerDiv: JQuery<HTMLElement>;
    private mapViews: Array<SubMapPosition>;
    private isDragging: boolean;
    private mouseDownPoint: Point2d;
    private origin: Point2d;
    private originalOrigin: Point2d;
    private context: CanvasRenderingContext2D | null;

    private containerWidth: number;
    private containerHeight: number;

    constructor(elementId: string) {
        super();

        this.mouseDownPoint = new Point2d();
        this.originalOrigin = new Point2d();
        this.mapViews = new Array<SubMapPosition>();
        this.isDragging = false;
        this.origin = new Point2d();

        let jQueryElement = $("#" + elementId);
        if (jQueryElement.length != 1) {
            throw new Error("Must specify a unique html element id to place the map in.");
        }
        this.containerDiv = jQueryElement;

        // Create canvas object and place it in the div
        let canvasObj = document.createElement("canvas");
        let canvasObjHtml = $(canvasObj);
        this.containerDiv.append(canvasObjHtml);

        this.containerWidth = 300;
        this.containerHeight = 400;
        canvasObjHtml.attr("width", "300px");
        canvasObjHtml.attr("height", "400px");
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
        if (context == null) {
            context = this.context;
            if ( context == null )
                return;
        }
        context.clearRect(0, 0, this.containerWidth, this.containerHeight);

        super.render(context);

        context.save();
        context.translate(this.origin.x, this.origin.y);

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
    
            this.origin.x = this.originalOrigin.x + (event.offsetX - this.mouseDownPoint.x)
            this.origin.y = this.originalOrigin.y + (event.offsetY - this.mouseDownPoint.y)
            this.render();
        }
    }

    private mouseUp(event: JQuery.Event): void {
        if (this.isDragging) {
            if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
                return;
    
            this.origin.x = this.originalOrigin.x + (event.offsetX - this.mouseDownPoint.x);
            this.origin.y = this.originalOrigin.y + (event.offsetY - this.mouseDownPoint.y);
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