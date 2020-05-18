
import {parse} from 'fast-xml-parser'
import { IncomingMessage } from "http";
import { get } from "https";
import { createGunzip } from 'zlib';

export abstract class AddsFileLoader {
    private readonly url: string;

    constructor(url: string) {
        this.url = url;
    }

    public abstract async generateFiles(callback: (dataset: string, zoomLevel: number, fileName: string, data: string) => void): Promise<void>;

    protected async retrieve(
        successCallback: (data: any) => void, 
        errorCallback: ((statusCode: number | undefined , data: string | undefined) => void) | undefined = undefined): Promise<void> {
        
        // Make the http request
        console.log("test3a");
        await new Promise((resolve) => get(this.url, (response: IncomingMessage) => {
            console.log("test3b");
            if ( response.statusCode !== undefined && response.statusCode >= 200 && response.statusCode < 300 ) {
                let data: string = "";

                // Create the unzip pipeline
                let unzipStream = createGunzip();
                response.pipe(unzipStream);

                // Handle the data coming in on the unzip pipeline
                unzipStream.on("data", (chunk: any) => {
                    data += chunk as string
                });
                unzipStream.on("end", () => {
                    var jsonObj = parse(data, {ignoreAttributes: false});
                    if ( jsonObj !== undefined && jsonObj.response !== undefined && jsonObj.response.data !== undefined ) {
                        if ( jsonObj.response.errors !== undefined && jsonObj.response.errors != "" ) {
                            console.error(`Error reported by ADDS server. ${jsonObj.response.errors}`);
                        } else if ( jsonObj.response.warnings !== undefined && jsonObj.response.warnings != "" ) {
                            console.error(`Warnings reported by ADDS server. ${jsonObj.response.warnings}`);
                        } else {
                            successCallback(jsonObj.response.data);
                        }
                    } else {
                        console.error(`Empty file retrieved from ${this.url}`);
                    }
                    resolve();
                });
                unzipStream.on("error", () => {
                    console.error(`Error during download from ${this.url}.`);
                    if ( errorCallback !== undefined ) {
                        errorCallback(response.statusCode, undefined);
                    }
                    resolve();
                })
            } else {
                // The http request failed.  Report the error and leave.
                let message: string | undefined = undefined;
                if ( response.readable ) {
                    message = response.read();
                }
                console.error(`$error requesting data from '${this.url}'.  Status code ${response.statusCode}.  Message ${message}.`)
                if ( errorCallback !== undefined ) {
                    errorCallback(response.statusCode, message);
                }
                resolve();
            }
        }));
    }
};