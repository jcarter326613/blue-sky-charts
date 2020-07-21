import { Cache } from '../main/airport-cache/cache'
import { LocalCache } from '../main/airport-cache/local-cache'
import { handler } from '../main/index'

Cache.overrideCache = new LocalCache("./data/test");

let promise = handler({
    "queryStringParameters": {        
        "startLongitude": -81.36735781587564,
        "endLongitude": -66.99873435926018,
        "startLatitude": 30.903992421593472,
        "endLatitude": 49.830785203499744,
        "bufferLongitude": 1.0,
        "bufferLatitude": 0.25,
        "information": "wind"
    },
    "headers": {
        "origin": "http://localhost:3000/"
    },
    "httpMethod": "GET"
});
promise.then((value: any) => {
    console.log(value);
})
