import { AddsFileLoader } from './adds-file-loader'
import { CoordinateConversion, PointGeo, PointWebMercator } from 'coordinates'
import { LayerConfiguration } from './layer-configuration'
import { MultiGrid } from './multi-grid'

export class MetarFileLoader extends AddsFileLoader {
    private static readonly url: string = "https://www.aviationweather.gov/adds/dataserver_current/current/metars.cache.xml.gz";
    private static readonly MAX_METAR_CELING: number = 900000;
    private layerConfiguration: LayerConfiguration;

    constructor() {
        super(MetarFileLoader.url);
        this.layerConfiguration = new LayerConfiguration("metar");
        this.layerConfiguration.maxZoom = 4;
    }

    public generateFiles(callback: (dataset: string, zoomLevel: number, fileName: string, data: string) => void): void {
        let skyConditionMultiGrid = new MultiGrid(this.layerConfiguration.maxZoom);

        this.retrieve((jsonObject: any) => {
            if (jsonObject.METAR === undefined) {
                console.error("METAR file download did not have METAR elements")
            }

            // Go through each metar and place it in its correct zoom cells
            jsonObject.METAR.forEach((metar: any) => {
                let location = this.getWebMercatorLocation(metar);
                if ( location !== undefined ) {
                    let skyConditionList = this.extractSkyCondition(metar.sky_condition);
                    if ( skyConditionList.length == 0 ) {
                        skyConditionMultiGrid.addObject(location, skyConditionList, -MetarFileLoader.MAX_METAR_CELING);
                    } else {
                        skyConditionMultiGrid.addObject(location, skyConditionList, -skyConditionList[0].elevation);
                    }
                }
            });

            // Write out the zoom files
            skyConditionMultiGrid.forEach((zoomLevel: number, fileName: string, data: string): void => {
                callback("metar/ceiling", zoomLevel, fileName, data);
            });
        });
    }

    private getWebMercatorLocation(metar: any): PointWebMercator | undefined {
        if (metar.latitude === undefined || metar.longitude === undefined) {
            console.error(`Location information missing for metar station ${metar.station_id}`);
            return undefined;
        }

        let latitude: number = parseInt(metar.latitude);
        let longitude: number = parseInt(metar.longitude);

        if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
            console.error(`Location information corrupted for metar station ${metar.station_id}`);
            return undefined;
        }

        let pointGeo = new PointGeo(longitude, latitude);
        return CoordinateConversion.convertToWebMercator(pointGeo);
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