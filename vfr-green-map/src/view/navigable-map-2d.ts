
import * as $ from 'jquery'
import { JQueryMousewheelEventObject } from '../types/jquery-mousewheel'
import { Box2d, PointWebMercator, PointGeo } from 'coordinates'
import { CoordinateConversion } from 'coordinates'
import { IMap } from './i-map'
import { ISubMapView } from './i-sub-map-view'
import { Point2d } from 'coordinates'
import { SubMapModel } from '../models/sub-map-model'
import { MapTileView } from './map-tile-view'
import { TileProvider } from '../resources/tile-provider'
import { MapDataView } from './map-data-view'
import { DataProvider } from '../resources/data-provider'

declare function require(module: string): any;

export class NavigableMap2d implements IMap {
    private tileProvider: TileProvider;
    private dataProvider: DataProvider;

    // Map state variables
    private context: CanvasRenderingContext2D | null;
    private mapViews: Array<SubMapPosition>;
    private origin2d: PointWebMercator;
    private mark: PointWebMercator | undefined;
    private scale: number;
    private scaleDriver: number;
    private readonly maxScaleDriver: number;
    private readonly zoom0PixelsPerLongitude: number;

    // Mouse event variables
    private isDragging: boolean;
    private isPinching: boolean;
    private mouseDownClient: Point2d;
    private mouseDownOrigin2d: Point2d;
    private touchMoveIdentifier: number;
    private pinchClientPoint1: Point2d;
    private pinchClientPoint2: Point2d;
    private pinchOriginalScale: number;

    // html element variables
    private containerWidth: number;
    private containerHeight: number;
    private canvasObjHtml: JQuery<HTMLElement>;
    private containerDiv: JQuery<HTMLElement>;
    //private debugDiv: JQuery<HTMLElement>;

    constructor(elementId: string, mapRoot: string, originLongitude: number | undefined, originLatitude: number | undefined,
        zoom: number | undefined, markLongitude: number | undefined, markLatitude: number | undefined) {
        require("jquery-mousewheel");

        // Set all parameterized defaults
        if ( zoom === undefined ) {
            this.scaleDriver = 2;
        } else {
            this.scaleDriver = zoom;
        }
        if ( originLongitude !== undefined && originLatitude !== undefined ) {
            this.origin2d = CoordinateConversion.convertToWebMercator(new PointGeo(originLongitude, originLatitude));
        } else {
            this.origin2d = new PointWebMercator();
        }
        if ( markLatitude !== undefined && markLongitude !== undefined ) {
            if ( zoom === undefined ) {
                this.scaleDriver = 8;
            }
            this.origin2d = CoordinateConversion.convertToWebMercator(new PointGeo(markLongitude, markLatitude));
            this.mark = this.origin2d;
        }

        // Set all constant and derived defaults
        this.tileProvider = new TileProvider(`${mapRoot}/sectional`);
        this.dataProvider = new DataProvider();
        this.maxScaleDriver = 12;
        if ( this.scaleDriver > this.maxScaleDriver ) {
            this.scaleDriver = this.maxScaleDriver;
        } else if (this.scaleDriver < 0) {
            this.scaleDriver = 0;
        }
        this.scale = 0;
        this.updateScale();
        this.containerWidth = 0;
        this.containerHeight = 0;
        this.zoom0PixelsPerLongitude = 20;

        this.mouseDownClient = new Point2d();
        this.mouseDownOrigin2d = new Point2d();
        this.touchMoveIdentifier = 0;

        this.mapViews = new Array<SubMapPosition>();
        this.isDragging = false;
        this.isPinching = false;
        this.pinchClientPoint1 = new Point2d();
        this.pinchClientPoint2 = new Point2d();
        this.pinchOriginalScale = 0;

        // Identify and store the html comtainer for this control
        let jQueryElement = $("#" + elementId);
        if (jQueryElement.length != 1) {
            throw new Error("Must specify a unique html element id to place the map in.");
        }
        this.containerDiv = jQueryElement;

        // Create a debug text div
        /*
        this.debugDiv = $(document.createElement("div"));
        this.debugDiv.attr("style", "font-size: 14px;")
        this.containerDiv.append(this.debugDiv);
        */

        // Create canvas object and place it in the div
        let canvasObj = document.createElement("canvas");
        this.canvasObjHtml = $(canvasObj);
        this.containerDiv.append(this.canvasObjHtml);
        this.context = canvasObj.getContext("2d");

        this.setSize();
        this.addEventListeners();
        this.render();
        this.retrieveConfiguration(`${mapRoot}/metadata.json`);
    }

    public requestRedraw(): void {
        this.render();
    }

    private updateScale(): void {
        this.scale = 1 / (2 ** this.scaleDriver);
    }

    private addEventListeners(): void {
        this.containerDiv.mousedown((event: JQuery.Event) => this.mouseDown(event));
        this.containerDiv.mousemove((event: JQuery.Event) => this.mouseMove(event));
        this.containerDiv.mouseup((event: JQuery.Event) => this.mouseUp(event));
        this.containerDiv.mousewheel((event: JQueryMousewheelEventObject) => this.mouseScroll(event))
        this.containerDiv.on("touchstart", (event: JQuery.Event) => this.touchStart(event));
        this.containerDiv.on("touchmove", (event: JQuery.Event) => this.touchMove(event));
        this.containerDiv.on("touchend", (event: JQuery.Event) => this.touchEnd(event));
        this.containerDiv.on("touchcancel", (event: JQuery.Event) => this.touchEnd(event));

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

            let subMapView = new MapTileView(this.tileProvider, this);
            let fileExtent = subMapView.initialize(subMapModel, key);
            if (fileExtent === undefined)
                continue;

            let fileExtent2dUpperLeft = CoordinateConversion.convertToWebMercator(fileExtent.getTopLeft());
            let fileExtent2dLowerRight = CoordinateConversion.convertToWebMercator(fileExtent.getBottomRight());
            let fileExtent2d = new Box2d(fileExtent2dUpperLeft.x, fileExtent2dUpperLeft.y, fileExtent2dLowerRight.x, fileExtent2dLowerRight.y);

            // Add the map to the list of map views and set the origin to be the center of the map
            // This will need to change in the future to not set the center like this
            this.mapViews.push(new SubMapPosition(subMapView, fileExtent2d));
        }
        
        // Create a data view for proof of concept
        let dataView = new MapDataView(this.dataProvider);
        dataView.initialize();
        this.mapViews.push(new SubMapPosition(dataView, new Box2d(0, 0, PointWebMercator.MAX_X_MERCATOR, PointWebMercator.MAX_Y_MERCATOR)));
    }

    private retrieveConfiguration(mapConfigurationFile: string): void {
        let thisObj = this;
        $.getJSON(mapConfigurationFile,
            function(data: Record<string, SubMapModel>) {
                thisObj.initializeMapModel(data);
                thisObj.render();
            });
    }

    public render(): void {
        if (this.context == null)
            return;

        let viewport2d = this.calculateViewport();
        let viewportMercatorWidth = viewport2d.getLowerRight().x - viewport2d.getUpperLeft().x;
        let viewportMercatorHeight = viewport2d.getLowerRight().y - viewport2d.getUpperLeft().y;

        let context = this.context;
        context.save();
        context.fillStyle = "rgb(50,50,50)";
        context.fillRect(0, 0, this.containerWidth, this.containerHeight);

        this.tileProvider.clearQueue();
        this.mapViews.forEach((submap) => {
            if ( context == null )
                return;
            context.save();

            // Calculate the viewport from the perspective of the un modified sub map
            let mapPosition2d: Box2d = submap.getPosition();
            let viewportOverlap2d = mapPosition2d.union(viewport2d);
            if (viewportOverlap2d != null) {
                let originalWidth = submap.getSubMapView().getOriginalWidth();
                let originalHeight = submap.getSubMapView().getOriginalHeight();

                let mapMercatorWidth = mapPosition2d.getLowerRight().x - mapPosition2d.getUpperLeft().x;
                let mapMercatorHeight = mapPosition2d.getLowerRight().y - mapPosition2d.getUpperLeft().y;
                let mapScale = (this.containerWidth * mapMercatorWidth) / (originalWidth * viewportMercatorWidth);

                let mapShiftX = (mapPosition2d.getUpperLeft().x - this.origin2d.x) * this.containerWidth / viewportMercatorWidth
                let mapShiftY = (mapPosition2d.getUpperLeft().y - this.origin2d.y) * this.containerHeight / viewportMercatorHeight
 
                context.translate(this.containerWidth / 2, this.containerHeight / 2);
                context.translate(mapShiftX, mapShiftY);

                // Figure out the part of the map we want to draw in 2d coordinates relative to the upper left corner
                let subMapDrawSection = new Box2d(
                    originalWidth * (viewportOverlap2d.getUpperLeft().x - mapPosition2d.getUpperLeft().x) / mapMercatorWidth,
                    originalHeight * (viewportOverlap2d.getUpperLeft().y - mapPosition2d.getUpperLeft().y) / mapMercatorHeight,
                    originalWidth * (viewportOverlap2d.getLowerRight().x - mapPosition2d.getUpperLeft().x) / mapMercatorWidth,
                    originalHeight * (viewportOverlap2d.getLowerRight().y - mapPosition2d.getUpperLeft().y) / mapMercatorHeight);

                // Draw the submap
                submap.getSubMapView().render(context, subMapDrawSection, mapScale);
            }
            context.restore();
        })

        // Draw test X
        if (this.mark != undefined) {
            let testPoints = [this.mark];
            testPoints.forEach((testXPositionMercator) => {
                context.save();
                
                let drawX = (testXPositionMercator.x - this.origin2d.x) * this.containerWidth / viewportMercatorWidth;
                let drawY = (testXPositionMercator.y - this.origin2d.y) * this.containerHeight / viewportMercatorHeight;

                context.translate(this.containerWidth / 2, this.containerHeight / 2);
                context.strokeStyle = "rgb(0,0,0)";
                context.beginPath();
                context.moveTo(drawX - 10, drawY - 10);
                context.lineTo(drawX + 10, drawY + 10);
                context.stroke();
                context.beginPath();
                context.moveTo(drawX + 10, drawY - 10);
                context.lineTo(drawX - 10, drawY + 10);
                context.stroke();
                context.restore();
            });
        }

        context.restore();
    }

    /**
     * Returns the viewport in unscaled coordinates.
     */
    private calculateViewport(): Box2d {
        let viewportDimentions2d = this.getViewportDimensions2d();
        let widthBy2 = viewportDimentions2d.x / 2;
        let heightBy2 = viewportDimentions2d.y / 2;
        let viewport = new Box2d(this.origin2d.x - widthBy2, this.origin2d.y - heightBy2,
            this.origin2d.x + widthBy2, this.origin2d.y + heightBy2);
        return viewport;
    }

    private getViewportDimensions2d(): Point2d {
        let viewportWidth = this.containerWidth * this.scale;
        let viewportHeight = this.containerHeight * this.scale;
        return new Point2d(viewportWidth, viewportHeight);
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
            this.scaleDriver += .25;
            if ( this.scaleDriver > this.maxScaleDriver ) {
                this.scaleDriver = this.maxScaleDriver;
            }
        } else {
            this.scaleDriver -= .25;
            if (this.scaleDriver < 0) {
                this.scaleDriver = 0;
            }
        }
        this.updateScale();
        this.render();
    }

    private mouseDown(event: JQuery.Event): void {
        if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
            return;

        if (this.mouseDownHelper(event.offsetX, event.offsetY)) {
            event.stopPropagation();
            event.preventDefault();
        }
    }

    private mouseDownHelper(offsetX: number, offsetY: number): boolean {
        // Detect if the mouse event is outside the canvas
        if (offsetX < 0 || this.containerWidth < offsetX ||
            offsetY < 0 || this.containerHeight < offsetY) {
            return false;
        }

        // Continue with starting the drag state
        this.mouseDownClient = new Point2d(offsetX, offsetY);
        this.mouseDownOrigin2d = this.origin2d.clone();
        this.isDragging = true;

        return true;
    }

    private mouseMove(event: JQuery.Event): void {
        if ( event === undefined || event.offsetX === undefined || event.offsetY === undefined )
            return;

        this.mouseMoveHelper(event.offsetX, event.offsetY);
    }
    
    private mouseMoveHelper(offsetX: number, offsetY: number): void {
        if (this.isDragging) {
            let xDifference = offsetX - this.mouseDownClient.x;
            let yDifference = offsetY - this.mouseDownClient.y;
            let viewportDimentions = this.getViewportDimensions2d();

            let percentageClientTraverseX = xDifference / this.containerWidth;
            let percentageClientTraverseY = yDifference / this.containerHeight;

            this.origin2d = new PointWebMercator(this.mouseDownOrigin2d.x - viewportDimentions.x * percentageClientTraverseX,
                this.mouseDownOrigin2d.y - viewportDimentions.y * percentageClientTraverseY);

            this.render();
        }
    }

    private mouseUp(event: JQuery.Event): void {
        this.mouseUpHelper();
    }

    private mouseUpHelper() {
        this.isDragging = false;
    }

    /* Touch handler events */
    private touchStart(event: JQuery.Event) : void {
        if ( event === undefined || event.targetTouches === undefined ) {
            return;
        }

        if ( event.targetTouches.length == 1 ) {
            let touch = event.targetTouches[0];
            if ( touch === undefined || touch.clientX === undefined || touch.clientY === undefined || touch.identifier === undefined ) {
                return;
            }
            this.touchMoveIdentifier = touch.identifier;
            if ( this.mouseDownHelper(touch.clientX, touch.clientY) ) {
                event.preventDefault();
                event.stopPropagation();
            }
        } else if ( event.targetTouches.length == 2 ) {
            this.isDragging = false;
            this.pinchClientPoint1 = new Point2d(event.targetTouches[0].clientX, event.targetTouches[0].clientY)
            this.pinchClientPoint2 = new Point2d(event.targetTouches[1].clientX, event.targetTouches[1].clientY)
            this.pinchOriginalScale = this.scale
        }
    }

    private touchMove(event: JQuery.Event) : void {
        if ( event === undefined || event.targetTouches === undefined || event.targetTouches.length === undefined ) {
            return;
        }

        if ( event.targetTouches.length == 1 ) {
            let touch: Touch | undefined = undefined;
            for ( let i = 0; i < event.targetTouches.length; i++ ) {
                let thisTouch = event.targetTouches[i];
                if ( thisTouch.identifier == this.touchMoveIdentifier ) {
                    touch = thisTouch;
                    break;
                }
            }

            if ( touch !== undefined ) {
                this.mouseMoveHelper(touch.clientX, touch.clientY);
            }
        } else if ( event.targetTouches.length == 2 ) {
            let touch1 = event.targetTouches[0]
            let touch2 = event.targetTouches[1]
            let newPoint1 = new Point2d(touch1.clientX, touch1.clientY);
            let newPoint2 = new Point2d(touch2.clientX, touch2.clientY);

            let originalDistance = this.pinchClientPoint1.calculateDistance(this.pinchClientPoint2);
            let thisDistance = newPoint1.calculateDistance(newPoint2);

            let targetScale = this.pinchOriginalScale / (thisDistance / originalDistance)
            this.scaleDriver = Math.log(1 / targetScale) / Math.log(2)

            if ( this.scaleDriver > this.maxScaleDriver ) {
                this.scaleDriver = this.maxScaleDriver
            } else if ( this.scaleDriver < 0 ) {
                this.scaleDriver = 0;
            }

            this.updateScale()
            this.render();
        }
    }

    private touchEnd(event: JQuery.Event): void {
        if ( event === undefined || event.targetTouches === undefined || event.targetTouches.length === undefined ) {
            this.mouseUpHelper();
            return;
        }
        
        if ( event.targetTouches.length == 0 ) {
            this.mouseUpHelper();
        } else {
            this.touchStart(event);
        }
    }
};

class SubMapPosition {
    private subMapView: ISubMapView;
    private location: Box2d;

    constructor(subMap: ISubMapView, location: Box2d) {
        this.subMapView = subMap;
        this.location = location;
    }

    public setPosition(location: Box2d): void {
        this.location = location;
    }

    public getPosition(): Box2d {
        return this.location;
    }

    public getSubMapView(): ISubMapView {
        return this.subMapView;
    }
}