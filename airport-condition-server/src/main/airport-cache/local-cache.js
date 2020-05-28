"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LocalCache = void 0;
const cache_1 = require("./cache");
const fs_1 = require("fs");
const path_1 = require("path");
const easy_await_1 = require("main/easy-await");
class LocalCache extends cache_1.Cache {
    constructor(root) {
        super();
        this.root = root;
    }
    retrieveFile(name, callback) {
        if (this.hasKey(name)) {
            callback(this.retrieve(name));
        }
        let fullPath = path_1.join(this.root, name);
        fs_1.readFile(fullPath, null, (err, data) => {
            if (err != null) {
                easy_await_1.EasyAwait.instance.reportFatalError(`Could not load aiport information from file ${fullPath}.  Error: ${err.message}.`);
            }
            else {
                try {
                    let obj = JSON.parse(data.toString());
                    callback(obj);
                }
                catch (e) {
                    easy_await_1.EasyAwait.instance.reportFatalError(`Error parsing file ${fullPath}`);
                }
            }
        });
    }
}
exports.LocalCache = LocalCache;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoibG9jYWwtY2FjaGUuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJsb2NhbC1jYWNoZS50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOzs7QUFFQSxtQ0FBK0I7QUFDL0IsMkJBQTZCO0FBQzdCLCtCQUEyQjtBQUMzQixnREFBNEM7QUFFNUMsTUFBYSxVQUFXLFNBQVEsYUFBSztJQUdqQyxZQUFZLElBQVk7UUFDcEIsS0FBSyxFQUFFLENBQUM7UUFFUixJQUFJLENBQUMsSUFBSSxHQUFHLElBQUksQ0FBQztJQUNyQixDQUFDO0lBRU0sWUFBWSxDQUFDLElBQVksRUFBRSxRQUFtRDtRQUVqRixJQUFLLElBQUksQ0FBQyxNQUFNLENBQUMsSUFBSSxDQUFDLEVBQUc7WUFDckIsUUFBUSxDQUFDLElBQUksQ0FBQyxRQUFRLENBQUMsSUFBSSxDQUFDLENBQUMsQ0FBQztTQUNqQztRQUdELElBQUksUUFBUSxHQUFHLFdBQUksQ0FBQyxJQUFJLENBQUMsSUFBSSxFQUFFLElBQUksQ0FBQyxDQUFDO1FBQ3JDLGFBQVEsQ0FBQyxRQUFRLEVBQUUsSUFBSSxFQUFFLENBQUMsR0FBaUMsRUFBRSxJQUFZLEVBQUUsRUFBRTtZQUN6RSxJQUFLLEdBQUcsSUFBSSxJQUFJLEVBQUc7Z0JBQ2Ysc0JBQVMsQ0FBQyxRQUFRLENBQUMsZ0JBQWdCLENBQUMsK0NBQStDLFFBQVEsYUFBYSxHQUFHLENBQUMsT0FBTyxHQUFHLENBQUMsQ0FBQzthQUMzSDtpQkFBTTtnQkFDSCxJQUFJO29CQUNBLElBQUksR0FBRyxHQUFHLElBQUksQ0FBQyxLQUFLLENBQUMsSUFBSSxDQUFDLFFBQVEsRUFBRSxDQUFDLENBQUM7b0JBQ3RDLFFBQVEsQ0FBQyxHQUFnQyxDQUFDLENBQUM7aUJBQzlDO2dCQUFDLE9BQU8sQ0FBQyxFQUFFO29CQUNSLHNCQUFTLENBQUMsUUFBUSxDQUFDLGdCQUFnQixDQUFDLHNCQUFzQixRQUFRLEVBQUUsQ0FBQyxDQUFDO2lCQUN6RTthQUNKO1FBQ0wsQ0FBQyxDQUFDLENBQUM7SUFDUCxDQUFDO0NBQ0o7QUE5QkQsZ0NBOEJDIn0=