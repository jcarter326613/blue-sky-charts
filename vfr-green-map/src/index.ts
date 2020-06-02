import { NavigableMap2d } from './view/navigable-map-2d';
import { OverlayTypes } from './view/overlay-types';

let MapPlugin: any = {}
MapPlugin.install = function testMapModule(Vue: any, options: any) {
    Vue.prototype.$createMap = function (el: string, mapConfirugationFile: string, 
        originLogitude: number | undefined, originLatitude: number | undefined, zoom: number | undefined,
        markLongitude: number | undefined, markLatitude: number | undefined): NavigableMap2d {
        return new NavigableMap2d(el, mapConfirugationFile, originLogitude, originLatitude, zoom, markLongitude, markLatitude);
    };
    Vue.prototype.$OverlayTypes = OverlayTypes;
}

export default MapPlugin;