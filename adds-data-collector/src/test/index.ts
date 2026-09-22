import { DatabaseWriter } from '../main/io/database-writer'
import { handler } from '../main/index'
import { LocalWriter } from '../main/io/local-writer'

DatabaseWriter.OverrideWriter = new LocalWriter("./testData");

handler();