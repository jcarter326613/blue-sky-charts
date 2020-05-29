
import { AirportInformation } from './airport-information'

export abstract class Cache {
    private fileCache: Record<string, Array<AirportInformation>>;
    public static overrideCache: Cache | undefined;

    constructor() {
        this.fileCache = {};
    }

    public abstract retrieveFile(name: string, callback: (data: Array<AirportInformation>) => void): void

    protected save(key: string, contents: Array<AirportInformation>): void {
        this.fileCache[key] = contents;
    }

    protected hasKey(key: string): boolean {
        return key in this.fileCache;
    }

    protected retrieve(key: string): Array<AirportInformation> {
        return this.fileCache[key];
    }
}