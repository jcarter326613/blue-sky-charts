import { Cache } from './airport-cache/cache'
import { S3Cache } from './airport-cache/s3-cache'
import { BoxGeo, PointGeo, Point2d } from 'coordinates'
import { EasyAwait, LambdaEntry } from 'easy-await'
import { ConditionCompiler } from './condition-compiler';

export const handler = async (event: any = {}): Promise<any> => {
    let entry = new LambdaEntry();
    let compiler: ConditionCompiler;
    return entry.handleGetRequest(event, true, (parameters) => {
        if (!("startLongitude" in parameters) || 
            parameters["startLongitude"] < -180 || parameters["startLongitude"] > 180) {
            EasyAwait.instance.reportUserError("Missing or invalid field startLongitude");
            return;
        } else if (!("endLongitude" in parameters) || 
            parameters["endLongitude"] < -180 || parameters["endLongitude"] > 180) {
            EasyAwait.instance.reportUserError("Missing or invalid field endLongitude");
            return;
        } else if (!("startLatitude" in parameters) || 
            parameters["startLatitude"] < -90 || parameters["startLatitude"] > 90) {
            EasyAwait.instance.reportUserError("Missing or invalid field startLatitude");
            return;
        } else if (!("endLatitude" in parameters) || 
            parameters["endLatitude"] < -90 || parameters["endLatitude"] > 90) {
            EasyAwait.instance.reportUserError("Missing or invalid field endLatitude");
            return;
        } else if (!("bufferLongitude" in parameters) || parameters["bufferLongitude"] < 0) {
            EasyAwait.instance.reportUserError("Missing or invalid field bufferLongitude");
            return;
        } else if (!("bufferLatitude" in parameters) || parameters["bufferLatitude"] < 0) {
            EasyAwait.instance.reportUserError("Missing or invalid field bufferLatitude");
            return;
        } else if (!("information" in parameters) || !isValidInformationRequestCategory(parameters["information"])) {
            EasyAwait.instance.reportUserError("Missing or invalid field information");
            return;
        }

        //Pull out the arguments
        let topLeft = new PointGeo(parameters["startLongitude"], parameters["endLatitude"]);
        let bottomRight = new PointGeo(parameters["endLongitude"], parameters["startLatitude"]);
        let region = new BoxGeo(topLeft, bottomRight);
        let buffer = new PointGeo(parameters["bufferLongitude"], parameters["bufferLatitude"]);

        //Fulfill the request
        let cache: Cache;
        if ( Cache.overrideCache !== undefined ) {
            cache = Cache.overrideCache;
        } else {
            cache = new S3Cache("metar")
            Cache.overrideCache = cache;
        }
        compiler = new ConditionCompiler(cache, parameters["information"]);
        compiler.compileConditions(region, buffer);
    }, () => {
        let conditionList = compiler.getConditions();
        return JSON.stringify(conditionList);
    });
}

function isValidInformationRequestCategory(category: string): boolean {
    return (category == "ceiling" || category == "visibility" || category == "cloudCover" || 
        category == "wind" || category == "temperatureCelcius" || category == "dewpointSpreadCelcius" || 
        category == "flightCategory");
}