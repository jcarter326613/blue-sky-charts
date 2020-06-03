import { Cache } from '../main/airport-cache/cache'
import { LocalCache } from '../main/airport-cache/local-cache'
import { handler } from '../main/index'

Cache.overrideCache = new LocalCache("./data/test");

let promise = handler({
    "queryStringParameters": {        
        "startLongitude": -180,
        "endLongitude": 180,
        "startLatitude": -90,
        "endLatitude": 90,
        "bufferLongitude": 16,
        "bufferLatitude": 16,
        "information": "ceiling"
    },
    "headers": {
        "origin": "http://localhost:3000/"
    },
    "httpMethod": "GET"
});
promise.then((value: any) => {
    console.log(value);
})
