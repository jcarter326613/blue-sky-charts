import { AddsFileLoader } from './adds-file-loader'

export class MetarFileLoader extends AddsFileLoader {
    private static readonly url: string = "https://www.aviationweather.gov/adds/dataserver_current/current/metars.cache.xml";

    constructor() {
        super(MetarFileLoader.url);
    }
}