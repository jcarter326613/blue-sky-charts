import { DatabaseWriter } from './database-writer'
import { EasyAwait } from '../easy-await'
import * as fs from 'fs';

export class LocalWriter extends DatabaseWriter {
    private directory: string;

    constructor(directory: string) {
        super();
        this.directory = directory;
    }

    public writeFiles(dataset: string, fileName: string, data: string): void {
        EasyAwait.instance.startThread();
        let fullPathDir = `${this.directory}/${dataset}`;
        let fullPath = `${fullPathDir}/${fileName}`;
        fs.mkdirSync(fullPathDir, {recursive: true})
        fs.writeFile(fullPath, data, () => {
            EasyAwait.instance.endThread();
        });
    }
}