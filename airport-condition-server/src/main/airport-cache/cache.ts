
import { AirportInformation } from './airport-information'

export abstract class Cache {
    private fileCache: Record<string, [Date,Array<AirportInformation>]>;
    public static overrideCache: Cache | undefined;
    private static readonly MAX_CACHE_AGE_SECONDS = 60;

    constructor() {
        this.fileCache = {};
    }

    public abstract retrieveFile(name: string, callback: (data: Array<AirportInformation>) => void): void

    protected save(key: string, contents: Array<AirportInformation>): void {
        let tuple: [Date,Array<AirportInformation>] = [new Date(), contents];
        this.fileCache[key] = tuple;
    }

    protected retrieve(key: string): Array<AirportInformation> | undefined {
        if (key in this.fileCache) {
            let tuple = this.fileCache[key];
            let now = new Date();
            if ( (now.getTime() - tuple[0].getTime()) / 1000 > Cache.MAX_CACHE_AGE_SECONDS ) {
                return undefined;
            }
            return tuple[1];
        }
        return undefined;
    }
}
