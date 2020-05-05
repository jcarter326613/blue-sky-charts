import map_inventory as mi
import rasterio

from os import mkdir
from os import path
from PIL import Image

def explode_map(name, definition, image_cache_folder, image_location):
    mkdir(image_cache_folder)
    zoom_directory = "/".join(image_cache_folder, "0")
    with rasterio.open(image_location) as src:
        

def explode_maps():
    inventory = mi.read_inventory_metadata()
    for map_name in inventory:
        map_definition = inventory[map_name]
        image_cache_folder = "maps/{}_SEC_{}".format(map_name, map_definition["version"])
        image_path = image_cache_folder + ".tif"
        if not path.exists(image_path):
            explode_map(map_name, map_definition, image_cache_folder, image_path)

explode_maps()