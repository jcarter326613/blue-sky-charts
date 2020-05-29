import { handler } from '../main/index'

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
