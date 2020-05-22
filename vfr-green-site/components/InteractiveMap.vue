<template></template>

<script lang="javascript">
import Vue from 'vue'
import MyMap from 'vfr-green-map'

Vue.use(MyMap)
 
export default Vue.component("interactivemap", {
    data: function() {
        return {};
    },
    mounted: function() {
        this.$createMap(this.parentId, "/maps/world-vfr", this.originLongitude, this.originLatitude, this.zoom,
            this.markLongitude, this.markLatitude);
    },
    props: [
        "parentId",
        "originLatitude",
        "originLongitude",
        "markLatitude",
        "markLongitude",
        "zoom"
    ],
    render: function() {
        return []
    }
})
</script>
