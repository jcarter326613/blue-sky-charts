export class EasyAwait {
    public static instance: EasyAwait = new EasyAwait();
    private promise: Promise<void> | undefined;
    private threadCount: number;
    private threadLocationCount: Record<string, number>;
    private started: boolean;
    private fatalErrorReported: boolean;
    private fatalErrorMessage: string | undefined;
    private fatalErrorIsUserError: boolean;

    private constructor() {
        this.started = false;
        this.threadCount = 0;
        this.threadLocationCount = {};
        this.promise = undefined;
        this.fatalErrorReported = false;
        this.fatalErrorIsUserError = false;
    }

    public initialize(): void {
        this.started = false;
        this.threadCount = 0;
        this.promise = new Promise<void>((resolve) => {
            this.wait(resolve);
        });
    }

    public startThread(location: string): void {
        if ( this.hasFatalError() ) {
            return;
        }
        if ( !(location in this.threadLocationCount) ) {
            this.threadLocationCount[location] = 0;
        }
        this.threadLocationCount[location]++;
        this.threadCount++;
        this.started = true;
    }

    public endThread(location: string): void {
        if ( this.hasFatalError() ) {
            return;
        }
        if ( !(location in this.threadLocationCount) ) {
            this.reportFatalError(`EasyAwait, location ${location} ended but never started`);
            return;
        }
        if ( this.threadLocationCount[location] == 0 ) {
            this.reportFatalError(`EasyAwait, location ${location} ended but is unbalanced`);
            return;
        }
        this.threadLocationCount[location]--;
        this.threadCount--;
        if ( this.threadCount < 0 ) {
            this.reportFatalError("EasyAwait thread count unbalanced in unknown location.");
        }
    }

    public async join(): Promise<void> {
        await this.promise;
    }

    public hasFatalError(): boolean {
        return this.fatalErrorReported;
    }

    public hasUserError(): boolean {
        return this.fatalErrorReported && this.fatalErrorIsUserError;
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

    public reportUserError(logMessage: string): void {
        this.fatalErrorIsUserError = true;
        this.reportFatalError(logMessage);
    }

    private wait(resolve: () => void): void {
        if (!this.started || this.threadCount > 0) {
            setTimeout(() => this.wait(resolve), 1);
        } else {
            resolve();
        }
    }
}