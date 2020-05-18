import { IncomingMessage } from "http";
import { get } from "https";

export class AddsFileLoader {
    private readonly url: string;

    constructor(url: string) {
        this.url = url;
    }

    public retrieve(successCallback: (data: string) => void, 
        errorCallback: ((statusCode: number | undefined , data: string | undefined) => void) | undefined = undefined): void {
        
        get(this.url, (response: IncomingMessage) => {
            
            if ( response.statusCode !== undefined && response.statusCode >= 200 && response.statusCode < 300 ) {
                let data: string = "";
                response.on("data", (chunk: any) => {
                    data += chunk as string
                });
                response.on("end", () => {
                    successCallback(data);
                });
                response.on("error", () => {
                    console.error(`Error during download from ${this.url}.`);
                    if ( errorCallback !== undefined ) {
                        errorCallback(response.statusCode, undefined);
                    }
                })
            } else {
                let message: string | undefined = undefined;
                if ( response.readable ) {
                    message = response.read();
                }
                console.error(`$error requesting data from '${this.url}'.  Status code ${response.statusCode}.  Message ${message}.`)
                if ( errorCallback !== undefined ) {
                    errorCallback(response.statusCode, message);
                }
            }
        });
    }
};