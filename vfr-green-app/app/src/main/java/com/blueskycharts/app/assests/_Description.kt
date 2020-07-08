package com.blueskycharts.app.assests

/**
        Responsible for retrieving file from remote location and locally caching said files according to a specified retention policy
        Responsible for informing others of what files are locally present in the cache
        Responsible for providing a single method of access to files which shields the caller from whether the file is cached or not
        Responsible for adjusting local cache for individual files given a change in caching requirements
        Responsible for providing methods for updating the id of files that exist within the local cache
        Responsible for providing the ability to create aliases for locally cached files
        Responsible for notifying any interested parties if a locally cached file was assigned an alias which
                corresponds to a failed request
        Responsible for allowing files to be grouped
        Responsible for reporting basic statistics on file groups such as file count or size
 */