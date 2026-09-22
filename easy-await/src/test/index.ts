import { EasyAwait } from '../main/index'

EasyAwait.instance.startThread("test");
EasyAwait.instance.endThread("test");
EasyAwait.instance.join();
