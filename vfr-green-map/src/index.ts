import { NavigableMap2d } from './view/navigable-map-2d';

let MapPlugin: any = {}
MapPlugin.install = function testMapModule(Vue: any, options: any) {
    Vue.prototype.$createMap = function (el: string): NavigableMap2d {
        return new NavigableMap2d(el);
    }
}

export default MapPlugin;