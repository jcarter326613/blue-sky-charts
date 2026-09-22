package com.blueskycharts.app.map.assetmanagement

/**
        Responsible for ensuring any local caches of imagery data associated with a map is up to date with the current version
        Responsible for ensuring any local caches of imagery data associated with a future known map version is up to date
        Responsible for enforcing desired caching policies on local imagery data (removing expired content)
        Responsible for reporting on the condition of the local cache and its difference from the desired state
        Responsible for ensuring cached content which applies to multiple versions of a map is properly aliased
 */