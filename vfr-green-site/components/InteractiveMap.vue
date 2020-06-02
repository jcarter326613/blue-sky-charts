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
        this.$interactiveMap = this.$createMap(this.parentId, "/maps/world-vfr", this.originLongitude, this.originLatitude, this.zoom,
            this.markLongitude, this.markLatitude);
    },
    props: [
        "parentId",
        "originLatitude",
        "originLongitude",
        "markLatitude",
        "markLongitude",
        "zoom",
        "overlayType"
    ],
    watch: {
        overlayType: function(newVal, oldVal) {
            if (this.$interactiveMap !== undefined) {
                this.$interactiveMap.setOverlayType(newVal);
            }
        }
    },
    render: function() {
        return []
    }
})
</script>
