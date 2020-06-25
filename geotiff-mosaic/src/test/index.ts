import { handler } from '../main/index'

let promise = handler();
promise.then((value: any) => {
    console.log(value);
})
