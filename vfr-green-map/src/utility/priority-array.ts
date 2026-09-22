
export class PriorityArray<T> {
    private content: Array<Array<T>>;
    private s: number;
    private lowestPriority: number;

    constructor() {
        this.content = [[]];
        this.s = 0;
        this.lowestPriority = 0;
    }

    public clear(): void {
        this.content = [];
        this.s = 0;
    }

    public add(obj: T, priority: number): void {
        // Setup the priority and set the size of this object
        if ( this.s == 0 || this.lowestPriority > priority ) {
            this.lowestPriority = priority;
        }
        this.s++;
        while ( this.content.length <= priority ) {
            this.content.push([]);
        }

        // Insert the object
        this.content[priority].push(obj);
    }

    public pop(): T {
        this.s--;
        let retVal = this.content[this.lowestPriority].pop();

        // Adjust the lowest priority if we've used this one up
        while ( this.content[this.lowestPriority].length == 0 && this.s > 0 ) {
            this.lowestPriority++;
        }

        // Return the object
        if ( retVal === undefined ) {
            return this.pop();
        }
        return retVal;
    }

    public size(): number {
        return this.s;
    }

    public getPriorityList(priority: number): Array<T> {
        return this.content[priority];
    }
}