import { AddsFileLoader } from './adds-file-loader'
import { CoordinateConversion, PointGeo, PointWebMercator } from 'coordinates'
import { DatabaseWriter } from './io/database-writer'
import { EasyAwait } from './easy-await'
import { LayerConfiguration } from './layer-configuration'
import { MultiGrid } from './multi-grid'

export class MetarFileLoader extends AddsFileLoader {
    private static readonly url: string = "https://www.aviationweather.gov/adds/dataserver_current/current/metars.cache.xml.gz";
    private layerConfiguration: LayerConfiguration;

    constructor() {
        super(MetarFileLoader.url);
        this.layerConfiguration = new LayerConfiguration("metar");
        this.layerConfiguration.maxZoom = 2;
    }

    public generateFiles(writer: DatabaseWriter): void {
        let skyConditionMultiGrid = new MultiGrid(this.layerConfiguration.maxZoom);

        EasyAwait.instance.startThread();
        this.retrieve((jsonObject: any) => {
            if (jsonObject.METAR === undefined) {
                console.error("METAR file download did not have METAR elements")
                EasyAwait.instance.endThread();
                return;
            }

            // Go through each metar and place it in its correct zoom cells
            jsonObject.METAR.forEach((metar: any) => {
                let geoLocation = this.getGeoLocation(metar);
                if ( geoLocation !== undefined ) {
                    let metarObj = {
                        "latitude": geoLocation.latitude,
                        "longitude": geoLocation.longitude,
                        "ceiling": <number|undefined>undefined,
                        "visibility": <number|undefined>undefined,
                        "cloudCover": <string|undefined>undefined,
                        "windSpeed": <number|undefined>undefined,
                        "windDirection": <number|undefined>undefined,
                        "windGust": <number|undefined>undefined,
                        "temperatureCelcius": <number|undefined>undefined,
                        "dewpointSpreadCelcius": <number|undefined>undefined,
                        "flightCategory": <string|undefined>undefined
                    };

                    // Extract ceiling and sky cover
                    let skyConditionList = this.extractSkyCondition(metar.sky_condition);
                    if ( skyConditionList.length > 0 ) {
                        for ( let condition of skyConditionList ) {
                            if ( condition.skyCover == "OVC" || condition.skyCover == "BKN") {
                                if ( metarObj["ceiling"] === undefined || metarObj["ceiling"] > condition.elevation ) {
                                    metarObj["ceiling"] = condition.elevation
                                }
                            } else if ( condition.skyCover == "OVX" && metar.vert_vis_ft !== undefined ) {
                                metarObj["ceiling"] = metar.vert_vis_ft
                            }

                            if ( this.isValidSkyCoverIndicator(condition.skyCover) && condition.skyCover != "CLR" && (
                                    metarObj["cloudCover"] === undefined || 
                                    this.skyCoverCompare(metarObj["cloudCover"], condition.skyCover) < 0 )) {
                                metarObj["cloudCover"] = condition.skyCover
                            }
                        }
                    }

                    //Extract visibility
                    if ( metar.visibility_statute_mi !== undefined ) {
                        metarObj["visibility"] = Math.floor(metar.visibility_statute_mi);
                    }

                    // Extract wind information
                    metarObj["windDirection"] = metar.wind_dir_degrees;
                    metarObj["windSpeed"] = metar.wind_speed_kt;
                    metarObj["windGust"] = metar.wind_gust_kt;
                    
                    // Extract temp and dewpoint
                    metarObj["temperatureCelcius"] = metar.temp_c;
                    if ( metar.temp_c !== undefined && metar.dewpoint_c !== undefined ) {
                        metarObj["dewpointSpreadCelcius"] = metar.temp_c - metar.dewpoint_c;
                    }

                    // Extract flight category
                    metarObj["flightCategory"] = metar.flight_category;

                    // Add the extracted object to the grid
                    let mercatorLocation = CoordinateConversion.convertToWebMercator(geoLocation);
                    skyConditionMultiGrid.addObject(mercatorLocation, metarObj);
                }
            });

            // Write out the zoom files
            skyConditionMultiGrid.forEach((fileName: string, data: string): void => {
                writer.writeFiles("metar", fileName, data);
            });
            EasyAwait.instance.endThread();
        });
    }

    private isValidSkyCoverIndicator(cover: string): boolean {
        if ( cover == "OVC" || cover == "SCT" || cover == "CLR" || cover == "BKN" || cover == "FEW" || cover == "OVX" ) {
            return true;
        }
        console.error(`Found unknown sky cover indicator: ${cover}`);
        return false;
    }

    private skyCoverCompare(a: string, b: string) {
        return this.getSkyCoverId(a) - this.getSkyCoverId(b);
    }

    private getSkyCoverId(a: string): number {
        if ( a == "CLR" ) {
            return 0
        } else if ( a == "FEW" ) {
            return 1
        } else if ( a == "SCT" ) {
            return 2
        } else if ( a == "BKN" ) {
            return 3
        } else if ( a == "OVC" ) {
            return 4
        } else if ( a == "OVX" ) {
            return 5
        } else {
            console.error(`Found unknown sky cover indicator in comparison: ${a}`);
            return 0;
        }
    }

    private getGeoLocation(metar: any): PointGeo | undefined {
        if (metar.latitude === undefined || metar.longitude === undefined) {
            //console.error(`Location information missing for metar station ${metar.station_id}`);
            return undefined;
        }

        let latitude: number = parseFloat(metar.latitude);
        let longitude: number = parseFloat(metar.longitude);

        if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
            console.error(`Location information corrupted for metar station ${metar.station_id}`);
            return undefined;
        }

        return new PointGeo(longitude, latitude);
    }

    private extractSkyCondition(skyCondition: any): Array<SkyCondition> {
        let skyConditionList: Array<SkyCondition> = new Array<SkyCondition>();
        if ( skyCondition === undefined ) {
            // Clear skies
            return skyConditionList;
        }

        let addConditionFunction = (rawObj: any) => {
            let newSkyCondition: SkyCondition = new SkyCondition(
                rawObj["@_sky_cover"],
                parseInt(rawObj["@_cloud_base_ft_agl"])
            );
            if (!Number.isNaN(newSkyCondition.elevation)) {
                skyConditionList.push(newSkyCondition);
            }
        }

        if (skyCondition.length === undefined) {
            addConditionFunction(skyCondition);
        } else {
            skyCondition.forEach((condition: any) => addConditionFunction(condition));
        }

        skyConditionList.sort((a, b) => a.elevation < b.elevation ? -1 : a.elevation > b.elevation ? 1 : 0);
        return skyConditionList;
    }
}

class SkyCondition {
    public skyCover: string;
    public elevation: number;

    constructor(skyCover: string, elevation: number) {
        this.skyCover = skyCover;
        this.elevation = elevation;
    }
}