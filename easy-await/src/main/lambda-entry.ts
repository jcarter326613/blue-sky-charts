
import {EasyAwait} from './easy-await'

export class LambdaEntry {
    constructor(){
    }

    public async handleGetRequest(event: any, hasParameters: boolean,
        executionCallback: ((parameters: Record<string, any>, body: (string | undefined)) => void),
        successResultsCallback: (() => string)): Promise<any> {
        return this.handleRequest(event, "GET", hasParameters, false, executionCallback, successResultsCallback);
    }

    public async handlePostRequest(event: any, hasParameters: boolean,
        executionCallback: ((parameters: Record<string, any>, body: (string | undefined)) => void),
        successResultsCallback: (() => string)): Promise<any> {
        return this.handleRequest(event, "POST", hasParameters, true, executionCallback, successResultsCallback);
    }

    private async handleRequest(event: any, httpMethod: string, hasParameters: boolean, hasBody: boolean,
        executionCallback: ((parameters: Record<string, any>, body: (string | undefined)) => void),
        successResultsCallback: (() => string)): Promise<any> {
        console.log(`Received request: ${JSON.stringify(event)}`);
    
        // Check CORS
        if (!("httpMethod" in event && "headers" in event)) {
            console.warn("Failed CORS.  HttpMethod or Headers missing");
            return {
                "statusCode": 400,
                "body": JSON.stringify({"message": "Invalid request"})
            }
        } else if (!(event["httpMethod"] == httpMethod && "origin" in event["headers"] && (
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
            "Access-Control-Allow-Methods": httpMethod
        };
    
        //Validate request
        if (hasParameters && !("queryStringParameters" in event) || event["queryStringParameters"] === null) {
            return {
                "statusCode": 400,
                "headers": headers,
                "body": JSON.stringify({"message": "Missing collection of query parameters"})
            }
        }
        let body: string | undefined;
        if ( hasBody ) {
            if ( !("body" in event) || event["body"].length == 0 ) {
                return {
                    "statusCode": 400,
                    "headers": headers,
                    "body": JSON.stringify({"message": "Body missing"})
                }
            }
            body = event["body"];
        }
        
        //Fulfill the request
        EasyAwait.instance.initialize();
        EasyAwait.instance.startThread("Handler entrypoint");
    
        if ( hasParameters ) {
            executionCallback(event["queryStringParameters"], body);
        } else {
            executionCallback({}, body);
        }
        
        EasyAwait.instance.endThread("Handler entrypoint");
        await EasyAwait.instance.join();
    
        // Report the results
        if ( EasyAwait.instance.hasFatalError() ) {
            let message = EasyAwait.instance.getFatalMessage();
            if ( message === undefined ) {
                message = "Internal server error";
            }
            let statusCode = 500;
            if ( EasyAwait.instance.hasUserError() ) {
                statusCode = 400;
            }
            return {
                "statusCode": statusCode,
                "headers": headers,
                "body": JSON.stringify({"message": message})
            };
        } else {
            let body = successResultsCallback();
            console.log(body);
            return {
                "statusCode": 200,
                "headers": headers,
                "body": body
            };
        }
    }
}