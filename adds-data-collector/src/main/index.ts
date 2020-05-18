import * as AWS from 'aws-sdk';
import {AddsFileLoader} from './adds-file-loader';
import {MetarFileLoader} from './metar-file-loader';

let s3BucketName: string = "vfr-green-artifacts-245819277863";
let maxPerTile: number = 5;

export const handler = async (event: any = {}): Promise<any> => {
    // Create the list of file loaders
    let fileLoaders: Array<AddsFileLoader> = new Array<AddsFileLoader>();
    fileLoaders.push(new MetarFileLoader());

    // For each dataserver file
    for ( let i = 0; i < fileLoaders.length; i++ ) {
        let loader = fileLoaders[i];
        await loader.generateFiles(writeFiles);
    }
}

let writeFiles = (dataset: string, zoomLevel: number, fileName: string, data: string): void => {
    AWS.config.region = "us-east-1";

    // Create S3 service object
    let s3 = new AWS.S3({apiVersion: '2006-03-01'});

    // Write the object to the bucket
    let key = `${dataset}/${zoomLevel}/${fileName}`;
    let params = {
        Bucket: s3BucketName,
        Key: key,
        Body: data,
        StorageClass: "STANDARD"
    };
    s3.putObject(params, function(err, data) {
        if (err) {
            console.error(`Error uploading file ${key} to S3.  Error: ${err}`);
        }
    });
}
