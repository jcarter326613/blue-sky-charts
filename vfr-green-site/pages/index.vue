<template>
  <div class="frame">
    <div class="expandingFrameElement" id="mapContainer"><interactivemap parentId="mapContainer"/></div>
    <div class="staticFrameElement">test content {{ message }}</div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import "~/components/InteractiveMap.vue"

export default Vue.extend({
  data: function() {
      return {"message": "Hello world2"};
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