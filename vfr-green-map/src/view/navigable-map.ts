
import * as $ from 'jquery'
import { JQueryMousewheelEventObject } from '../types/jquery-mousewheel'
import { Box2d } from '../coordinates/box-2d'
import { CoordinateConverstion } from '../coordinates/coordinate-conversion'
import { Point2d } from '../coordinates/point-2d'
import { PointGeo } from '../coordinates/point-geo'
import { PointRadial } from '../coordinates/point-radial'
import { SubMapModel } from '../models/sub-map-model'
import { SubMapView } from './sub-map-view'
import { TileProvider } from './tile-provider'

declare function require(module: string): any;

export class NavigableMap {
    private tileProvider: TileProvider;

    // Map state variables
    private context: CanvasRenderingContext2D | null;
    private mapViews: Array<SubMapPosition>;
    private origin: PointRadial;
    private readonly minOriginRadius: number;
    private scale: number;
    private scaleDriver: number;
    private readonly maxScaleDriver: number;

    // Mouse event variables
    private isDragging: boolean;
    private mouseDownPoint2d: Point2d;
    private mouseDownOriginRadial: PointRadial;

    // html element variables
    private containerWidth: number;
    private containerHeight: number;
    private canvasObjHtml: JQuery<HTMLElement>;
    private containerDiv: JQuery<HTMLElement>;
    //private debugDiv: JQuery<HTMLElement>;

    constructor(elementId: string) {
        require("jquery-mousewheel");

        this.tileProvider = new TileProvider();
        this.minOriginRadius = 1;
        this.maxScaleDriver = 20;
        this.scaleDriver = 10;
        this.scale = 1 / this.scaleDriver;
        this.containerWidth = 0;
        this.containerHeight = 0;

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
        //this.debugDiv = $(document.createElement("div"));
        //this.containerDiv.append(this.debugDiv);

        // Create canvas object and place it in the div
        let canvasObj = document.createElement("canvas");
        this.canvasObjHtml = $(canvasObj);
        this.containerDiv.append(this.canvasObjHtml);
        this.context = canvasObj.getContext("2d");

        this.setSize();
        this.addEventListeners();
        this.render();
        this.retrieveConfiguration();
    }

    private addEventListeners(): void {
        this.containerDiv.mousedown((event: JQuery.Event) => this.mouseDown(event));
        this.containerDiv.mousemove((event: JQuery.Event) => this.mouseMove(event));
        this.containerDiv.mouseup((event: JQuery.Event) => this.mouseUp(event));
        this.containerDiv.mousewheel((event: JQueryMousewheelEventObject) => this.mouseScroll(event))

        // Setup the window resize listener
        let thisObject = this;
        $(window).resize(function() {
            requestAnimationFrame(function(){
                thisObject.setSize();
                thisObject.render();
            })
        });
    }

    private setSize(): void {
        let width = this.containerDiv.width();
        let height = this.containerDiv.height();

        if ( width !== undefined && height !== undefined ) {
            this.containerWidth = width;
            this.containerHeight = height;
            this.canvasObjHtml.attr("width", width);
            this.canvasObjHtml.attr("height", height);
        }
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
            if (subMapModel.fileExtent.topLeft.longitude < -90 &&       //The contents of this if statement are wrong
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
            this.origin = centerPoint.clone();
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

    public render(): void {
        if (this.context == null)
            return;

        let context = this.context;
        context.save();
        context.clearRect(0, 0, this.containerWidth, this.containerHeight);

        this.mapViews.forEach(mapPosition => {
            if ( context == null )
                return;
            context.save();

            // Calculate the viewport from the perspective of the un modified sub map
            let radialPosition: PointRadial = mapPosition.getPosition();

            let originalWidth = mapPosition.getSubMapView().getOriginalWidth();
            let originalHeight = mapPosition.getSubMapView().getOriginalHeight();
            let viewport = this.calculateViewport(radialPosition);
            // Since the viewport is calculated as an area around the center of the map in the original unscaled size, we need to translate the
            // viewport over the center of the unscaled map
            viewport.upperLeft.x += originalWidth / 2;
            viewport.lowerRight.x += originalWidth / 2;
            viewport.upperLeft.y += originalHeight / 2;
            viewport.lowerRight.y += originalHeight / 2;

            let angleDiff = radialPosition.getAngleRadians() - this.origin.getAngleRadians();
            context.translate(this.containerWidth / 2, 0);
            context.translate(0, -this.origin.getRadius() * this.scale);
            context.rotate(-angleDiff);
            context.translate(0, radialPosition.getRadius() * this.scale);
            context.translate(-originalWidth * this.scale / 2, -originalHeight * this.scale / 2);

            // Draw the submap
            mapPosition.getSubMapView().render(context, viewport, this.scale);
            context.restore();
        })

        context.restore();
    }

    /**
     * Returns the viewport in unscaled coordinates.  The viewport shrinks below the origin as the scale increases.
     * @param radialPosition The center position of the map we are calculatign the viewport position for.
     * @returns The viewport relative to the map position given with -y pointing towards the center of the radial
     *  coordinate system.
     */
    private calculateViewport(radialPosition: PointRadial): Box2d {
        // Find the v corners in radial coordinates
        let topAngleDiff = Math.atan((this.containerWidth / (2 * this.scale)) / this.origin.getRadius());
        let upperLeft = new PointRadial();
        let upperRight = new PointRadial();
        upperLeft.setAngleRadians(this.origin.getAngleRadians() - topAngleDiff)
        upperRight.setAngleRadians(this.origin.getAngleRadians() + topAngleDiff)
        let upperRadius = this.origin.getRadius() / Math.cos(topAngleDiff)
        upperLeft.setRadius(upperRadius);
        upperRight.setRadius(upperRadius);

        let bottomAngleDiff = Math.atan((this.containerWidth / (2 * this.scale)) / 
            (this.origin.getRadius() + (this.containerHeight / this.scale)));
        let bottomLeft = new PointRadial();
        let bottomRight = new PointRadial();
        bottomLeft.setAngleRadians(this.origin.getAngleRadians() - bottomAngleDiff)
        bottomRight.setAngleRadians(this.origin.getAngleRadians() + bottomAngleDiff)
        let lowerRadius = (this.origin.getRadius() + (this.containerHeight / this.scale)) / Math.cos(bottomAngleDiff)
        bottomLeft.setRadius(lowerRadius);
        bottomRight.setRadius(lowerRadius);

        // Convert v corners to 2d relative to map center
        let upperLeft2d = CoordinateConverstion.getRelativePoint2d(upperLeft, radialPosition);
        let upperRight2d = CoordinateConverstion.getRelativePoint2d(upperRight, radialPosition);
        let bottomLeft2d = CoordinateConverstion.getRelativePoint2d(bottomLeft, radialPosition);
        let bottomRight2d = CoordinateConverstion.getRelativePoint2d(bottomRight, radialPosition);

        // Expand corners
        let finalViewport = new Box2d();
        finalViewport.upperLeft.x = Math.min(upperLeft2d.x, upperRight2d.x, bottomLeft2d.x, bottomRight2d.x);
        finalViewport.upperLeft.y = Math.min(upperLeft2d.y, upperRight2d.y, bottomLeft2d.y, bottomRight2d.y);
        finalViewport.lowerRight.x = Math.max(upperLeft2d.x, upperRight2d.x, bottomLeft2d.x, bottomRight2d.x);
        finalViewport.lowerRight.y = Math.max(upperLeft2d.y, upperRight2d.y, bottomLeft2d.y, bottomRight2d.y);
        return finalViewport;
    }

    /* Mouse handler events */
    private mouseScroll(event: JQueryMousewheelEventObject): void {
        if ( event === undefined )
            return;

        // Detect if the mouse event is outside the canvas
        if (event.offsetX < 0 || this.containerWidth < event.offsetX ||
            event.offsetY < 0 || this.containerHeight < event.offsetY) {
            return;
        }

        event.stopPropagation();
        event.preventDefault();

        //let zoomAmount = event.deltaY * event.deltaFactor;

        if (event.deltaY > 0 ) {
            this.scaleDriver += 1;
            if ( this.scaleDriver > this.maxScaleDriver ) {
                this.scaleDriver = this.maxScaleDriver;
            }
        } else {
            this.scaleDriver -= 1;
            if (this.scaleDriver < 1) {
                this.scaleDriver = 1;
            }
        }
        this.scale = 1 / this.scaleDriver;
        this.render();
    }

    private mouseDown(event: JQuery.Event): void {
        if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
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
    
            let xMouseDownOffsetFromCenter = (this.mouseDownPoint2d.x - this.containerWidth / 2) / this.scale;
            let yMouseDownOffsetFromTrueOrigin = this.mouseDownPoint2d.y / this.scale + this.mouseDownOriginRadial.getRadius();
            let angleMouseDownFromTrueOrigin = Math.atan(xMouseDownOffsetFromCenter / yMouseDownOffsetFromTrueOrigin);
            let rMouseDownFromTrueOrigin = xMouseDownOffsetFromCenter / Math.sin(angleMouseDownFromTrueOrigin);

            let xMouseMoveOffsetFromCenter = (event.offsetX - this.containerWidth / 2) / this.scale;
            let yMouseMoveOffsetFromTrueOrigin = event.offsetY / this.scale + this.mouseDownOriginRadial.getRadius();
            let angleMouseMoveFromTrueOrigin = Math.atan(xMouseMoveOffsetFromCenter / yMouseMoveOffsetFromTrueOrigin);

            let angleDifference = angleMouseMoveFromTrueOrigin - angleMouseDownFromTrueOrigin;
            let properMouseMoveAngleFromTrueOrigin = Math.asin(xMouseMoveOffsetFromCenter / rMouseDownFromTrueOrigin);
            let properMouseMoveYFromTrueOrigin = Math.cos(properMouseMoveAngleFromTrueOrigin) * rMouseDownFromTrueOrigin;
            let properMouseMoveAngleDifference = properMouseMoveAngleFromTrueOrigin - angleMouseMoveFromTrueOrigin;

            let newOriginRadial = new PointRadial();
            newOriginRadial.setAngleRadians(this.mouseDownOriginRadial.getAngleRadians() - (angleDifference + 
                properMouseMoveAngleDifference));
            newOriginRadial.setRadius(properMouseMoveYFromTrueOrigin - event.offsetY / this.scale);
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