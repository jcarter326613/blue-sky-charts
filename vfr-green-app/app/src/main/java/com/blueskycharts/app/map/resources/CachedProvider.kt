package com.blueskycharts.app.map.resources

import com.blueskycharts.app.utility.PriorityArray
import com.blueskycharts.app.map.view.Map
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.timerTask

abstract class CachedProvider( private val map: Map, private val requestDelayMilliseconds: Int = 0 ) {
    private val maxActiveRequests = 2
    private val cache = Hashtable<String, CachedProviderRequest>(); //Need to add ageoff, causing memory leak
    private var numActiveRequests = AtomicInteger(0)
    private var numAwaitingQueueAddition = AtomicInteger(0)
    private val requestQueue: PriorityArray<Pair<String, CachedProviderRequest>> = PriorityArray();
    private var requestQueueKeys = Hashtable<String, Int>();    //key, priority
    private var lastQueueAddition = Date();
    private var processQueuePending = false;
    private val processQueueTimer = Timer(false);
    private val queueMutex: Mutex = Mutex()

    fun isLoading(): Boolean {
        return this.numActiveRequests.get() > 0 || this.requestQueue.size() > 0 || numAwaitingQueueAddition.get() > 0
    }

    fun clearQueue() {
        GlobalScope.launch {    //ok1
            queueMutex.withLock {
                this@CachedProvider.requestQueue.clear();
                this@CachedProvider.requestQueueKeys = Hashtable();
            }
        }
    }

    fun getCachedItem(key: String) :CachedProviderRequest? {
        return try {
            this.cache[key]
        } catch ( e: Throwable ) {
            null
        }
    }

    suspend fun getExistingRequest(key: String): CachedProviderRequest? {
        queueMutex.withLock {
            if (key in this.cache.keys) {
                return this.cache[key];
            }
            if (key in this.requestQueueKeys.keys) {
                val priority = this.requestQueueKeys[key] ?: return null;
                val priorityList = this.requestQueue.getPriorityList(priority);
                for (e in priorityList) {
                    if (e.first == key) {
                        return e.second;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Only to be called by CachedProviderRequest class to signify a downoad has completed.
     */
    fun completeRequest() {
        this.numActiveRequests.getAndDecrement();
        this.processQueue();
        this.map.requestRedraw()
    }

    protected fun incrementAwaitingQueueAddition() {
        numAwaitingQueueAddition.getAndIncrement()
    }

    protected fun decrementAwaitingQueueAddition() {
        numAwaitingQueueAddition.getAndDecrement()
    }

    protected suspend fun addDataToCache(key: String, request: CachedProviderRequest) {
        queueMutex.withLock {
            cache[key] = request
        }
    }

    protected fun addRequestToQueue(key: String, request: CachedProviderRequest) {
        //make it so a loaded request adds to the cache if it's receive date is newer than what's there or there's nothing there.
        if (key in this.requestQueueKeys) {
            return
        }
        incrementAwaitingQueueAddition()
        GlobalScope.launch {    //ok1
            queueMutex.withLock {
                try {
                    if (key in this@CachedProvider.requestQueueKeys.keys) {
                        return@launch
                    }
                    try {
                        if (this@CachedProvider.cache.containsKey(key)) {
                            val cacheItem: CachedProviderRequest? = this@CachedProvider.cache[key]
                            if (cacheItem != null && !cacheItem.inError && !(cacheItem.loaded && cacheItem.expired)) {
                                return@launch
                            }
                        }
                    } catch (e: Throwable) {
                    }

                    this@CachedProvider.requestQueueKeys[key] = request.priority
                    this@CachedProvider.requestQueue.add(Pair(key, request), request.priority)
                } finally {
                    decrementAwaitingQueueAddition()
                }
            }
            this@CachedProvider.lastQueueAddition = Date();
            this@CachedProvider.processQueue();
        }
    }

    private fun processQueue() {
        if (this.processQueuePending) {
            return
        }
        val now = Date();
        val timeToWait = this.requestDelayMilliseconds - (now.time - this.lastQueueAddition.time)
        if (timeToWait <= 0) {
            GlobalScope.launch {
                queueMutex.withLock {
                    while (this@CachedProvider.numActiveRequests.get() < maxActiveRequests && this@CachedProvider.requestQueue.size() > 0) {
                        val request = this@CachedProvider.requestQueue.pop()
                        if (request != null) {
                            this@CachedProvider.numActiveRequests.getAndIncrement();
                            this@CachedProvider.cache[request.first] = request.second;
                            request.second.sendRequest();
                            this@CachedProvider.requestQueueKeys.remove(request.first);
                        }
                    }
                }
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
