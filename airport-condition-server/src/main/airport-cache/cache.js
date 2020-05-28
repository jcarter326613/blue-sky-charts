"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Cache = void 0;
class Cache {
    constructor() {
        this.fileCache = {};
    }
    save(key, contents) {
        this.fileCache[key] = contents;
    }
    hasKey(key) {
        return key in this.fileCache;
    }
    retrieve(key) {
        return this.fileCache[key];
    }
}
exports.Cache = Cache;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiY2FjaGUuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJjYWNoZS50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOzs7QUFHQSxNQUFzQixLQUFLO0lBR3ZCO1FBQ0ksSUFBSSxDQUFDLFNBQVMsR0FBRyxFQUFFLENBQUM7SUFDeEIsQ0FBQztJQUlTLElBQUksQ0FBQyxHQUFXLEVBQUUsUUFBbUM7UUFDM0QsSUFBSSxDQUFDLFNBQVMsQ0FBQyxHQUFHLENBQUMsR0FBRyxRQUFRLENBQUM7SUFDbkMsQ0FBQztJQUVTLE1BQU0sQ0FBQyxHQUFXO1FBQ3hCLE9BQU8sR0FBRyxJQUFJLElBQUksQ0FBQyxTQUFTLENBQUM7SUFDakMsQ0FBQztJQUVTLFFBQVEsQ0FBQyxHQUFXO1FBQzFCLE9BQU8sSUFBSSxDQUFDLFNBQVMsQ0FBQyxHQUFHLENBQUMsQ0FBQztJQUMvQixDQUFDO0NBQ0o7QUFwQkQsc0JBb0JDIn0=