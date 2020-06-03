
import * as $ from 'jquery'
import { JQueryMousewheelEventObject } from '../types/jquery-mousewheel'
import { Box2d, BoxWebMercator, PointGeo, PointWebMercator } from 'coordinates'
import { CoordinateConversion } from 'coordinates'
import { IMap } from './i-map'
import { OverlayTypes } from './overlay-types'
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
    private dataOverlayView: SubMapPosition | undefined;
    private origin2d: PointWebMercator;
    private mark: PointWebMercator | undefined;
    private scale: number;
    private scaleDriver: number;
    private readonly maxScaleDriver: number;

    // Mouse event variables
    private isDragging: boolean;
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
            this.scaleDriver = 4.25;
        } else {
            this.scaleDriver = zoom;
        }
        if ( originLongitude !== undefined && originLatitude !== undefined ) {
            this.origin2d = CoordinateConversion.convertToWebMercator(new PointGeo(originLongitude, originLatitude));
        } else {
            this.origin2d = CoordinateConversion.convertToWebMercator(new PointGeo(-98.5795, 39.8283));
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

        this.mouseDownClient = new Point2d();
        this.mouseDownOrigin2d = new Point2d();
        this.touchMoveIdentifier = 0;

        this.mapViews = new Array<SubMapPosition>();
        this.isDragging = false;
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

    public setOverlayType(type: OverlayTypes): boolean {
        let success: boolean
        if (type != OverlayTypes.None) {
            let dataView = new MapDataView(this.dataProvider, type);
            let extent = dataView.initialize();
            if ( extent !== undefined ) {
                this.dataOverlayView = new SubMapPosition(dataView, extent);
                success = true;
            } else {
                success = false;
            }
        } else {
            if ( this.dataOverlayView !== undefined ) {
                this.dataOverlayView.getSubMapView().dispose();
                this.dataOverlayView = undefined;
            }
            success = true;
        }

        this.render();
        return success;
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
            let subMapModel = data[key];
            if (subMapModel.fileExtent == null || subMapModel.imageWidth == null || subMapModel.imageHeight == null)
                continue;

            let subMapView = new MapTileView(this.tileProvider, this);
            let fileExtent = subMapView.initialize(subMapModel, key);
            if (fileExtent === undefined)
                continue;

            this.mapViews.push(new SubMapPosition(subMapView, fileExtent));
        }
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

        let context = this.context;
        context.save();
        context.fillStyle = "rgb(50,50,50)";
        context.fillRect(0, 0, this.containerWidth, this.containerHeight);

        this.tileProvider.clearQueue();
        this.mapViews.forEach((submap) => {
            if ( context == null )
                return;
            context.save();
            this.renderSubMap(submap, viewport2d);
            context.restore();
        })
        if ( this.dataOverlayView !== undefined ) {
            context.save();
            this.renderSubMap(this.dataOverlayView, viewport2d);
            context.restore();
        }

        // Draw test X
        if (this.mark != undefined) {
            let testPoints = [this.mark];
            testPoints.forEach((testXPositionMercator) => {
                context.save();
                
                let drawX = (testXPositionMercator.x - this.origin2d.x) * this.containerWidth / viewport2d.getWidth();
                let drawY = (testXPositionMercator.y - this.origin2d.y) * this.containerHeight / viewport2d.getHeight();

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

    private renderSubMap(submap: SubMapPosition, viewport2d: BoxWebMercator): void {
        if ( this.context == null ) {
            return;
        }
        // Calculate the viewport from the perspective of the un modified sub map
        let mapPosition2d: BoxWebMercator = submap.getPosition();
        let viewportOverlap2d = mapPosition2d.union(viewport2d);
        if (viewportOverlap2d != null) {
            let originalWidth = submap.getSubMapView().getOriginalWidth();
            let originalHeight = submap.getSubMapView().getOriginalHeight();

            let mapMercatorWidth = mapPosition2d.getBottomRight().x - mapPosition2d.getTopLeft().x;
            let mapMercatorHeight = mapPosition2d.getBottomRight().y - mapPosition2d.getTopLeft().y;
            let mapScale = (this.containerWidth * mapMercatorWidth) / (originalWidth * viewport2d.getWidth());

            let mapShiftX = (mapPosition2d.getTopLeft().x - this.origin2d.x) * this.containerWidth / viewport2d.getWidth()
            let mapShiftY = (mapPosition2d.getTopLeft().y - this.origin2d.y) * this.containerHeight / viewport2d.getHeight()

            this.context.translate(this.containerWidth / 2, this.containerHeight / 2);
            this.context.translate(mapShiftX, mapShiftY);

            // Figure out the part of the map we want to draw in 2d coordinates relative to the upper left corner
            let subMapDrawSection = new Box2d(
                originalWidth * (viewportOverlap2d.getTopLeft().x - mapPosition2d.getTopLeft().x) / mapMercatorWidth,
                originalHeight * (viewportOverlap2d.getTopLeft().y - mapPosition2d.getTopLeft().y) / mapMercatorHeight,
                originalWidth * (viewportOverlap2d.getBottomRight().x - mapPosition2d.getTopLeft().x) / mapMercatorWidth,
                originalHeight * (viewportOverlap2d.getBottomRight().y - mapPosition2d.getTopLeft().y) / mapMercatorHeight);

            // Draw the submap
            submap.getSubMapView().render(this.context, subMapDrawSection, mapScale);
        } else {
            submap.getSubMapView().moveOffscreen();
        }
    }

    /**
     * Returns the viewport in unscaled coordinates.
     */
    private calculateViewport(): BoxWebMercator {
        let viewportDimentions2d = this.getViewportDimensions2d();
        let widthBy2 = viewportDimentions2d.x / 2;
        let heightBy2 = viewportDimentions2d.y / 2;
        let viewport = new BoxWebMercator(this.origin2d.x - widthBy2, this.origin2d.y - heightBy2,
            this.origin2d.x + widthBy2, this.origin2d.y + heightBy2);
        return viewport;
    }

    private getViewportDimensions2d(): PointWebMercator {
        let viewportWidth = this.containerWidth * this.scale;
        let viewportHeight = this.containerHeight * this.scale;
        return new PointWebMercator(viewportWidth, viewportHeight);
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
    private location: BoxWebMercator;

    constructor(subMap: ISubMapView, location: BoxWebMercator) {
        this.subMapView = subMap;
        this.location = location;
    }

    public setPosition(location: BoxWebMercator): void {
        this.location = location;
    }

    public getPosition(): BoxWebMercator {
        return this.location;
    }

    public getSubMapView(): ISubMapView {
        return this.subMapView;
    }
}