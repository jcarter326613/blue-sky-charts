import * as AWS from 'aws-sdk';
import { AirportInformation } from './airport-information';
import { Cache } from './cache'
import { join } from 'path'
import { EasyAwait } from 'easy-await';

export class S3Cache extends Cache {
    private static readonly BUCKET_NAME = "blueskycharts.com";
    private root: string;
    private s3: AWS.S3

    constructor(root: string) {
        super();

        this.root = root;
        AWS.config.region = "us-east-1";
        this.s3 = new AWS.S3({apiVersion: '2006-03-01'});
    }

    public retrieveFile(name: string, callback: (data: Array<AirportInformation>) => void): void {
        // If the file is in the cache, returns it
        let cachedValue = this.retrieve(name);
        if ( cachedValue !== undefined ) {
            callback(cachedValue);
            return;
        }

        // Otherwise read the file in s3 and return it
        let fullPath = join(this.root, name);
        let params = {
            Bucket: S3Cache.BUCKET_NAME,
            Key: fullPath,
        };
        
        EasyAwait.instance.startThread("S3Cache.retrieveFile");
        this.s3.getObject(params, (err, data) => {
            if (err) {
                EasyAwait.instance.reportFatalError(`Error retrieving file ${fullPath} from S3.  Error: ${err}`);
            } else if (data.Body === undefined) {
                EasyAwait.instance.reportFatalError(`Error retrieving file ${fullPath} from S3.  Body missing`);
            } else {
                try {
                    let obj = JSON.parse(data.Body.toString());
                    this.save(name, obj as Array<AirportInformation>);
                    callback(obj as Array<AirportInformation>);
                } catch (e) {
                    EasyAwait.instance.reportFatalError(`Error parsing file ${fullPath}`);
                }
            }
            EasyAwait.instance.endThread("S3Cache.retrieveFile");
        });
    }
}