import * as $ from 'jquery'

export class NavigableMap {
    private containerDiv: JQuery<HTMLElement>;

    constructor(elementId: string) {
        let jQueryElement = $("#" + elementId);
        if (jQueryElement.length != 1) {
            throw new Error("Must specify a unique html element id to place the map in.");
        }
        this.containerDiv = jQueryElement;

        // Create canvas object and place it in the div
        let canvasObj = document.createElement("canvas");
        let canvasObjHtml = $(canvasObj);
        this.containerDiv.append(canvasObjHtml);

        canvasObjHtml.attr("width", "300px");
        canvasObjHtml.attr("height", "400px");
        let context = canvasObj.getContext("2d");

        context.fillRect(50, 50, 50, 50);
    }
};
