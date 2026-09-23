
import { receiveWebsiteMessage } from '../main/index'

let promise = receiveWebsiteMessage({
    "queryStringParameters": {        
        "name": "Jason Carter",
        "email": "jcarter@naturalhues.com"
    },
    "headers": {
        "origin": "http://localhost:3000/"
    },
    "httpMethod": "POST",
    "body": "A really long message\nwith multiplelines"
});
promise.then((value: any) => {
    console.log(value);
})
