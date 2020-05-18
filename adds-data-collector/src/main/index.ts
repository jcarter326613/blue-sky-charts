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
    fileLoaders.forEach((loader: AddsFileLoader) => {
        // Download the file
        loader.generateFiles();
    });







/*
    AWS.config.region = "us-east-1";

    // Create S3 service object
    let s3 = new AWS.S3({apiVersion: '2006-03-01'});

    // Create the parameters for calling createBucket
    var bucketParams = {
        Bucket: "srgsgkhesgkuhseg-example"
    };
    
    // call S3 to create the bucket
    s3.createBucket(bucketParams, function(err, data) {
        if (err) {
            console.log("Error in here", err);
        } else {
            console.log("Success in here", data.Location);
        }
    });
    */
    return "success";
}
