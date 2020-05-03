import * as $ from 'jquery'
import { CanvasElement } from './canvas-element';
import { CoordinateConverstion } from '../coordinates/coordinate-conversion'
import { Point2d } from '../coordinates/point-2d'
import { PointRadial } from '../coordinates/point-radial'
import { SubMapView } from './sub-map-view';
import { Color } from '../color';

export class NavigableMap extends CanvasElement {
    // Map state variables
    private context: CanvasRenderingContext2D | null;
    private mapViews: Array<SubMapPosition>;
    private origin: Point2d;
    private readonly minOriginRadius: number

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
    private debugDiv: JQuery<HTMLElement>;

    constructor(elementId: string) {
        super();

        this.minOriginRadius = 1;

        this.mouseDownPointGlobal2d = new Point2d();
        this.mouseDownPointGlobalRadial = new PointRadial();
        this.mouseDownPoint2d = new Point2d();
        this.originalOriginGlobal2d = new Point2d();
        this.originalOriginGlobalRadial = new PointRadial();

        this.origin = new Point2d(0, this.minOriginRadius);
        this.mapViews = new Array<SubMapPosition>();
        this.isDragging = false;

        let jQueryElement = $("#" + elementId);
        if (jQueryElement.length != 1) {
            throw new Error("Must specify a unique html element id to place the map in.");
        }
        this.containerDiv = jQueryElement;

        // Create a debug text div
        this.debugDiv = $(document.createElement("div"));
        this.containerDiv.append(this.debugDiv);

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
        this.mapViews.push(new SubMapPosition(new SubMapView(), new PointRadial(0.25, 150)));
        this.mapViews.push(new SubMapPosition(new SubMapView(), new PointRadial(0, 150)));
        this.mapViews.push(new SubMapPosition(new SubMapView(), new PointRadial(-0.25 / 2, 150)));

        this.mapViews[1].getSubMapView().setBackgroundColor(new Color(255, 0, 0));
        this.mapViews[1].getSubMapView().setBackgroundColor(new Color(0, 255, 0));
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
        
        context.translate(this.containerWidth / 2, 0);

        let originRadial = CoordinateConverstion.convertPoint2dToPointRadial(this.origin);
        context.translate(0, -originRadial.getRadius());
        context.rotate(originRadial.getAngleRadians());

        context.fillRect(-2, 0, 4, 500);
        context.fillRect(0, -2, 500, 4);
        context.rotate(-Math.PI / 4)
        context.fillRect(-2, 0, 4, 500);
        context.rotate(Math.PI / 2)
        context.fillRect(-2, 0, 4, 500);
        context.rotate(-Math.PI / 4)

        context.save()
        context.fillStyle = "rgb(0, 0, 255)";
        context.fillRect(-2, 0, 4, -500);
        context.restore();

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

        let x2d = event.offsetX - (this.containerWidth / 2) + this.origin.x;
        let y2d = event.offsetY + this.origin.y;
        
        this.mouseDownPointGlobal2d = new Point2d(x2d, y2d);
        this.mouseDownPointGlobalRadial = CoordinateConverstion.convertPoint2dToPointRadial(this.mouseDownPointGlobal2d);
        this.mouseDownPoint2d = new Point2d(event.offsetX, event.offsetY);
        this.originalOriginGlobal2d = this.origin.clone()
        this.originalOriginGlobalRadial = CoordinateConverstion.convertPoint2dToPointRadial(this.origin);
        this.isDragging = true;
    }

    private mouseMove(event: JQuery.Event): void {
        if (this.isDragging) {
            if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
                return;
    
            let xTravel = event.offsetX - this.mouseDownPoint2d.x;
            let yTravel = event.offsetY - this.mouseDownPoint2d.y;

            let xMouseDownOffsetFromCenter = this.mouseDownPoint2d.x - this.containerWidth / 2;
            let yMouseDownOffsetFromTrueOrigin = this.mouseDownPoint2d.y + this.originalOriginGlobalRadial.getRadius();
            let angleMouseDownFromTrueOrigin = Math.atan(xMouseDownOffsetFromCenter / yMouseDownOffsetFromTrueOrigin);
            let rMouseDownFromTrueOrigin = xMouseDownOffsetFromCenter / Math.sin(angleMouseDownFromTrueOrigin);

            let xMouseMoveOffsetFromCenter = event.offsetX - this.containerWidth / 2;
            let yMouseMoveOffsetFromTrueOrigin = event.offsetY + this.originalOriginGlobalRadial.getRadius();
            let angleMouseMoveFromTrueOrigin = Math.atan(xMouseMoveOffsetFromCenter / yMouseMoveOffsetFromTrueOrigin);

            let angleDifference = angleMouseMoveFromTrueOrigin - angleMouseDownFromTrueOrigin;
            let properMouseMoveAngleFromTrueOrigin = Math.asin(xMouseMoveOffsetFromCenter / rMouseDownFromTrueOrigin);
            let properMouseMoveYFromTrueOrigin = Math.cos(properMouseMoveAngleFromTrueOrigin) * rMouseDownFromTrueOrigin;
            let properMouseMoveAngleDifference = properMouseMoveAngleFromTrueOrigin - angleMouseMoveFromTrueOrigin;

            let newOriginRadial = new PointRadial();
            newOriginRadial.setAngleRadians(this.originalOriginGlobalRadial.getAngleRadians() - (angleDifference + 
                properMouseMoveAngleDifference));
            newOriginRadial.setRadius(properMouseMoveYFromTrueOrigin - event.offsetY);
            if ( newOriginRadial.getRadius() >= this.minOriginRadius )
                this.origin = CoordinateConverstion.convertPointRadialToPoint2d(newOriginRadial);

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