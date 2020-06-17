package com.blueskycharts.app.map.resources

abstract class CachedProviderRequest( private val provider: CachedProvider, val priority: Int ) {
    var loaded: Boolean = false
        set(value) {
            if ( value ) {
                field = true;
                inError = false;
            } else {
                field = false;
            }
        }
        get() {
            return field && !this.inError;
        }

    var inError: Boolean = false
        set(value) {
            if ( value ) {
                field = true;
                loaded = false;
            } else {
                field = false;
            }
        }

    abstract fun sendRequest();

    abstract fun broadcastData(immediate: Boolean);

    protected fun completeRequest(isSuccess: Boolean) {
        this.provider.completeRequest();
        if ( isSuccess ) {
            this.loaded = true
            this.broadcastData(false);
        } else {
            this.inError = true
        }
    }
}