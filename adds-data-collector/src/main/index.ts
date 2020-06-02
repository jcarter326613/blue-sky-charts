import { AddsFileLoader } from './adds-file-loader';
import { DatabaseWriter } from './io/database-writer';
import { EasyAwait } from './easy-await'
import { MetarFileLoader } from './metar-file-loader';
import { S3Writer } from './io/s3-writer';

export const handler = async (event: any = {}): Promise<any> => {
    EasyAwait.instance.initialize();

    // Create the list of file loaders
    let fileLoaders: Array<AddsFileLoader> = new Array<AddsFileLoader>();
    fileLoaders.push(new MetarFileLoader());

    EasyAwait.instance.startThread();

    // Process each dataserver file
    let writer = DatabaseWriter.OverrideWriter;
    if ( writer === undefined ) {
        writer = new S3Writer();
    }
    fileLoaders.forEach((loader) => {
        if ( writer !== undefined ) {
            loader.generateFiles(writer);
        }
    });

    EasyAwait.instance.endThread();
    await EasyAwait.instance.join();
}
