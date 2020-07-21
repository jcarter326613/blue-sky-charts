import { appVersion } from '../main/index'

let promise = appVersion({
    "queryStringParameters": {        
        "appVersion": "1.0.0",
        "privacyVersion": "2020-07-20"
    },
    "headers": {
        "origin": "http://localhost:3000/"
    },
    "httpMethod": "GET"
});
promise.then((value: any) => {
    console.log(value);
})
