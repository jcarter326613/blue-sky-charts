import { AddsFileLoader } from './adds-file-loader'
import { CoordinateConversion, PointGeo, PointWebMercator } from 'coordinates'
import { EasyAwait } from './easy-await'
import { LayerConfiguration } from './layer-configuration'
import { MultiGrid } from './multi-grid'

export class MetarFileLoader extends AddsFileLoader {
    private static readonly url: string = "https://www.aviationweather.gov/adds/dataserver_current/current/metars.cache.xml.gz";
    private static readonly MAX_METAR_CELING: number = 900000;
    private layerConfiguration: LayerConfiguration;

    constructor() {
        super(MetarFileLoader.url);
        this.layerConfiguration = new LayerConfiguration("metar");
        this.layerConfiguration.maxZoom = 2;
    }

    public generateFiles(callback: (dataset: string, fileName: string, data: string) => void): void {
        let skyConditionMultiGrid = new MultiGrid(this.layerConfiguration.maxZoom);

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
                    let ceilingObj = {
                        "latitude": geoLocation.latitude,
                        "longitude": geoLocation.longitude,
                        "ceiling": <number|undefined>undefined
                    };

                    let mercatorLocation = CoordinateConversion.convertToWebMercator(geoLocation);
                    let skyConditionList = this.extractSkyCondition(metar.sky_condition);
                    let elevation: number | undefined = undefined;
                    if ( skyConditionList.length > 0 ) {
                        for ( let condition of skyConditionList ) {
                            if ( condition.skyCover == "OVC" || condition.skyCover == "BKN") {
                                ceilingObj["ceiling"] = condition.elevation
                                elevation = skyConditionList[0].elevation
                                break;
                            }
                        }
                    }

                    if ( elevation === undefined ) {
                        elevation = MetarFileLoader.MAX_METAR_CELING
                    }
                    skyConditionMultiGrid.addObject(mercatorLocation, ceilingObj);
                }
            });

            // Write out the zoom files
            skyConditionMultiGrid.forEach((fileName: string, data: string): void => {
                callback("metar", fileName, data);
            });
        });
    }

    private getGeoLocation(metar: any): PointGeo | undefined {
        if (metar.latitude === undefined || metar.longitude === undefined) {
            console.error(`Location information missing for metar station ${metar.station_id}`);
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