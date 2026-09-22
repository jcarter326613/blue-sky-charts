
import { PriorityArray } from '../utility/priority-array'

export abstract class CachedProvider {
    private static readonly MAX_ACTIVE_REQUESTS = 2;
    private cache: Record<string, CachedProviderRequest>; //Need to add ageoff, causing memory leak
    private numActiveRequests: number;
    private requestQueue: PriorityArray<[string, CachedProviderRequest]>;
    private requestQueueKeys: Record<string,number>;    //key, priority
    private requestDelayMilliseconds: number;
    private lastQueueAddition: Date;
    private processQueuePending: boolean;

    constructor(requestDelayMilliseconds: number = 0) {
        this.cache = {};
        this.numActiveRequests = 0;
        this.requestQueue = new PriorityArray<[string, CachedProviderRequest]>();
        this.requestQueueKeys = {};
        this.requestDelayMilliseconds = requestDelayMilliseconds;
        this.lastQueueAddition = new Date();
        this.processQueuePending = false;
    }

    public isLoading(): boolean {
        return this.numActiveRequests > 0 || this.requestQueue.size() > 0;
    }

    public clearQueue(): void {
        this.requestQueue.clear();
        this.requestQueueKeys = {};
    }

    public getExistingRequest(key: string): CachedProviderRequest | undefined {
        if ( key in this.cache ) {
            return this.cache[key];
        }
        if ( key in this.requestQueueKeys ) {
            let priority = this.requestQueueKeys[key];
            let priorityList = this.requestQueue.getPriorityList(priority);
            for ( let e of priorityList ) {
                if ( e[0] == key ) {
                    return e[1];
                }
            }
        }
    }

    /**
     * Only to be called by CachedProviderRequest class to signify a downoad has completed.
     */
    public completeRequest(): void {
        this.numActiveRequests--;
        this.processQueue();
    }

    protected addRequestToQueue(key: string, request: CachedProviderRequest): void {
        if (!(key in this.requestQueueKeys)) {
            this.requestQueueKeys[key] = request.getPriority();
            this.requestQueue.add([key, request], request.getPriority());
            this.lastQueueAddition = new Date();
            this.processQueue();
        }
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
            while ( this.numActiveRequests < CachedProvider.MAX_ACTIVE_REQUESTS && this.requestQueue.size() > 0 ) {
                let request = this.requestQueue.pop();
                if ( request === undefined ) {
                    break;
                }
                delete this.requestQueueKeys[request[0]];
                this.addRequestToCache(request[0], request[1]);
                request[1].sendRequest();
                this.numActiveRequests++;
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
    private provider: CachedProvider;
    private priority: number;

    constructor(provider: CachedProvider, priority: number) {
        this.loaded = false;
        this.inError = false;
        this.provider = provider;
        this.priority = priority;
    }

    abstract sendRequest(): void;

    abstract broadcastData(immediate: boolean): void;

    public getPriority(): number {
        return this.priority;
    }

    public isLoaded(): boolean {
        return this.loaded && !this.inError;
    }

    public isInError(): boolean {
        return this.inError;
    }

    protected setLoaded(loaded: boolean = true): void {
        this.loaded = loaded
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