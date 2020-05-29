import { Cache } from './airport-cache/cache'
import { S3Cache } from './airport-cache/s3-cache'
import { BoxGeo, PointGeo, Point2d } from 'coordinates'
import { EasyAwait } from './easy-await'
import { ConditionCompiler } from './condition-compiler';
import { LocalCache } from './airport-cache/local-cache';

let s3BucketName: string = "vfr-green-artifacts-245819277863";

export const handler = async (event: any = {}): Promise<any> => {
    //Validate request
    if (!("queryStringParameters" in event)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Missing collection of query parameters"})
        }
    }
    let queryStringParameters = event["queryStringParameters"];
    if (!("startLongitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Missing field startLongitude"})
        }
    } else if (!("endLongitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Missing field endLongitude"})
        }
    } else if (!("startLatitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Missing field startLatitude"})
        }
    } else if (!("endLatitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Missing field endLatitude"})
        }
    } else if (!("bufferLongitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Missing field bufferLongitude"})
        }
    } else if (!("bufferLatitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Missing field bufferLatitude"})
        }
    }

    //Pull out the arguments
    let topLeft = new PointGeo(queryStringParameters["startLongitude"], queryStringParameters["startLatitude"]);
    let bottomRight = new PointGeo(queryStringParameters["endLongitude"], queryStringParameters["endLatitude"]);
    let region = new BoxGeo(topLeft, bottomRight);
    let buffer = new PointGeo(queryStringParameters["bufferLongitude"], queryStringParameters["bufferLatitude"]);

    //Fulfill the request
    EasyAwait.instance.initialize();
    EasyAwait.instance.startThread();

    let cache: Cache;
    if ( Cache.overrideCache !== undefined ) {
        cache = Cache.overrideCache;
    } else {
        cache = new S3Cache("metar")
    }
    let compiler = new ConditionCompiler(cache);
    compiler.compileConditions(region, buffer);
    
    EasyAwait.instance.endThread();
    await EasyAwait.instance.join();

    if ( EasyAwait.instance.hasFatalError() ) {
        let message = EasyAwait.instance.getFatalMessage();
        if ( message === undefined ) {
            message = "Internal server error";
        }
        return {
            "statusCode": 500,
            "body": JSON.stringify({"message": message})
        };
    } else {
        let conditionList = compiler.getConditions();
        let body = JSON.stringify(conditionList);
        return {
            "statusCode": 200,
            "body": body
        };
    }
}
