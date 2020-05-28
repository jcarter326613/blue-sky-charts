import { handler } from '../main/index'

let promise = handler({"queryStringParameters":{
    "startLongitude": 1,
    "endLongitude": 2,
    "startLatitude": 1,
    "endLatitude": 3,
    "bufferLongitude": 4,
    "bufferLatitude": 4
}});
promise.then((value: any) => {
    console.log(value);
})
