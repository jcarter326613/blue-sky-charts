
export class Condition {
    public longitude: number;
    public latitude: number;
    
    public ceiling: number | undefined;
    public visibility: number | undefined;
    public cloudCover: string | undefined;
    public windSpeed: number | undefined;
    public windDirection: number | undefined;
    public windGust: number | undefined;
    public temperatureCelcius: number | undefined;
    public dewpointSpreadCelcius: number | undefined;
    public flightCategory: string | undefined;

    constructor() {
        this.longitude = 0;
        this.latitude = 0;
    }
}