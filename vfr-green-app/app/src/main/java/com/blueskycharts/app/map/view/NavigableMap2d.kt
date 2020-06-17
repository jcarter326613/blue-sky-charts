package com.blueskycharts.app.map.view

import com.blueskycharts.app.map.resources.TileProvider

class NavigableMap2d : Map {
    private val tileProvider: TileProvider;
    private val shadowTileProvider: TileProvider;
    //private dataProvider: DataProvider;

    // Map state variables
    //private context: CanvasRenderingContext2D | null;
    private mapViews: Collection<SubMapPosition>;
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

    constructor(elementId: string, mapRoot: string, originLongitude: number | undefined, originLatitude: number | undefined,
    zoom: number | undefined, markLongitude: number | undefined, markLatitude: number | undefined) {

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
        this.shadowTileProvider = new TileProvider(`${mapRoot}/world-shadow`);
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
        this.setSize();
        this.containerDiv.append(this.canvasObjHtml);
        this.context = canvasObj.getContext("2d");

        this.addEventListeners();
        this.render();
        this.retrieveConfiguration(`${mapRoot}/metadata.json`);
    }
}