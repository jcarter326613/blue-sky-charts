import * as AWS from 'aws-sdk'

export const handler = async (event: any = {}): Promise<any> => {
    AWS.config.region = "us-east-1";

    // Create S3 service object
    let s3 = new AWS.S3({apiVersion: '2006-03-01'});

    // Create the parameters for calling createBucket
    var bucketParams = {
        Bucket : "test-bucket",
        ACL : 'public-read'
    };
    
    // call S3 to create the bucket
    s3.createBucket(bucketParams, function(err, data) {
        if (err) {
            console.log("Error in here", err);
        } else {
            console.log("Success in here", data.Location);
        }
    });
    
    return "success";
}
