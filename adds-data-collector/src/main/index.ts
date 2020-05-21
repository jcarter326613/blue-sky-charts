import * as AWS from 'aws-sdk';
import {AddsFileLoader} from './adds-file-loader';
import {EasyAwait} from './easy-await'
import {MetarFileLoader} from './metar-file-loader';

let s3BucketName: string = "vfr-green-artifacts-245819277863";

export const handler = async (event: any = {}): Promise<any> => {
    EasyAwait.instance.initialize();

    // Create the list of file loaders
    let fileLoaders: Array<AddsFileLoader> = new Array<AddsFileLoader>();
    fileLoaders.push(new MetarFileLoader());

    EasyAwait.instance.startThread();

    // Process each dataserver file
    fileLoaders.forEach((loader) => {
        loader.generateFiles(writeFiles);
    });

    EasyAwait.instance.endThread();
    await EasyAwait.instance.join();
}

let writeFiles = (dataset: string, fileName: string, data: string): void => {
    AWS.config.region = "us-east-1";

    // Create S3 service object
    let s3 = new AWS.S3({apiVersion: '2006-03-01'});

    // Write the object to the bucket
    let key = `${dataset}/${fileName}`;
    let params = {
        Bucket: s3BucketName,
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
