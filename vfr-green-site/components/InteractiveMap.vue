<template>
    <div>
        test map
        {{ message }}
        <div id="cesiumContainer"></div>
    </div>
</template>

<script lang="javascript">
import Vue from 'vue'
import MyMap from 'vfr-green-map'

Vue.use(MyMap, {el: "cesiumContainer"})

export default Vue.component("interactivemap", {
    data: function() {
        return {"message": "Hello world"};
    },
    mounted: function() {
        this.$createMap("#cesiumContainer");
    }
})
</script>
