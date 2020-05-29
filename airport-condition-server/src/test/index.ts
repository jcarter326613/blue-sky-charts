import { Cache } from '../main/airport-cache/cache'
import { LocalCache } from '../main/airport-cache/local-cache'
import { handler } from '../main/index'

Cache.overrideCache = new LocalCache("./data/test");

let promise = handler({"queryStringParameters":{
    "startLongitude": -160,
    "endLongitude": -80,
    "startLatitude": 76.53,
    "endLatitude": -11,
    "bufferLongitude": 4,
    "bufferLatitude": 4
}});
promise.then((value: any) => {
    console.log(value);
})
