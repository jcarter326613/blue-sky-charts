import { NavigableMap2d } from './view/navigable-map-2d';

let MapPlugin: any = {}
MapPlugin.install = function testMapModule(Vue: any, options: any) {
    Vue.prototype.$createMap = function (el: string, mapConfirugationFile: string): NavigableMap2d {
        return new NavigableMap2d(el, mapConfirugationFile);
    }
}

export default MapPlugin;