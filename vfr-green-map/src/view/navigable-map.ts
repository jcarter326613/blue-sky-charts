import * as $ from 'jquery'
import { Box2d } from '../coordinates/box-2d'
import { CoordinateConverstion } from '../coordinates/coordinate-conversion'
import { Point2d } from '../coordinates/point-2d'
import { PointGeo } from '../coordinates/point-geo'
import { PointRadial } from '../coordinates/point-radial'
import { SubMapModel } from '../models/sub-map-model'
import { SubMapView } from './sub-map-view'
import { TileProvider } from './tile-provider'

export class NavigableMap {
    private tileProvider: TileProvider;

    // Map state variables
    private context: CanvasRenderingContext2D | null;
    private mapViews: Array<SubMapPosition>;
    private origin: PointRadial;
    private readonly minOriginRadius: number

    // Mouse event variables
    private isDragging: boolean;
    private mouseDownPoint2d: Point2d;
    private mouseDownOriginRadial: PointRadial;

    // html element variables
    private containerWidth: number;
    private containerHeight: number;
    private containerDiv: JQuery<HTMLElement>;
    private debugDiv: JQuery<HTMLElement>;

    constructor(elementId: string) {
        this.tileProvider = new TileProvider();
        this.minOriginRadius = 1;

        this.mouseDownPoint2d = new Point2d();
        this.mouseDownOriginRadial = new PointRadial();

        this.origin = new PointRadial(0, this.minOriginRadius);
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
        this.render();
        this.retrieveConfiguration();
    }

    private addMouseListeners(): void {
        this.containerDiv.mousedown((event: JQuery.Event) => this.mouseDown(event));
        this.containerDiv.mousemove((event: JQuery.Event) => this.mouseMove(event));
        this.containerDiv.mouseup((event: JQuery.Event) => this.mouseUp(event));
    }

    private initializeMapModel(data: Record<string, SubMapModel>): void {
        for (let key in data) {
            // Create the new submap
            let subMapModel = data[key];

            if (subMapModel.fileExtent == null || subMapModel.imageWidth == null || subMapModel.imageHeight == null)
                continue;

            let subMapView = new SubMapView(this.tileProvider);
            if (!subMapView.initialize(subMapModel, key))
                continue;

            // Calculate the center of the submap
            let centerPointGeo = new PointGeo();
            if (subMapModel.fileExtent.topLeft.longitude < -90 && 
                subMapModel.fileExtent.bottomRight.longitude > 90) {
                centerPointGeo.longitude = (subMapModel.fileExtent.topLeft.longitude + 
                    subMapModel.fileExtent.bottomRight.longitude) / 2;
            } else {
                centerPointGeo.longitude = (subMapModel.fileExtent.topLeft.longitude + 
                    subMapModel.fileExtent.bottomRight.longitude) / 2;
            }
            centerPointGeo.latitude = (subMapModel.fileExtent.topLeft.latitude + 
                subMapModel.fileExtent.bottomRight.latitude) / 2;

            let bottomLeft = subMapModel.fileExtent.bottomLeft;
            let bottomAngleDiff = 0;
            if (bottomLeft.longitude > 0 && centerPointGeo.longitude < 0)
                bottomAngleDiff = centerPointGeo.longitude + 360 - bottomLeft.longitude
            else
                bottomAngleDiff = centerPointGeo.longitude - bottomLeft.longitude
            let bottomAngleDiffRadians = Math.PI * 2 * bottomAngleDiff / 360.0

            let centerPoint = new PointRadial();
            centerPoint.setAngleDegrees(centerPointGeo.longitude + 180)
            let centerPointRadius = 0.5 * (subMapModel.imageWidth / Math.tan(bottomAngleDiffRadians) - subMapModel.imageHeight)
            centerPoint.setRadius(centerPointRadius)

            // Add the map to the list of map views and set the origin to be the center of the map
            // This will need to change in the future to not set the center like this
            this.mapViews.push(new SubMapPosition(subMapView, centerPoint));
            this.origin = centerPoint;
        }
    }

    private retrieveConfiguration(): void {
        let thisObj = this;
        $.getJSON("/metadata.json",
            function(data: Record<string, SubMapModel>) {
                thisObj.initializeMapModel(data);
                thisObj.render();
            });
    }

    public render(context: CanvasRenderingContext2D | null = null): void {
        // Get the context objet and clear the drawing area
        if (context == null) {
            context = this.context;
            if ( context == null )
                return;
        }
        context.clearRect(0, 0, this.containerWidth, this.containerHeight);
 
        //Setup the view transformation so that the radial origin is in the center of the viewing area
        context.save();
        
        context.translate(this.containerWidth / 2, 0);

        context.translate(0, -this.origin.getRadius());
        context.rotate(this.origin.getAngleRadians());

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
            let originalWidth = mapPosition.getSubMapView().getOriginalWidth();
            let originalHeight = mapPosition.getSubMapView().getOriginalHeight()
            context.translate(position.x, position.y);
            context.rotate(-radialPosition.getAngleRadians())
            context.translate(-originalWidth / 2, -originalHeight / 2);

            let viewport = new Box2d(originalWidth / 2 - this.containerWidth / 2, 
                originalHeight / 2 - this.containerHeight / 2,
                originalWidth / 2 + this.containerWidth / 2, 
                originalHeight / 2 + this.containerHeight / 2)

            mapPosition.getSubMapView().render(context, viewport, 1);
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

        this.mouseDownPoint2d = new Point2d(event.offsetX, event.offsetY);
        this.mouseDownOriginRadial = this.origin.clone();
        this.isDragging = true;
    }

    private mouseMove(event: JQuery.Event): void {
        if (this.isDragging) {
            if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
                return;
    
            let xMouseDownOffsetFromCenter = this.mouseDownPoint2d.x - this.containerWidth / 2;
            let yMouseDownOffsetFromTrueOrigin = this.mouseDownPoint2d.y + this.mouseDownOriginRadial.getRadius();
            let angleMouseDownFromTrueOrigin = Math.atan(xMouseDownOffsetFromCenter / yMouseDownOffsetFromTrueOrigin);
            let rMouseDownFromTrueOrigin = xMouseDownOffsetFromCenter / Math.sin(angleMouseDownFromTrueOrigin);

            let xMouseMoveOffsetFromCenter = event.offsetX - this.containerWidth / 2;
            let yMouseMoveOffsetFromTrueOrigin = event.offsetY + this.mouseDownOriginRadial.getRadius();
            let angleMouseMoveFromTrueOrigin = Math.atan(xMouseMoveOffsetFromCenter / yMouseMoveOffsetFromTrueOrigin);

            let angleDifference = angleMouseMoveFromTrueOrigin - angleMouseDownFromTrueOrigin;
            let properMouseMoveAngleFromTrueOrigin = Math.asin(xMouseMoveOffsetFromCenter / rMouseDownFromTrueOrigin);
            let properMouseMoveYFromTrueOrigin = Math.cos(properMouseMoveAngleFromTrueOrigin) * rMouseDownFromTrueOrigin;
            let properMouseMoveAngleDifference = properMouseMoveAngleFromTrueOrigin - angleMouseMoveFromTrueOrigin;

            let newOriginRadial = new PointRadial();
            newOriginRadial.setAngleRadians(this.mouseDownOriginRadial.getAngleRadians() - (angleDifference + 
                properMouseMoveAngleDifference));
            newOriginRadial.setRadius(properMouseMoveYFromTrueOrigin - event.offsetY);
            if ( newOriginRadial.getRadius() >= this.minOriginRadius )
                this.origin = newOriginRadial;

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