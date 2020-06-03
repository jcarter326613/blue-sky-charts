import { Cache } from './airport-cache/cache'
import { S3Cache } from './airport-cache/s3-cache'
import { BoxGeo, PointGeo, Point2d } from 'coordinates'
import { EasyAwait } from './easy-await'
import { ConditionCompiler } from './condition-compiler';
import { LocalCache } from './airport-cache/local-cache';

let s3BucketName: string = "blueskycharts.com";

export const handler = async (event: any = {}): Promise<any> => {
    console.log(`Received request: ${JSON.stringify(event)}`);

    // Check CORS
    if (!("httpMethod" in event && "headers" in event)) {
        console.warn("Failed CORS.  HttpMethod or Headers missing");
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Invalid request"})
        }
    } else if (!(event["httpMethod"] == "GET" && "origin" in event["headers"] && (
        event["headers"]["origin"] == "http://localhost:3000" ||
        event["headers"]["origin"] == "http://localhost:3000/" ||
        event["headers"]["origin"] == "http://blueskycharts.com" ||
        event["headers"]["origin"] == "http://blueskycharts.com/" ||
        event["headers"]["origin"] == "https://blueskycharts.com" ||
        event["headers"]["origin"] == "https://blueskycharts.com/"))) {
        console.warn(`Failed CORS. ${JSON.stringify(event)}`);
        return {
            "statusCode": 400,
            "body": JSON.stringify({"message": "Invalid request"})
        }
    }

    let headers = {
        "Access-Control-Allow-Headers" : "application/json",
        "Access-Control-Allow-Origin": event["headers"]["origin"],
        "Access-Control-Allow-Methods": "GET"
    };

    //Validate request
    if (!("queryStringParameters" in event) || event["queryStringParameters"] === null) {
        return {
            "statusCode": 400,
            "headers": headers,
            "body": JSON.stringify({"message": "Missing collection of query parameters"})
        }
    }
    let queryStringParameters = event["queryStringParameters"];
    if (!("startLongitude" in queryStringParameters) || 
        queryStringParameters["startLongitude"] < -180 || queryStringParameters["startLongitude"] > 180) {
        return {
            "statusCode": 400,
            "headers": headers,
            "body": JSON.stringify({"message": "Missing or invalid field startLongitude"})
        }
    } else if (!("endLongitude" in queryStringParameters) || 
        queryStringParameters["endLongitude"] < -180 || queryStringParameters["endLongitude"] > 180) {
        return {
            "statusCode": 400,
            "headers": headers,
            "body": JSON.stringify({"message": "Missing or invalid field endLongitude"})
        }
    } else if (!("startLatitude" in queryStringParameters) || 
        queryStringParameters["startLatitude"] < -90 || queryStringParameters["startLatitude"] > 90) {
        return {
            "statusCode": 400,
            "headers": headers,
            "body": JSON.stringify({"message": "Missing or invalid field startLatitude"})
        }
    } else if (!("endLatitude" in queryStringParameters) || 
        queryStringParameters["endLatitude"] < -90 || queryStringParameters["endLatitude"] > 90) {
        return {
            "statusCode": 400,
            "headers": headers,
            "body": JSON.stringify({"message": "Missing or invalid field endLatitude"})
        }
    } else if (!("bufferLongitude" in queryStringParameters) || queryStringParameters["bufferLongitude"] < 0) {
        return {
            "statusCode": 400,
            "headers": headers,
            "body": JSON.stringify({"message": "Missing or invalid field bufferLongitude"})
        }
    } else if (!("bufferLatitude" in queryStringParameters) || queryStringParameters["bufferLatitude"] < 0) {
        return {
            "statusCode": 400,
            "headers": headers,
            "body": JSON.stringify({"message": "Missing or invalid field bufferLatitude"})
        }
    } else if (!("information" in queryStringParameters) || !isValidInformationRequestCategory(queryStringParameters["information"])) {
        return {
            "statusCode": 400,
            "headers": headers,
            "body": JSON.stringify({"message": "Missing or invalid field information"})
        }
    }

    //Pull out the arguments
    let topLeft = new PointGeo(queryStringParameters["startLongitude"], queryStringParameters["endLatitude"]);
    let bottomRight = new PointGeo(queryStringParameters["endLongitude"], queryStringParameters["startLatitude"]);
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
        Cache.overrideCache = cache;
    }
    let compiler = new ConditionCompiler(cache, queryStringParameters["information"]);
    compiler.compileConditions(region, buffer);
    
    EasyAwait.instance.endThread();
    await EasyAwait.instance.join();

    // Report the results
    if ( EasyAwait.instance.hasFatalError() ) {
        let message = EasyAwait.instance.getFatalMessage();
        if ( message === undefined ) {
            message = "Internal server error";
        }
        return {
            "statusCode": 500,
            "headers": headers,
            "body": JSON.stringify({"message": message})
        };
    } else {
        let conditionList = compiler.getConditions();
        let body = JSON.stringify(conditionList);
        console.log(body);
        return {
            "statusCode": 200,
            "headers": headers,
            "body": body
        };
    }
}

function isValidInformationRequestCategory(category: string): boolean {
    return (category == "ceiling" || category == "visibility" || category == "cloudCover" || 
        category == "wind" || category == "temperatureCelcius" || category == "dewpointCelcius" || 
        category == "flightCategory");
}