import { get, IncomingMessage } from "http";

export class AddsFileLoader {
    private readonly url: string;

    constructor(url: string) {
        this.url = url;
    }

    public retrieve(callback: (message: IncomingMessage) => void): void {
        get(this.url, callback);
    }
};