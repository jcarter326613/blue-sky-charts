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
        this.arguments
        this.$createMap(this.parentId, "/maps", this.originLongitude, this.originLatitude, this.zoom);
    },
    props: [
        "parentId",
        "originLatitude",
        "originLongitude",
        "zoom"
    ],
    render: function() {
        return []
    }
})
</script>
