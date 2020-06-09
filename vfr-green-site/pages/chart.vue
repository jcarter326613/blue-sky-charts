<template>
  <div class="frame noselect">
    <div class="controls">
      <div @click="toggleLayerControls" class="dropDownHeader">
        <div class="dropDownHeaderLabel">Layers</div>
        <div v-show="!showLayerControls" class="dropDownHeaderControl">+</div>
        <div v-show="showLayerControls" class="dropDownHeaderControl">-</div>
      </div>
      <div v-show="showLayerControls" class="dropDownContents">
        <input type="radio" id="overlayTypeNone" :value="$OverlayTypes.None" v-model="selectedOverlayType">
        <label for="overlayTypeNone">None</label><br>
        <input type="radio" id="overlayTypeCeiling" :value="$OverlayTypes.Ceiling" v-model="selectedOverlayType">
        <label for="overlayTypeCeiling">Ceiling</label><br>
        <input type="radio" id="overlayTypeVisibility" :value="$OverlayTypes.Visibility" v-model="selectedOverlayType">
        <label for="overlayTypeVisibility">Visibility</label><br>
        <input type="radio" id="overlayTypeCloudCover" :value="$OverlayTypes.CloudCover" v-model="selectedOverlayType">
        <label for="overlayTypeCloudCover">Cloud Cover</label><br>
        <input type="radio" id="overlayTypeWind" :value="$OverlayTypes.Wind" v-model="selectedOverlayType">
        <label for="overlayTypeWind">Wind</label><br>
        <input type="radio" id="overlayTypeTempC" :value="$OverlayTypes.TempC" v-model="selectedOverlayType">
        <label for="overlayTypeTempC">Temperature Celsius</label><br>
        <input type="radio" id="overlayTypeDewpointC" :value="$OverlayTypes.DewpointSpreadC" v-model="selectedOverlayType">
        <label for="overlayTypeDewpointC">Dewpoint Spread Celsius</label><br>
        <input type="radio" id="overlayTypeCategory" :value="$OverlayTypes.Category" v-model="selectedOverlayType">
        <label for="overlayTypeCategory">Category</label>
      </div>
    </div>
    <div class="mapContainer"><interactivemap
      :originLongitude="originLongitude" :originLatitude="originLatitude" :zoom="zoom"
      :markLongitude="markLongitude" :markLatitude="markLatitude" :overlayType="selectedOverlayType"/></div>
  </div>
</template>

<script lang="javascript">
import Vue from 'vue'
import MyMap from 'vfr-green-map'
import "~/components/InteractiveMap.vue"

Vue.use(MyMap)

export default Vue.extend({
  data: function() {
      let showLayerControls;
      try {
        showLayerControls = screen.orientation.type.indexOf("landscape") >= 0
      } catch (e) {
        showLayerControls = true;
      }
      let overlayType = this.$OverlayTypes.None;
      switch(this.$route.query.overlay) {
        case "ceiling": {
          overlayType = this.$OverlayTypes.Ceiling;
          break;
        }
        case "wind": {
          overlayType = this.$OverlayTypes.Wind;
          break;
        }
        default: {
          break;
        }
      }
      return {
        "originLongitude": this.$route.query.longitude,
        "originLatitude": this.$route.query.latitude,
        "zoom": this.$route.query.zoom,
        "markLongitude": this.$route.query.markLongitude,
        "markLatitude": this.$route.query.markLatitude,
        "selectedOverlayType": overlayType,
        "showLayerControls": showLayerControls
      };
  },
  methods: {
    toggleLayerControls(e) {
      e.preventDefault();
      e.stopPropagation();
      this.showLayerControls = !this.showLayerControls;
    }
  },
  watch: {
    selectedOverlayType: function( newValue, oldValue ) {
      try {
        let isLandscape = screen.orientation.type.indexOf("landscape") >= 0
        if ( !isLandscape ) {
          this.showLayerControls = false;
        }
      } catch (e) {
      }
    }
  }
})
</script>

<style scoped>
.frame {
  display: flex;
  position: absolute;
  height: 100%;
  width: 100%;
  flex-direction: row;
  align-items: stretch;
}
.mapContainer {
  flex-grow: 1;
  font-size: 0px;
  overflow: hidden;
  z-index: 1;
}
.controls {
  flex-grow: 0;
  width: 15em;
  background-color: rgb(20,20,20);
  color: rgb(200,200,200);
  cursor: pointer;
  z-index: 2;
}
.dropDownHeader {
  display: flex;
  flex-direction: row;
}
.dropDownContents, .dropDownHeader {
  padding: 1em;
  border-bottom: 1px solid rgb(200,200,200);
}
.dropDownHeaderLabel {
  flex-grow: 1;
}
.dropDownHeaderControl {
  flex-grow: 0;
}
.noselect {
  -webkit-touch-callout: none; /* iOS Safari */
  -webkit-user-select: none; /* Safari */
  -khtml-user-select: none; /* Konqueror HTML */
  -moz-user-select: none; /* Firefox */
  -ms-user-select: none; /* Internet Explorer/Edge */
  user-select: none; /* Non-prefixed version, currently
                        supported by Chrome and Opera */
  -webkit-tap-highlight-color:  rgba(255, 255, 255, 0);
}

@media screen and (orientation: portrait) {
  .controls {
    float: left;
    position: absolute;
    width: 100%;
  }
}
</style>