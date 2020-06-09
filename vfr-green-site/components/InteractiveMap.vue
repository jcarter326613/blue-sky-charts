<template><div id="mapContainer" class="interactiveMap"></div></template>

<script lang="javascript">
import Vue from 'vue'
import MyMap from 'vfr-green-map'

Vue.use(MyMap)
 
export default Vue.component("interactivemap", {
    data: function() {
        return {};
    },
    mounted: function() {
        this.$interactiveMap = this.$createMap("mapContainer", "/maps/world-vfr", this.originLongitude, this.originLatitude, this.zoom,
            this.markLongitude, this.markLatitude);
        this.$interactiveMap.setOverlayType(this.overlayType);
    },
    props: [
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

<style scoped>
    .interactiveMap {
        position: relative;
        height: 100%;
        width: 100%;
        overflow: hidden;
    }
    .interactiveMap canvas {
        position: relative;
        width: 100%;
        height: 100%;
    }
</style>