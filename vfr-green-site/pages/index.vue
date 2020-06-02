<template>
  <div class="frame">
    <div>
      <input type="radio" id="overlayTypeNone" :value="$OverlayTypes.None" v-model="selectedOverlayType">
      <label for="overlayTypeNone">None</label>
      <input type="radio" id="overlayTypeCeiling" :value="$OverlayTypes.Ceiling" v-model="selectedOverlayType">
      <label for="overlayTypeCeiling">Ceiling</label>
    </div>
    <div class="expandingFrameElement" id="mapContainer"><interactivemap parentId="mapContainer" 
      :originLongitude="originLongitude" :originLatitude="originLatitude" :zoom="zoom"
      :markLongitude="markLongitude" :markLatitude="markLatitude" :overlayType="selectedOverlayType"/></div>
    <div class="staticFrameElement">test content {{ message }}</div>
  </div>
</template>

<script lang="javascript">
import Vue from 'vue'
import MyMap from 'vfr-green-map'
import "~/components/InteractiveMap.vue"

Vue.use(MyMap)

export default Vue.extend({
  data: function() {
      return {
        "message": "Hello world2",
        "originLongitude": this.$route.query.longitude,
        "originLatitude": this.$route.query.latitude,
        "zoom": this.$route.query.zoom,
        "markLongitude": this.$route.query.markLongitude,
        "markLatitude": this.$route.query.markLatitude,
        "selectedOverlayType": this.$OverlayTypes.None
      };
  }
})
</script>

<style scoped>
.frame {
    display: flex;
    position: absolute;
    height: 100%;
    width: 100%;
    flex-direction: column;
    align-items: stretch;
}
.expandingFrameElement {
    flex-grow: 1;
    font-size: 0px;
}
.staticFrameElement {
    flex-grow: 0;
}
</style>