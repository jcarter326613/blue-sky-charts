export class EasyAwait {
    public static instance: EasyAwait = new EasyAwait();
    private promise: Promise<void> | undefined;
    private threadCount: number;
    private started: boolean;
    private fatalErrorReported: boolean;
    private fatalErrorMessage: string | undefined;

    private constructor() {
        this.started = false;
        this.threadCount = 0;
        this.promise = undefined;
        this.fatalErrorReported = false;
    }

    public initialize(): void {
        this.started = false;
        this.threadCount = 0;
        this.promise = new Promise<void>((resolve) => {
            this.wait(resolve);
        });
    }

    public startThread(): void {
        if ( this.hasFatalError() ) {
            return;
        }
        this.threadCount++;
        this.started = true;
    }

    public endThread(): void {
        if ( this.hasFatalError() ) {
            return;
        }
        this.threadCount--;
        if ( this.threadCount < 0 ) {
            this.reportFatalError("EasyAwait thread count unbalanced.");
        }
    }

    public async join(): Promise<void> {
        await this.promise;
    }

    public hasFatalError(): boolean {
        return this.fatalErrorReported;
    }

    public getFatalMessage(): string | undefined {
        return this.fatalErrorMessage;
    }

    public reportFatalError(logMessage: string, publicMessage: string | undefined = undefined): void {
        console.error(logMessage);
        this.fatalErrorReported = true;
        this.fatalErrorMessage = publicMessage;
        this.threadCount = 0;
        this.started = true;
    }

    private wait(resolve: () => void): void {
        if (!this.started || this.threadCount > 0) {
            setTimeout(() => this.wait(resolve), 1);
        } else {
            resolve();
        }
    }
}