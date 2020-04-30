import { NavigableMap } from './view/navigable-map';

let MapPlugin: any = {}
MapPlugin.install = function testMapModule(Vue: any, options: any) {
    Vue.prototype.$createMap = function (el: string): NavigableMap {
        return new NavigableMap(el);
    }
}

export default MapPlugin;