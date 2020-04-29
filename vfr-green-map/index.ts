import * as $ from 'jquery'

let MapPlugin: any = {}
MapPlugin.install = function testMapModule(Vue, options) {
    Vue.prototype.$createMap = function (el) {
        $(el).text("This is put in by the plugin");
    }
}

export default MapPlugin;