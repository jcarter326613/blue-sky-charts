import { handler } from '../main/original/sectional'
import { Loader } from '../main/original/loader'

Loader.skipTileGeneration = false
handler()