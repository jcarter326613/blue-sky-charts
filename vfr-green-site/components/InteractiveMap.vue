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
        this.$createMap(this.parentId);
    },
    props: ["parentId"],
    render: function() {
        return []
    }
})
</script>
