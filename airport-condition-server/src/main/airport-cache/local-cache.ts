
import { AirportInformation } from './airport-information';
import { Cache } from './cache'
import { readFile } from 'fs'
import { join } from 'path'
import { EasyAwait } from 'main/easy-await';

export class LocalCache extends Cache {
    private root: string;

    constructor(root: string) {
        super();

        this.root = root;
    }

    public retrieveFile(name: string, callback: (data: Array<AirportInformation>) => void): void {
        // If the file is in the cache, returns it
        if ( this.hasKey(name) ) {
            callback(this.retrieve(name));
        }

        // Otherwise read the file in from disk and return it
        let fullPath = join(this.root, name);
        readFile(fullPath, null, (err: NodeJS.ErrnoException | null, data: Buffer) => {
            if ( err != null ) {
                EasyAwait.instance.reportFatalError(`Could not load aiport information from file ${fullPath}.  Error: ${err.message}.`);
            } else {
                try {
                    let obj = JSON.parse(data.toString());
                    callback(obj as Array<AirportInformation>);
                } catch (e) {
                    EasyAwait.instance.reportFatalError(`Error parsing file ${fullPath}`);
                }
            }
        });
    }
}