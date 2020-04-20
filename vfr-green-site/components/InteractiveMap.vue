<template>
    <div>
        test map
        {{ message }}
        <div id="cesiumContainer"></div>
    </div>
</template>

<script lang="javascript">
import Vue from 'vue'
import VueCesium from 'vue-cesium'
import lang from 'vue-cesium/lang/en-us'

Vue.use(VueCesium);
var viewer = new Cesium.Viewer('cesiumContainer');

export default Vue.component("interactivemap", {
    data: function() {
        return {"message": "Hello world"};
    },
    mounted: function() {
        // Create the webglearth script
        //let mapScript2 = document.createElement("script");
        //mapScript2.setAttribute("src", "http://www.webglearth.com/v2/api.js");
        //document.head.appendChild(mapScript2);

        // Initialize the map
        //let myMap = L.map('mapid');
        /*
        myMap.setView(L.latLng(43, -71), 0);
        L.tileLayer('http://localhost:3000/{z}/{x}/{y}.png', {
        maxZoom: 18
        }).addTo(myMap);
        */

        // Go here to understand zoom levels and tile sizes: https://leafletjs.com/examples/zoom-levels/
    }
})
</script>

<style scoped>
.viewer {
    width: 100%;
    height: 400px;
}
</style>