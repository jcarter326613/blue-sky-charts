package com.blueskycharts.app.map.resources

import com.blueskycharts.app.map.utility.PriorityArray
import java.util.*
import kotlin.concurrent.timerTask

abstract class CachedProvider( private val requestDelayMilliseconds: Int = 0 ) {
    private val maxActiveRequests = 2
    private val cache = Hashtable<String, CachedProviderRequest>(); //Need to add ageoff, causing memory leak
    private var numActiveRequests = 0;
    private val requestQueue: PriorityArray<Pair<String, CachedProviderRequest>> = PriorityArray();
    private var requestQueueKeys = Hashtable<String, Int>();    //key, priority
    private var lastQueueAddition = Date();
    private var processQueuePending = false;
    private val processQueueTimer = Timer(false);

    fun isLoading(): Boolean {
        return this.numActiveRequests > 0 || this.requestQueue.size() > 0;
    }

    fun clearQueue() {
        this.processQueueTimer.cancel();
        this.requestQueue.clear();
        this.requestQueueKeys = Hashtable();
    }

    fun getExistingRequest(key: String): CachedProviderRequest? {
        if ( key in this.cache.keys ) {
            return this.cache[key];
        }
        if ( key in this.requestQueueKeys.keys ) {
            val priority = this.requestQueueKeys[key] ?: return null;
            val priorityList = this.requestQueue.getPriorityList(priority);
            for ( e in priorityList ) {
                if ( e.first == key ) {
                    return e.second;
                }
            }
        }
        return null;
    }

    /**
     * Only to be called by CachedProviderRequest class to signify a downoad has completed.
     */
    fun completeRequest() {
        this.numActiveRequests--;
        this.processQueue();
    }

    protected fun addRequestToQueue(key: String, request: CachedProviderRequest) {
        if (key !in this.requestQueueKeys) {
            this.requestQueueKeys.put(key, request.priority);
            this.requestQueue.add(Pair(key, request), request.priority);
            this.lastQueueAddition = Date();
            this.processQueue();
        }
    }

    protected fun addRequestToCache(key: String, request: CachedProviderRequest) {
        this.cache.put(key, request);
    }

    private fun processQueue() {
        if ( this.processQueuePending ) {
            return;
        }
        val now = Date();
        val timeToWait = this.requestDelayMilliseconds - (now.time - this.lastQueueAddition.time)
        if ( timeToWait <= 0 ) {
            while ( this.numActiveRequests < maxActiveRequests && this.requestQueue.size() > 0 ) {
                val request = this.requestQueue.pop() ?: break;
                this.numActiveRequests++;
                this.requestQueueKeys.remove(request.first);
                this.addRequestToCache(request.first, request.second);
                request.second.sendRequest();
            }
        } else {
            this.processQueuePending = true;
            val task: TimerTask = timerTask {
                processQueuePending = false;
                processQueue();
            }
            processQueueTimer.schedule(task, timeToWait)
        }
    }
};
