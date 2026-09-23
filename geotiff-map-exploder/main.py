import map_inventory as mi
import numpy as np
import rasterio
import rasterio.features
import rasterio.warp
import matplotlib.pyplot as pl

map_inventory = mi.read_inventory_metadata()
for map in map_inventory:
    map_section = map_inventory[map]
    version = map_section["version"]

    map_version = {}
    map_version["effectiveDate"] = "2020-07-01"
    map_version["fileExtent"] = map_section["fileExtent"]
    map_version["imageHeight"] = map_section["imageHeight"]
    map_version["imageWidth"] = map_section["imageWidth"]
    map_version["mapBounds"] = map_section["mapBounds"]
    map_version["maxZoom"] = map_section["maxZoom"]
    map_version["tileWidth"] = map_section["tileWidth"]
    
    map_section = {}
    map_section["versions"] = {}
    map_section["versions"][version] = map_version
    
    map_inventory[map] = map_section
mi.write_inventory_metadata(map_inventory)