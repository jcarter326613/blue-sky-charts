import { AddsFileLoader } from './adds-file-loader'
import { LayerConfiguration } from './layer-configuration'

export class MetarFileLoader extends AddsFileLoader {
    private static readonly url: string = "https://www.aviationweather.gov/adds/dataserver_current/current/metars.cache.xml.gz";
    private layerConfiguration: LayerConfiguration;

    constructor() {
        super(MetarFileLoader.url);
        this.layerConfiguration = new LayerConfiguration("metar")
    }

    public generateFiles(): void {
        this.retrieve((jsonObject: any) => {
            // For each zoom level
    
            // Write out the data to s3
        });
    }
}
