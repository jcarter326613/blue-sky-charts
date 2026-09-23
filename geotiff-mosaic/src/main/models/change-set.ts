export class ChangeSet {
    public tiles: Record<number, Record<number, Array<number>>> | undefined  // [zoom, x, y]
}