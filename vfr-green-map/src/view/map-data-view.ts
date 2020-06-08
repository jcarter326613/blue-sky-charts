/**
 * Here's what we are going to do.  Create a lambda function which can respond to rest requests that returns a json similar to the one
 * being injested here.  This control will over request a region and not request a new region until we leave those bounds.  We will have to bucket
 * that request somehow and cache responses.  Bucketing is to allow cache hits.
 * The parent map will tell us when a mouse is "down."
 */

import { BoxGeoModel } from "../models/box-geo-model"
import { Box2d, BoxGeo, BoxWebMercator, Point2d, PointGeo, PointWebMercator, CoordinateConversion } from "coordinates"
import { ISubMapView } from "./i-sub-map-view"
import { IDataReceiver } from "../resources/i-data-receiver"
import { SubMapModel } from "../models/sub-map-model"
import { DataProvider } from "../resources/data-provider"
import { OverlayTypes } from "./overlay-types"

export class MapDataView implements ISubMapView, IDataReceiver {
    // Metadata
    private tileWidth: number;
    private tileHeight: number;

    // Rendering
    private dataProvider: DataProvider;
    private context: CanvasRenderingContext2D | undefined;
    private contextTransform: DOMMatrix | undefined;
    private contextScale: number | undefined;
    private contextRegion: Box2d | undefined;
    private isDisposed: boolean;
    private overlayType: OverlayTypes;

    constructor(dataProvider: DataProvider, type: OverlayTypes) {
        this.dataProvider = dataProvider;
        this.tileWidth = 0;
        this.tileHeight = 0;
        this.isDisposed = false;
        this.overlayType = type;
    }

    public initialize(): BoxWebMercator | undefined {
        if ( this.overlayType == OverlayTypes.None ) {
            return undefined;
        }

        let tilesAcross = 2 ** 2;
        this.tileWidth = PointWebMercator.MAX_X_MERCATOR / tilesAcross;
        this.tileHeight = PointWebMercator.MAX_Y_MERCATOR / tilesAcross;

        return new BoxWebMercator(0, 0, PointWebMercator.MAX_X_MERCATOR, PointWebMercator.MAX_Y_MERCATOR);
    }

    public dispose(): void {
        this.isDisposed = true;
    }

    public getOriginalWidth(): number {
        return PointWebMercator.MAX_X_MERCATOR;
    }

    public getOriginalHeight(): number {
        return PointWebMercator.MAX_Y_MERCATOR;
    }

    public receiveData(location: PointWebMercator, data: any): void {
        if ( this.isDisposed || this.context === undefined || this.contextTransform === undefined || this.contextRegion === undefined ) {
            return;
        }

        if ( location.x < this.contextRegion.getUpperLeft().x || location.x > this.contextRegion.getLowerRight().x ||
            location.y < this.contextRegion.getUpperLeft().y || location.y > this.contextRegion.getLowerRight().y ) {
            return;
        }

        let currentTransform = this.context.getTransform();
        this.context.setTransform(this.contextTransform);
        switch ( this.overlayType ) {
            case OverlayTypes.Ceiling: {
                this.renderCeiling(location, data);
                break;
            }
            case OverlayTypes.Category: {
                this.renderCategory(location, data);
                break;
            }
            case OverlayTypes.DewpointC: {
                this.renderDewpoint(location, data);
                break;
            }
            case OverlayTypes.TempC: {
                this.renderTemperature(location, data);
                break;
            }
            case OverlayTypes.Visibility: {
                this.renderVisibility(location, data);
                break;
            }
            case OverlayTypes.Wind: {
                this.renderWind(location, data);
                break;
            }
            default: {
                console.error("Request to render unknown type.");
            }
        }
        this.context.setTransform(currentTransform);
    }

    /**
     * Graphic wind barb key: https://www.weather.gov/hfo/windbarbinfo.  We are not rounding to the nearest 5 here.  We are rounding up.
     */
    private renderWind(location: PointWebMercator, data: any): void {
        if ( data.windSpeed === undefined || data.windDirection === undefined ||
            this.context === undefined || this.contextScale === undefined ) {
            return;
        }

        // Center the coordinates on the location the wind barb should be
        this.context.translate(location.x * this.contextScale, location.y * this.contextScale);

        // If the wind is variable, draw that.
        if ( data.windDirection == "VRB" ) {
            let circleRadius = 50;
            this.context.strokeStyle = "rgb(0,0,0)";
            this.context.fillStyle = "rgb(0,0,0)";
            this.context.beginPath();
            this.context.arc(0, 0, circleRadius, 0, 2 * Math.PI);
            this.context.stroke();
            this.context.fill();
            this.context.strokeStyle = "rgb(255,255,255)";
            this.context.fillStyle = "rgb(255,255,255)";
            this.context.beginPath();
            this.context.arc(0, 0, circleRadius * 2 / 3, 0, 2 * Math.PI);
            this.context.stroke();
            this.context.fill();
            this.context.strokeStyle = "rgb(0,0,0)";
            this.context.fillStyle = "rgb(0,0,0)";
            this.context.beginPath();
            this.context.arc(0, 0, circleRadius * 1 / 3, 0, 2 * Math.PI);
            this.context.stroke();
            this.context.fill();
        } else {
            // Otherwise, get the angle and speed of the wind
            let windAngle = Math.round(parseFloat(data.windDirection));
            let speedToDraw = Math.ceil(parseFloat(data.windSpeed));
            if ( data.windGust !== undefined ) {
                speedToDraw = Math.ceil(parseFloat(data.windGust));
            }

            if ( speedToDraw != 0 ) {
                // Figure out the configuration of wind barbs
                let numShort = 0;
                let numLong = 0;
                let numPenants = 0;
                while ( speedToDraw > 45 ) {
                    speedToDraw -= 50;
                    numPenants++;
                }
                while ( speedToDraw > 5 ) {
                    speedToDraw -= 10;
                    numLong++;
                }
                if ( speedToDraw > 0 ) {
                    numShort = 1;
                }

                // Figure out how tall the wind barb needs to be
                let poleWidth = 4;
                let poleBallRadius = poleWidth;
                let barbWidth = poleWidth;
                let maxBarbLength = 20;
                let minBarbLength = maxBarbLength / 2;
                let penantWidth = maxBarbLength * 2 / 3;
                let barbAngleRadians = Math.acos((penantWidth / 2) / maxBarbLength)
                let penantDepth = Math.sin(barbAngleRadians) * maxBarbLength;
                let minPoleLength = 20;
                let minPoleTail = 6;

                let indicatorBlankSpaceHeight = barbWidth * (numShort + numLong + numPenants - 1)
                let indicatorHeight = barbWidth * (numShort + numLong) + penantWidth * numPenants;
                let poleLength = indicatorBlankSpaceHeight + indicatorHeight + minPoleTail
                if ( poleLength < minPoleLength ) {
                    poleLength = minPoleLength
                }

                // Draw the pole
                this.context.rotate(-Math.PI / 2);
                this.context.rotate(windAngle * 2 * Math.PI / 360);
                this.context.translate(-poleLength / 2, 0);
                this.context.fillRect(0, -poleWidth / 2, poleLength, poleWidth);
                this.context.beginPath();
                this.context.moveTo(0,0);
                this.context.arc(0, 0, poleBallRadius, 0, Math.PI * 2);
                this.context.fill();

                // If there is only one short barb, draw that at center
                if ( numPenants == 0 && numLong == 0 && numShort == 1 ) {
                    this.context.translate(poleLength / 2, 0);
                    this.context.save();
                    this.context.rotate(barbAngleRadians);
                    this.context.fillRect(0, -barbWidth / 2, minBarbLength, barbWidth);
                    this.context.restore();
                } else {
                    this.context.translate(poleLength, 0);

                    // Draw each penant
                    let penantDrawn = false;
                    while ( numPenants > 0 ) {
                        this.context.beginPath()
                        this.context.moveTo(0,0);
                        this.context.lineTo(-penantWidth / 2, penantDepth);
                        this.context.lineTo(-penantWidth, 0);
                        this.context.fill();
                        this.context.translate(-penantWidth, 0);

                        numPenants--;
                        penantDrawn = true;
                    }
                    if ( penantDrawn ) {
                        this.context.translate(-barbWidth, 0);
                    }

                    // Draw each long barb
                    while ( numLong > 0 ) {
                        this.context.save();
                        this.context.rotate(barbAngleRadians);
                        this.context.fillRect(0, 0, maxBarbLength, barbWidth);
                        this.context.restore();
                        this.context.translate(-(barbWidth * 2), 0);
                        
                        numLong--;
                    }

                    // Draw the short barb
                    if ( numShort > 0 ) {
                        this.context.save();
                        this.context.rotate(barbAngleRadians);
                        this.context.fillRect(0, 0, minBarbLength, barbWidth);
                        this.context.restore();
                    }
                }
            } else {
                // Draw no wind circle
                let circleRadius = 50;
                this.context.strokeStyle = "rgb(0,0,0)";
                this.context.fillStyle = "rgb(0,0,0)";
                this.context.beginPath();
                this.context.arc(0, 0, circleRadius, 0, 2 * Math.PI);
                this.context.stroke();
                this.context.fill();
                this.context.strokeStyle = "rgb(255,255,255)";
                this.context.fillStyle = "rgb(255,255,255)";
                this.context.beginPath();
                this.context.arc(0, 0, circleRadius * 2 / 3, 0, 2 * Math.PI);
                this.context.stroke();
                this.context.fill();
            }
        }
    }

    private renderCeiling(location: PointWebMercator, data: any): void {
        if ( data.ceiling === undefined ) {
            return;
        }

        this.renderBoxText(location, (parseInt(data.ceiling) / 100).toString());
    }

    private renderCategory(location: PointWebMercator, data: any): void {
        if ( data.flightCategory === undefined ) {
            return;
        }

        this.renderBoxText(location, data.flightCategory);
    }

    private renderDewpoint(location: PointWebMercator, data: any): void {
        if ( data.dewpointCelcius === undefined ) {
            return;
        }

        this.renderBoxText(location, data.dewpointCelcius.toString());
    }

    private renderTemperature(location: PointWebMercator, data: any): void {
        if ( data.temperatureCelcius === undefined ) {
            return;
        }

        this.renderBoxText(location, data.temperatureCelcius.toString());
    }

    private renderVisibility(location: PointWebMercator, data: any): void {
        if ( data.visibility === undefined ) {
            return;
        }

        this.renderBoxText(location, data.visibility.toString());
    }

    private renderBoxText(location: PointWebMercator, text: string) {
        if ( this.context === undefined || this.contextScale === undefined ) {
            return;
        }
        let lineHeight = this.context.measureText('M').width * 1.2;
        let textDimensions = this.context.measureText(text);
        let textRect = new Box2d(location.x * this.contextScale - textDimensions.width / 2, location.y * this.contextScale - lineHeight / 2,
            location.x * this.contextScale + textDimensions.width / 2, location.y * this.contextScale + lineHeight / 2);
        this.context.strokeStyle = "rgb(0,0,0)";
        this.context.fillStyle = "rgb(255,255,255)";
        this.context.fillRect(textRect.getUpperLeft().x - 3, textRect.getUpperLeft().y - 3, 
            textRect.getDimensions().x + 6, textRect.getDimensions().y + 6);
        this.context.strokeRect(textRect.getUpperLeft().x - 3, textRect.getUpperLeft().y - 3, 
            textRect.getDimensions().x + 6, textRect.getDimensions().y + 6);

        this.context.strokeText(text, textRect.getUpperLeft().x, textRect.getLowerRight().y);
    }

    public render(context: CanvasRenderingContext2D, region: Box2d, scale: number): void {       
        if ( this.isDisposed ) {
            return;
        }
        this.context = context;
        this.contextTransform = context.getTransform();
        this.contextScale = scale;
        this.contextRegion = region;

        let pixelsAcross = region.getDimensions().x * scale;
        let longitudeAcross = 360 * region.getDimensions().x / this.getOriginalWidth();
        let pixelsAcrossBuffer = this.context.measureText('0').width * 5
        let longitudeBuffer = longitudeAcross * pixelsAcrossBuffer / pixelsAcross
        let latitudeBuffer = longitudeBuffer * 0.6
        
        this.dataProvider.retrieveTile(CoordinateConversion.convertBox2dToBoxGeo(region), new PointGeo(longitudeBuffer, latitudeBuffer), 
            this.overlayType, this);
    }

    public moveOffscreen(): void {
        this.context = undefined;
    }
}