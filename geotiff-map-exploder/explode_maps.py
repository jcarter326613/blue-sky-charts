import gdal_util
import map_inventory as mi
import math
import numpy as np
import os
import pandas as pd
import rasterio
import sys

from os import mkdir
from os import path
from os import system
from PIL import Image
from rasterio.enums import ColorInterp
from rasterio.enums import Resampling
from tqdm import tqdm

def _explode_map_helper(image_cache_folder, zoom_level, original_image, original_image_dimensions, this_xy, tile_width, max_zoom, zoom_restriction):
    # Check if we've zoomed far enough
    if zoom_level > max_zoom or (zoom_restriction != None and zoom_level > zoom_restriction):
        return

    if zoom_restriction is None or zoom_restriction == zoom_level:
        # Make sure the zoom directory exists
        zoom_directory = "/".join([image_cache_folder, str(zoom_level)])
        if not path.exists(zoom_directory):
            mkdir(zoom_directory)

        # Get the crop area for this x, y, and zoom level
        block_size = (original_image_dimensions[0] / (2 ** zoom_level), \
            (original_image_dimensions[1] / (2 ** zoom_level)))
        crop_dimensions = (this_xy[0] * block_size[0], this_xy[1] * block_size[1], \
            (this_xy[0] + 1) * block_size[0], (this_xy[1] + 1) * block_size[1])
        crop_dimensions = (math.floor(crop_dimensions[0]), math.floor(crop_dimensions[1]), \
            math.floor(crop_dimensions[2]), math.floor(crop_dimensions[3]))
        cropped_image = original_image.crop(crop_dimensions)

        # Resize the image and write it out
        cropped_image.thumbnail((tile_width, tile_width), Image.ANTIALIAS)
        cropped_image.save(zoom_directory + "/{}_{}.png".format(this_xy[0], this_xy[1]), "PNG")

    # Split the image into 4 equal parts and recurse
    next_coordinate = (this_xy[0] * 2, this_xy[1] * 2)

    _explode_map_helper(image_cache_folder, zoom_level + 1, original_image, original_image_dimensions, \
        next_coordinate, tile_width, max_zoom, zoom_restriction)
    _explode_map_helper(image_cache_folder, zoom_level + 1, original_image, original_image_dimensions, \
        (next_coordinate[0] + 1, next_coordinate[1]), tile_width, max_zoom, zoom_restriction)
    _explode_map_helper(image_cache_folder, zoom_level + 1, original_image, original_image_dimensions, \
        (next_coordinate[0], next_coordinate[1] + 1), tile_width, max_zoom, zoom_restriction)
    _explode_map_helper(image_cache_folder, zoom_level + 1, original_image, original_image_dimensions, \
        (next_coordinate[0] + 1, next_coordinate[1] + 1), tile_width, max_zoom, zoom_restriction)

def explode_map(name, definition, image_cache_folder, image_location, tile_width, max_zoom, zoom_restriction):
    print("Exploding map " + name)

    # Load the original image
    Image.MAX_IMAGE_PIXELS = 300000000
    image = Image.open(image_location)

    print("Expected max zoom level: ciel({}), actual max zoom: {}, zoom restriction {}".format(math.log2(image.width / tile_width), max_zoom, zoom_restriction))

    _explode_map_helper(image_cache_folder, 0, image, (image.width, image.height), (0, 0), tile_width, max_zoom, zoom_restriction)

def explode_maps(map_name, version, location_name, zoom_restriction):
    if location_name not in ["local", "remote", "relative"]:
        print("bad location")
        exit()

    inventory = mi.read_inventory_metadata()
    map_definition = inventory[map_name]["versions"][version]

    if "tileWidth" not in map_definition:
        print("Missing tile width for map " + map_name)
        exit()

    tile_width = map_definition["tileWidth"]
    max_zoom = map_definition["maxZoom"]
    image_path = "maps/{}_SEC_{}_WEB_CROPPED.tif".format(map_name, version)
    image_cache_folder = "maps/tiles/{}_SEC_{}".format(map_name, version)
    if os.path.exists("maps/tiles"):
        os.system("rm -rf maps/tiles")
    os.makedirs(image_cache_folder)
    png_image_path = gdal_util.convert_tiff_to_png(image_path)
    explode_map(map_name, map_definition, image_cache_folder, png_image_path, tile_width, max_zoom, zoom_restriction)
    if location_name == "local":
        if not os.path.exists("../vfr-green-site/static/maps/world-vfr/sectional"):
            os.makedirs("../vfr-green-site/static/maps/world-vfr/sectional")
        os.system("mv {} ../vfr-green-site/static/maps/world-vfr/sectional".format(image_cache_folder))
    elif location_name == "remote":
        os.system("aws s3 sync ./maps/tiles s3://blueskycharts.com/maps/world-vfr/sectional")
    #os.system("rm -f {}*".format(png_image_path))

if len(sys.argv) not in [4,5]:
    print("Usage python3 explode_maps.py <mapname> <version> <local|remote> (<zoom_restriction>)")
    exit()
    
zoom_restriction = None
if len(sys.argv) == 5:
    zoom_restriction = int(sys.argv[4])
explode_maps(sys.argv[1], sys.argv[2], sys.argv[3], zoom_restriction)