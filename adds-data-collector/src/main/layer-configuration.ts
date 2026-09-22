
export class LayerConfiguration {
    public name: string;
    public maxZoom: number;

    constructor(name: string) {
        this.name = name;
        this.maxZoom = 0;
    }
}