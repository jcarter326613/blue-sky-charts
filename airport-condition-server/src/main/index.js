"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handler = void 0;
const coordinates_1 = require("coordinates");
const easy_await_1 = require("./easy-await");
const condition_compiler_1 = require("./condition-compiler");
let s3BucketName = "vfr-green-artifacts-245819277863";
exports.handler = async (event = {}) => {
    console.log(event);
    if (!("queryStringParameters" in event)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({ "message": "Missing collection of query parameters" })
        };
    }
    let queryStringParameters = event["queryStringParameters"];
    if (!("startLongitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({ "message": "Missing field startLongitude" })
        };
    }
    else if (!("endLongitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({ "message": "Missing field endLongitude" })
        };
    }
    else if (!("startLatitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({ "message": "Missing field startLatitude" })
        };
    }
    else if (!("endLatitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({ "message": "Missing field endLatitude" })
        };
    }
    else if (!("bufferLongitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({ "message": "Missing field bufferLongitude" })
        };
    }
    else if (!("bufferLatitude" in queryStringParameters)) {
        return {
            "statusCode": 400,
            "body": JSON.stringify({ "message": "Missing field bufferLatitude" })
        };
    }
    let topLeft = new coordinates_1.PointGeo(queryStringParameters["startLongitude"], queryStringParameters["startLatitude"]);
    let bottomRight = new coordinates_1.PointGeo(queryStringParameters["endLongitude"], queryStringParameters["endLatitude"]);
    let region = new coordinates_1.BoxGeo(topLeft, bottomRight);
    let buffer = new coordinates_1.PointGeo(queryStringParameters["bufferLongitude"], queryStringParameters["bufferLatitude"]);
    easy_await_1.EasyAwait.instance.initialize();
    easy_await_1.EasyAwait.instance.startThread();
    let compiler = new condition_compiler_1.ConditionCompiler();
    compiler.compileConditions(region, buffer);
    easy_await_1.EasyAwait.instance.endThread();
    await easy_await_1.EasyAwait.instance.join();
    if (easy_await_1.EasyAwait.instance.hasFatalError()) {
        let message = easy_await_1.EasyAwait.instance.getFatalMessage();
        if (message === undefined) {
            message = "Internal server error";
        }
        return {
            "statusCode": 500,
            "body": JSON.stringify({ "message": message })
        };
    }
    else {
        let conditionList = compiler.getConditions();
        let body = JSON.stringify(conditionList);
        return {
            "statusCode": 200,
            "body": body
        };
    }
};
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiaW5kZXguanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJpbmRleC50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOzs7QUFBQSw2Q0FBcUQ7QUFDckQsNkNBQXNDO0FBQ3RDLDZEQUF5RDtBQUV6RCxJQUFJLFlBQVksR0FBVyxrQ0FBa0MsQ0FBQztBQUVqRCxRQUFBLE9BQU8sR0FBRyxLQUFLLEVBQUUsUUFBYSxFQUFFLEVBQWdCLEVBQUU7SUFDM0QsT0FBTyxDQUFDLEdBQUcsQ0FBQyxLQUFLLENBQUMsQ0FBQztJQUduQixJQUFJLENBQUMsQ0FBQyx1QkFBdUIsSUFBSSxLQUFLLENBQUMsRUFBRTtRQUNyQyxPQUFPO1lBQ0gsWUFBWSxFQUFFLEdBQUc7WUFDakIsTUFBTSxFQUFFLElBQUksQ0FBQyxTQUFTLENBQUMsRUFBQyxTQUFTLEVBQUUsd0NBQXdDLEVBQUMsQ0FBQztTQUNoRixDQUFBO0tBQ0o7SUFDRCxJQUFJLHFCQUFxQixHQUFHLEtBQUssQ0FBQyx1QkFBdUIsQ0FBQyxDQUFDO0lBQzNELElBQUksQ0FBQyxDQUFDLGdCQUFnQixJQUFJLHFCQUFxQixDQUFDLEVBQUU7UUFDOUMsT0FBTztZQUNILFlBQVksRUFBRSxHQUFHO1lBQ2pCLE1BQU0sRUFBRSxJQUFJLENBQUMsU0FBUyxDQUFDLEVBQUMsU0FBUyxFQUFFLDhCQUE4QixFQUFDLENBQUM7U0FDdEUsQ0FBQTtLQUNKO1NBQU0sSUFBSSxDQUFDLENBQUMsY0FBYyxJQUFJLHFCQUFxQixDQUFDLEVBQUU7UUFDbkQsT0FBTztZQUNILFlBQVksRUFBRSxHQUFHO1lBQ2pCLE1BQU0sRUFBRSxJQUFJLENBQUMsU0FBUyxDQUFDLEVBQUMsU0FBUyxFQUFFLDRCQUE0QixFQUFDLENBQUM7U0FDcEUsQ0FBQTtLQUNKO1NBQU0sSUFBSSxDQUFDLENBQUMsZUFBZSxJQUFJLHFCQUFxQixDQUFDLEVBQUU7UUFDcEQsT0FBTztZQUNILFlBQVksRUFBRSxHQUFHO1lBQ2pCLE1BQU0sRUFBRSxJQUFJLENB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