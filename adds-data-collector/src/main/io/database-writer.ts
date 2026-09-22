export abstract class DatabaseWriter {
    public static OverrideWriter: DatabaseWriter | undefined;

    public abstract writeFiles(dataset: string, fileName: string, data: string): void;
}