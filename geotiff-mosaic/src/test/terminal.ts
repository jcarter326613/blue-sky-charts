import { handler } from '../main/original/terminal'
import { Loader } from '../main/original/loader'

Loader.skipTileGeneration = false
handler()