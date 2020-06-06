export abstract class CachedProvider {
    private static readonly MAX_ACTIVE_REQUESTS = 2;
    private cache: Record<string, CachedProviderRequest>; //Need to add ageoff, causing memory leak
    private numActiveRequests: number;
    private requestQueue: Record<string, CachedProviderRequest>;
    private requestDelayMilliseconds: number;
    private lastQueueAddition: Date;
    private processQueuePending: boolean;

    constructor(requestDelayMilliseconds: number = 0) {
        this.cache = {};
        this.numActiveRequests = 0;
        this.requestQueue = {};
        this.requestDelayMilliseconds = requestDelayMilliseconds;
        this.lastQueueAddition = new Date();
        this.processQueuePending = false;
    }

    public clearQueue(): void {
        this.requestQueue = {}
    }

    public getExistingRequest(key: string): CachedProviderRequest | undefined {
        return this.cache[key];
    }

    /**
     * Only to be called by CachedProviderRequest class to signify a downoad has completed.
     */
    public completeRequest(): void {
        this.numActiveRequests--;
        this.processQueue();
    }

    protected addRequestToQueue(key: string, request: CachedProviderRequest): void {
        this.requestQueue[key] = request;
        this.lastQueueAddition = new Date();
        this.processQueue();
    }

    protected addRequestToCache(key: string, request: CachedProviderRequest): void {
        this.cache[key] = request;
    }

    private processQueue(): void {
        if ( this.processQueuePending ) {
            return;
        }
        let now = new Date();
        let timeToWait = this.requestDelayMilliseconds - (now.getTime() - this.lastQueueAddition.getTime())
        if ( timeToWait <= 0 ) {
            while ( this.numActiveRequests < CachedProvider.MAX_ACTIVE_REQUESTS && Object.keys(this.requestQueue).length > 0 ) {
                for ( let i in this.requestQueue ) {
                    let request = this.requestQueue[i];
                    this.addRequestToCache(i, request);
                    request.sendRequest();
                    delete this.requestQueue[i]
                    this.numActiveRequests++;
                    break;
                }
            }
        } else {
            this.processQueuePending = true;
            setTimeout(() => {
                this.processQueuePending = false;
                this.processQueue();
            }, timeToWait);
        }
    }
};

export abstract class CachedProviderRequest {
    private loaded: boolean;
    private inError: boolean;
    private provider: CachedProvider

    constructor(provider: CachedProvider) {
        this.loaded = false;
        this.inError = false;
        this.provider = provider;
    }

    abstract sendRequest(): void;

    abstract broadcastData(immediate: boolean): void;

    public isLoaded(): boolean {
        return this.loaded && !this.inError;
    }

    public isInError(): boolean {
        return this.inError;
    }

    protected setLoaded(): void {
        this.loaded = true;
        this.inError = false;
    }

    protected setError(): void {
        this.inError = true;
        this.loaded = false;
    }

    protected completeRequest(isSuccess: boolean): void {
        this.provider.completeRequest();
        if ( isSuccess ) {
            this.setLoaded();
            this.broadcastData(false);
        } else {
            this.setError();
        }
    }
}