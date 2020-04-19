<template>
  <div>
    <interactivemap />
    test content
    {{ message }}
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
