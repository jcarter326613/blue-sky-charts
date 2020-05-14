import map_inventory as mi
import math
import numpy as np
import pandas as pd
import rasterio
import gdal_util

from os import mkdir
from os import path
from os import system
from PIL import Image
from rasterio.enums import ColorInterp
from rasterio.enums import Resampling
from tqdm import tqdm

def _explode_map_helper(image_cache_folder, zoom_level, original_image, original_image_dimensions, this_xy, tile_width):
    # Make sure the zoom directory exists
    zoom_directory = "/".join([image_cache_folder, str(zoom_level)])
    if not path.exists(zoom_directory):
        mkdir(zoom_directory)

    # Get the crop are for this x, y, and zoom level
    block_size = (original_image_dimensions[0] / (2 ** zoom_level), \
        (original_image_dimensions[1] / (2 ** zoom_level)))
    crop_dimensions = (this_xy[0] * block_size[0], this_xy[1] * block_size[1], \
        (this_xy[0] + 1) * block_size[0], (this_xy[1] + 1) * block_size[1])
    crop_dimensions = (math.floor(crop_dimensions[0]), math.floor(crop_dimensions[1]), \
        math.floor(crop_dimensions[2]), math.floor(crop_dimensions[3]))
    cropped_image = original_image.crop(crop_dimensions)

    # Resize the image and write it out
    cropped_image.thumbnail((tile_width, tile_width), Image.ANTIALIAS)
    cropped_image.save(zoom_directory + "/{}_{}.jpg".format(this_xy[0], this_xy[1]), "JPEG")

    # Check if we've zoomed far enough
    if (original_image_dimensions[0] / (2 ** zoom_level)) <= tile_width or zoom_level > 7:
        return

    # Split the image into 4 equal parts and recurse
    next_coordinate = (this_xy[0] * 2, this_xy[1] * 2)

    _explode_map_helper(image_cache_folder, zoom_level + 1, original_image, original_image_dimensions, \
        next_coordinate, tile_width)
    _explode_map_helper(image_cache_folder, zoom_level + 1, original_image, original_image_dimensions, \
        (next_coordinate[0] + 1, next_coordinate[1]), tile_width)
    _explode_map_helper(image_cache_folder, zoom_level + 1, original_image, original_image_dimensions, \
        (next_coordinate[0], next_coordinate[1] + 1), tile_width)
    _explode_map_helper(image_cache_folder, zoom_level + 1, original_image, original_image_dimensions, \
        (next_coordinate[0] + 1, next_coordinate[1] + 1), tile_width)

def explode_map(name, definition, image_cache_folder, image_location, tile_width):
    print("Exploding map " + name)
    mkdir(image_cache_folder)

    # Load the original image
    Image.MAX_IMAGE_PIXELS = 300000000
    image = Image.open(image_location)

    print("Expected max zoom level: ciel({})".format(math.log2(image.width / tile_width)))

    _explode_map_helper(image_cache_folder, 0, image, (image.width, image.height), (0, 0), tile_width)

def explode_maps():
    inventory = mi.read_inventory_metadata()
    for map_name in inventory:
        map_definition = inventory[map_name]

        if "tileWidth" not in map_definition:
            print("Missing tile width for map " + map_name)
            exit()

        tile_width = map_definition["tileWidth"]
        image_path = "maps/{}_SEC_{}_WEB.tif".format(map_name, map_definition["version"])
        image_cache_folder = "maps/{}_SEC_{}".format(map_name, map_definition["version"])
        if not path.exists(image_cache_folder):
            rgb_image_path = gdal_util.convert_tiff_to_rgb(image_path)
            explode_map(map_name, map_definition, image_cache_folder, rgb_image_path, tile_width)

explode_maps()
