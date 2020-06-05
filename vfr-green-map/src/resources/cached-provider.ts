export abstract class CachedProvider {
    private static readonly MAX_ACTIVE_REQUESTS = 2;
    private cache: Record<string, CachedProviderRequest>; //Need to add ageoff, causing memory leak
    private numActiveRequests: number;
    private requestQueue: Record<string, CachedProviderRequest>;
    private requestDelayMilliseconds: number;

    constructor(requestDelayMilliseconds: number = 0) {
        this.cache = {};
        this.numActiveRequests = 0;
        this.requestQueue = {};
        this.requestDelayMilliseconds = requestDelayMilliseconds;
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
        for ( let i in this.requestQueue ) {
            let request = this.requestQueue[i];
            this.addRequestToCache(i, request);
            request.sendRequest();
            delete this.requestQueue[i]
            return;
        }

        this.numActiveRequests--;
    }

    protected addRequestToQueue(key: string, request: CachedProviderRequest): void {
        if ( this.numActiveRequests < CachedProvider.MAX_ACTIVE_REQUESTS ) {
            this.addRequestToCache(key, request);
            request.sendRequest();
            this.numActiveRequests++;
        } else {
            this.requestQueue[key] = request;
        }
    }

    protected addRequestToCache(key: string, request: CachedProviderRequest): void {
        this.cache[key] = request;
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

    abstract broadcastData(): void;

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
            this.broadcastData();
        } else {
            this.setError();
        }
    }
}