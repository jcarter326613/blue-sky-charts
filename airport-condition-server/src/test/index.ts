import { Cache } from '../main/airport-cache/cache'
import { LocalCache } from '../main/airport-cache/local-cache'
import { handler } from '../main/index'

Cache.overrideCache = new LocalCache("./data/test");

let promise = handler({
    "queryStringParameters": {
        
        "startLongitude": -115.354736328125,
        "endLongitude": -50.645263671875,
        "startLatitude": 21.570073988213277,
        "endLatitude": 44.37101850747771,
        
        /*
        "startLongitude": -82,
        "endLongitude": -81,
        "startLatitude": 32,
        "endLatitude": 33,
        */
        "bufferLongitude": 1,
        "bufferLatitude": 1
    },
    "headers": {
        "origin": "http://localhost:3000/"
    },
    "httpMethod": "GET"
});
promise.then((value: any) => {
    console.log(value);
})
