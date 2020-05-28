"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const index_1 = require("../main/index");
let promise = index_1.handler({ "queryStringParameters": {
        "startLongitude": 1,
        "endLongitude": 2,
        "startLatitude": 1,
        "endLatitude": 3,
        "bufferLongitude": 4,
        "bufferLatitude": 4
    } });
promise.then((value) => {
    console.log(value);
});
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiaW5kZXguanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJpbmRleC50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOztBQUFBLHlDQUF1QztBQUV2QyxJQUFJLE9BQU8sR0FBRyxlQUFPLENBQUMsRUFBQyx1QkFBdUIsRUFBQztRQUMzQyxnQkFBZ0IsRUFBRSxDQUFDO1FBQ25CLGNBQWMsRUFBRSxDQUFDO1FBQ2pCLGVBQWUsRUFBRSxDQUFDO1FBQ2xCLGFBQWEsRUFBRSxDQUFDO1FBQ2hCLGlCQUFpQixFQUFFLENBQUM7UUFDcEIsZ0JBQWdCLEVBQUUsQ0FBQztLQUN0QixFQUFDLENBQUMsQ0FBQztBQUNKLE9BQU8sQ0FBQyxJQUFJLENBQUMsQ0FBQyxLQUFVLEVBQUUsRUFBRTtJQUN4QixPQUFPLENBQUMsR0FBRyxDQUFDLEtBQUssQ0FBQyxDQUFDO0FBQ3ZCLENBQUMsQ0FBQyxDQUFBIn0=