<template>
  <div class="frame">
    <div class="expandingFrameElement" id="mapContainer"><interactivemap parentId="mapContainer" 
      :originLongitude="originLongitude" :originLatitude="originLatitude" :zoom="zoom"
      :markLongitude="markLongitude" :markLatitude="markLatitude"/></div>
    <div class="staticFrameElement">test content {{ message }}</div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import "~/components/InteractiveMap.vue"

export default Vue.extend({
  data: function() {
      return {
        "message": "Hello world2",
        "originLongitude": this.$route.query.longitude,
        "originLatitude": this.$route.query.latitude,
        "zoom": this.$route.query.zoom,
        "markLongitude": this.$route.query.markLongitude,
        "markLatitude": this.$route.query.markLatitude,
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