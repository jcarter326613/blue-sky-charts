
import * as $ from 'jquery'
import { JQueryMousewheelEventObject } from '../types/jquery-mousewheel'
import { Box2d } from '../coordinates/box-2d'
import { BoxGeo } from '../coordinates/box-geo'
import { CoordinateConverstion } from '../coordinates/coordinate-conversion'
import { Point2d } from '../coordinates/point-2d'
import { PointGeo } from '../coordinates/point-geo'
import { PointRadial } from '../coordinates/point-radial'
import { SubMapModel } from '../models/sub-map-model'
import { SubMapView } from './sub-map-view'
import { TileProvider } from './tile-provider'

declare function require(module: string): any;

export class NavigableMap2d {
    private tileProvider: TileProvider;

    // Map state variables
    private context: CanvasRenderingContext2D | null;
    private mapViews: Array<SubMapPosition>;
    private origin: PointGeo;
    private scale: number;
    private scaleDriver: number;
    private readonly maxScaleDriver: number;
    private readonly zoom0PixelsPerLongitude: number;

    // Mouse event variables
    private isDragging: boolean;
    private mouseDownPoint2d: Point2d;
    private mouseDownOriginGeo: PointGeo;

    // html element variables
    private containerWidth: number;
    private containerHeight: number;
    private canvasObjHtml: JQuery<HTMLElement>;
    private containerDiv: JQuery<HTMLElement>;
    //private debugDiv: JQuery<HTMLElement>;

    constructor(elementId: string) {
        require("jquery-mousewheel");

        this.tileProvider = new TileProvider();
        this.maxScaleDriver = 60;
        this.scaleDriver = 10;
        this.scale = 1 / this.scaleDriver;
        this.containerWidth = 0;
        this.containerHeight = 0;
        this.zoom0PixelsPerLongitude = 20;

        this.mouseDownPoint2d = new Point2d();
        this.mouseDownOriginGeo = new PointGeo();

        this.origin = new PointGeo();
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

            let fileExtent = BoxGeo.createFromModel(subMapModel.fileExtent);
            let subMapView = new SubMapView(this.tileProvider);
            if (!subMapView.initialize(subMapModel, key))
                continue;

            // Add the map to the list of map views and set the origin to be the center of the map
            // This will need to change in the future to not set the center like this
            this.mapViews.push(new SubMapPosition(subMapView, fileExtent));
            this.origin = fileExtent.getTopLeft();
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

        let viewport = this.calculateViewport();
        let viewport2dTopLeft = CoordinateConverstion.convertToWebMercator(viewport.getTopLeft());
        let viewport2dBottomRight = CoordinateConverstion.convertToWebMercator(viewport.getBottomRight());
        let viewport2d = new Box2d(viewport2dTopLeft.x, viewport2dTopLeft.y,
            viewport2dBottomRight.x, viewport2dBottomRight.y);

        let context = this.context;
        context.save();
        context.clearRect(0, 0, this.containerWidth, this.containerHeight);

        this.mapViews.forEach(submap => {
            if ( context == null )
                return;
            context.save();

            // Calculate the viewport from the perspective of the un modified sub map
            let mapPosition: BoxGeo = submap.getPosition();
            let viewportOverlap = mapPosition.union(viewport);
            if (viewportOverlap != null) {
                let viewportOverlap2dTopLeft = CoordinateConverstion.convertToWebMercator(viewportOverlap.getTopLeft());
                let viewportOverlap2dBottomRight = CoordinateConverstion.convertToWebMercator(viewportOverlap.getBottomRight());
                let viewportOverlap2d = new Box2d(viewportOverlap2dTopLeft.x, viewportOverlap2dTopLeft.y,
                    viewportOverlap2dBottomRight.x, viewportOverlap2dBottomRight.y);

                let map2dTopLeft = CoordinateConverstion.convertToWebMercator(mapPosition.getTopLeft());
                let map2dBottomRight = CoordinateConverstion.convertToWebMercator(mapPosition.getBottomRight());

                let originalWidth = submap.getSubMapView().getOriginalWidth();
                let originalHeight = submap.getSubMapView().getOriginalHeight();

                // Assumes viewport can't be negative facing
                let widthPercentageOfViewport = (mapPosition.getBottomRight().longitude - mapPosition.getTopLeft().longitude) /
                    (viewport.getBottomRight().longitude - viewport.getTopLeft().longitude);
                let viewportWidthPixels = this.containerWidth;
                let mapScale = (viewportWidthPixels * widthPercentageOfViewport) / originalWidth;

                context.translate(this.containerWidth / 2, this.containerHeight / 2);
                context.translate(-originalWidth * mapScale / 2, -originalHeight * mapScale / 2);

                // Figure out the part of the map we want to draw in 2d coordinates relative to the upper left corner
                let subMapDrawSection = new Box2d(
                    originalWidth * (viewportOverlap2dTopLeft.x - map2dTopLeft.x) / (map2dBottomRight.x - map2dTopLeft.x),
                    originalHeight * (viewportOverlap2dTopLeft.y - map2dTopLeft.y) / (map2dBottomRight.y - map2dTopLeft.y),
                    originalWidth * (viewportOverlap2dBottomRight.x - map2dTopLeft.x) / (map2dBottomRight.x - map2dTopLeft.x),
                    originalHeight * (viewportOverlap2dBottomRight.y - map2dTopLeft.y) / (map2dBottomRight.y - map2dTopLeft.y))

                // Draw the submap
                submap.getSubMapView().render(context, subMapDrawSection, mapScale);
            }
            context.restore();
        })

        context.restore();
    }

    /**
     * Returns the viewport in unscaled coordinates.
     */
    private calculateViewport(): BoxGeo {
        let origin2d = CoordinateConverstion.convertToWebMercator(this.origin);
        let widthBy2 = this.containerWidth * this.scale / 2;
        let heightBy2 = this.containerHeight * this.scale / 2;
        let viewport = new Box2d(origin2d.x - widthBy2, origin2d.y - heightBy2,
            origin2d.x + widthBy2, origin2d.y + heightBy2);
        if (viewport.upperLeft.x < 0)
            viewport.upperLeft.x = 0
        if (viewport.upperLeft.y < 0)
            viewport.upperLeft.y = 0
        if (viewport.lowerRight.x >= CoordinateConverstion.MAX_X_MERCATOR)
            viewport.lowerRight.x = CoordinateConverstion.MAX_X_MERCATOR
            if (viewport.lowerRight.y >= CoordinateConverstion.MAX_Y_MERCATOR)
                viewport.lowerRight.y = CoordinateConverstion.MAX_Y_MERCATOR
        let viewportGeo = new BoxGeo(CoordinateConverstion.convertFromWebMercator(viewport.upperLeft),
            CoordinateConverstion.convertFromWebMercator(viewport.lowerRight));
        return viewportGeo;
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

        if (event.deltaY < 0 ) {
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

        /*
        this.mouseDownPoint2d = new Point2d(event.offsetX, event.offsetY);
        this.mouseDownOriginRadial = this.origin.clone();
        this.isDragging = true;
        */
    }

    private mouseMove(event: JQuery.Event): void {
        if (this.isDragging) {
            if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
                return;
    
                /*
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
            */
        }
    }

    private mouseUp(event: JQuery.Event): void {
        this.isDragging = false;
    }
};

class SubMapPosition {
    private subMapView: SubMapView;
    private location: BoxGeo;

    constructor(subMap: SubMapView, location: BoxGeo) {
        this.subMapView = subMap;
        this.location = location;
    }

    public setPosition(location: BoxGeo): void {
        this.location = location;
    }

    public getPosition(): BoxGeo {
        return this.location;
    }

    public getSubMapView(): SubMapView {
        return this.subMapView;
    }
}