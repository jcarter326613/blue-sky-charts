import { Cache } from '../main/airport-cache/cache'
import { LocalCache } from '../main/airport-cache/local-cache'
import { handler } from '../main/index'

Cache.overrideCache = new LocalCache("./data/test");

let promise = handler({
    "queryStringParameters": {        
        "startLongitude": -118,
        "endLongitude": -94,
        "startLatitude": 43,
        "endLatitude": 55,
        "bufferLongitude": 1.0,
        "bufferLatitude": 0.25,
        "information": "gust"
    },
    "headers": {
        "origin": "http://localhost:3000/"
    },
    "httpMethod": "GET"
});
promise.then((value: any) => {
    console.log(value);
})
