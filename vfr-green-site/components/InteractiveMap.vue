<template>
    <div>
        test map
        {{ message }}
    </div>
</template>

<script lang="ts">
import Vue from 'vue'
import L from 'leaflet'
import { LMap, LTileLayer, LMarker } from 'vue2-leaflet'

export default Vue.component("interactivemap", {
  data: function() {
      return {"message": "Hello world"};
  }
})
</script>