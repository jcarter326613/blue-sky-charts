export class EasyAwait {
    public static instance: EasyAwait = new EasyAwait();
    private promise: Promise<void> | undefined;
    private threadCount: number;
    private started: boolean;

    private constructor() {
        this.started = false;
        this.threadCount = 0;
        this.promise = undefined;
    }

    public initialize(): void {
        this.started = false;
        this.threadCount = 0;
        this.promise = new Promise<void>((resolve) => {
            this.wait(resolve);
        });
    }

    public startThread(): void {
        this.threadCount++;
        this.started = true;
    }

    public endThread(): void {
        this.threadCount--;
        if ( this.threadCount < 0 ) {
            console.error("EasyAwait thread count unbalanced.");
            throw new Error("EasyAwait thread count unbalanced.");
        }
    }

    public async join(): Promise<void> {
        await this.promise;
    }

    private wait(resolve: () => void): void {
        if (!this.started || this.threadCount > 0) {
            setTimeout(() => this.wait(resolve), 1);
        } else {
            resolve();
        }
    }
}