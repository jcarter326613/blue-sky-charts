"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.EasyAwait = void 0;
let EasyAwait = (() => {
    class EasyAwait {
        constructor() {
            this.started = false;
            this.threadCount = 0;
            this.promise = undefined;
            this.fatalErrorReported = false;
        }
        initialize() {
            this.started = false;
            this.threadCount = 0;
            this.promise = new Promise((resolve) => {
                this.wait(resolve);
            });
        }
        startThread() {
            if (this.hasFatalError()) {
                return;
            }
            this.threadCount++;
            this.started = true;
        }
        endThread() {
            if (this.hasFatalError()) {
                return;
            }
            this.threadCount--;
            if (this.threadCount < 0) {
                this.reportFatalError("EasyAwait thread count unbalanced.");
            }
        }
        async join() {
            await this.promise;
        }
        hasFatalError() {
            return this.fatalErrorReported;
        }
        getFatalMessage() {
            return this.fatalErrorMessage;
        }
        reportFatalError(logMessage, publicMessage = undefined) {
            console.error(logMessage);
            this.fatalErrorReported = true;
            this.fatalErrorMessage = publicMessage;
            this.threadCount = 0;
            this.started = true;
        }
        wait(resolve) {
            if (!this.started || this.threadCount > 0) {
                setTimeout(() => this.wait(resolve), 1);
            }
            else {
                resolve();
            }
        }
    }
    EasyAwait.instance = new EasyAwait();
    return EasyAwait;
})();
exports.EasyAwait = EasyAwait;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZWFzeS1hd2FpdC5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbImVhc3ktYXdhaXQudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7O0FBQUE7SUFBQSxNQUFhLFNBQVM7UUFRbEI7WUFDSSxJQUFJLENBQUMsT0FBTyxHQUFHLEtBQUssQ0FBQztZQUNyQixJQUFJLENBQUMsV0FBVyxHQUFHLENBQUMsQ0FBQztZQUNyQixJQUFJLENBQUMsT0FBTyxHQUFHLFNBQVMsQ0FBQztZQUN6QixJQUFJLENBQUMsa0JBQWtCLEdBQUcsS0FBSyxDQUFDO1FBQ3BDLENBQUM7UUFFTSxVQUFVO1lBQ2IsSUFBSSxDQUFDLE9BQU8sR0FBRyxLQUFLLENBQUM7WUFDckIsSUFBSSxDQUFDLFdBQVcsR0FBRyxDQUFDLENBQUM7WUFDckIsSUFBSSxDQUFDLE9BQU8sR0FBRyxJQUFJLE9BQU8sQ0FBTyxDQUFDLE9BQU8sRUFBRSxFQUFFO2dCQUN6QyxJQUFJLENBQUMsSUFBSSxDQUFDLE9BQU8sQ0FBQyxDQUFDO1lBQ3ZCLENBQUMsQ0FBQyxDQUFDO1FBQ1AsQ0FBQztRQUVNLFdBQVc7WUFDZCxJQUFLLElBQUksQ0FBQyxhQUFhLEVBQUUsRUFBRztnQkFDeEIsT0FBTzthQUNWO1lBQ0QsSUFBSSxDQUFDLFdBQVcsRUFBRSxDQUFDO1lBQ25CLElBQUksQ0FBQyxPQUFPLEdBQUcsSUFBSSxDQUFDO1FBQ3hCLENBQUM7UUFFTSxTQUFTO1lBQ1osSUFBSyxJQUFJLENBQUMsYUFBYSxFQUFFLEVBQUc7Z0JBQ3hCLE9BQU87YUFDVjtZQUNELElBQUksQ0FBQyxXQUFXLEVBQUUsQ0FBQztZQUNuQixJQUFLLElBQUksQ0FBQyxXQUFXLEdBQUcsQ0FBQyxFQUFHO2dCQUN4QixJQUFJLENBQUMsZ0JBQWdCLENBQUMsb0NBQW9DLENBQUMsQ0FBQzthQUMvRDtRQUNMLENBQUM7UUFFTSxLQUFLLENBQUMsSUFBSTtZQUNiLE1BQU0sSUFBSSxDQUFDLE9BQU8sQ0FBQztRQUN2QixDQUFDO1FBRU0sYUFBYTtZQUNoQixPQUFPLElBQUksQ0FBQyxrQkFBa0IsQ0FBQztRQUNuQyxDQUFDO1FBRU0sZUFBZTtZQUNsQixPQUFPLElBQUksQ0FBQyxpQkFBaUIsQ0FBQztRQUNsQyxDQUFDO1FBRU0sZ0JBQWdCLENBQUMsVUFBa0IsRUFBRSxnQkFBb0MsU0FBUztZQUNyRixPQUFPLENBQUMsS0FBSyxDQUFDLFVBQVUsQ0FBQyxDQUFDO1lBQzFCLElBQUksQ0FBQyxrQkFBa0IsR0FBRyxJQUFJLENBQUM7WUFDL0IsSUFBSSxDQUFDLGlCQUFpQixHQUFHLGFBQWEsQ0FBQztZQUN2QyxJQUFJLENBQUMsV0FBVyxHQUFHLENBQUMsQ0FBQztZQUNyQixJQUFJLENBQUMsT0FBTyxHQUFHLElBQUksQ0FBQztRQUN4QixDQUFDO1FBRU8sSUFBSSxDQUFDLE9BQW1CO1lBQzVCLElBQUksQ0FBQyxJQUFJLENBQUMsT0FBTyxJQUFJLElBQUksQ0FBQyxXQUFXLEdBQUcsQ0FBQyxFQUFFO2dCQUN2QyxVQUFVLENBQUMsR0FBRyxFQUFFLENBQUMsSUFBSSxDQUFDLElBQUksQ0FBQyxPQUFPLENBQUMsRUFBRSxDQUFDLENBQUMsQ0FBQzthQUMzQztpQkFBTTtnQkFDSCxPQUFPLEVBQUUsQ0FBQzthQUNiO1FBQ0wsQ0FBQzs7SUFsRWEsa0JBQVEsR0FBYyxJQUFJLFNBQVMsRUFBRSxDQUFDO0lBbUV4RCxnQkFBQztLQUFBO0FBcEVZLDhCQUFTIn0=