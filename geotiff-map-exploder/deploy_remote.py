import map_inventory as mi
import sys

map_inventory = mi.read_inventory_metadata()
for m in map_inventory:
    del map_inventory[m]["mapBounds"] 

mi.write_inventory_metadata(map_inventory, sys.argv[1], False)