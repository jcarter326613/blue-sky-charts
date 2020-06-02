import * as AWS from 'aws-sdk';
import { DatabaseWriter } from './database-writer'
import { EasyAwait } from '../easy-await'

export class S3Writer extends DatabaseWriter {
    private static readonly s3BucketName: string = "blueskycharts.com";
    
    constructor() {
        super();
        AWS.config.region = "us-east-1";
    }

    public writeFiles(dataset: string, fileName: string, data: string): void {
        // Create S3 service object
        let s3 = new AWS.S3({apiVersion: '2006-03-01'});

        // Write the object to the bucket
        let key = `${dataset}/${fileName}`;
        let params = {
            Bucket: S3Writer.s3BucketName,
            Key: key,
            Body: data,
            StorageClass: "STANDARD"
        };
        
        EasyAwait.instance.startThread();
        s3.putObject(params, function(err, data) {
            if (err) {
                console.error(`Error uploading file ${key} to S3.  Error: ${err}`);
            }
            EasyAwait.instance.endThread();
        });
    }
}