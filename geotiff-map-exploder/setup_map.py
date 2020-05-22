
import gdal_util
import json
import map_inventory as mi
import math
import os
import rasterio
import rasterio.features
import rasterio.warp
import shutil
import sys
import tqdm
import urllib.request
import zipfile

from define_crops import define_crops
from os import path
from pyproj import Proj, transform

TILE_WIDTH = 256

def getExtent(projectionType, src, geom):
    geom_lat_long = rasterio.warp.transform_geom(
        src.crs, projectionType, geom, precision=6)
    if geom_lat_long["type"] != "Polygon" or \
        len(geom_lat_long["coordinates"]) != 1 or len(geom_lat_long["coordinates"][0]) < 4:
        print("Error in reading geometry")
        exit()
    
    min_longitude = geom_lat_long["coordinates"][0][0][0]
    min_latitude = geom_lat_long["coordinates"][0][0][1]
    max_longitude = min_longitude
    max_latitude = min_latitude

    for thisGeom in geom_lat_long["coordinates"][0]:
        if min_longitude > thisGeom[0]:
            min_longitude = thisGeom[0]
        if min_latitude > thisGeom[1]:
            min_latitude = thisGeom[1]
        if max_longitude < thisGeom[0]:
            max_longitude = thisGeom[0]
        if max_latitude < thisGeom[1]:
            max_latitude = thisGeom[1]

    file_extent = {
        "topLeft": {
            "longitude": min_longitude,
            "latitude": max_latitude
        },
        "bottomLeft": {
            "longitude": min_longitude,
            "latitude": min_latitude
        },
        "bottomRight": {
            "longitude": max_longitude,
            "latitude": min_latitude
        },
        "topRight": {
            "longitude": max_longitude,
            "latitude": max_latitude
        }
    }

    if file_extent["topLeft"]["longitude"] > file_extent["topRight"]["longitude"] or \
        file_extent["bottomLeft"]["longitude"] > file_extent["bottomRight"]["longitude"] or \
        file_extent["topLeft"]["latitude"] < file_extent["bottomLeft"]["longitude"] or \
        file_extent["topRight"]["latitude"] < file_extent["bottomRight"]["longitude"]:
        print("Geometry error.  Points not in correct order")
        exit()

    return file_extent

# Get the name and url of the map to process
print("What map would you like to process?")
map_name = sys.stdin.readline().strip()

map_inventory = mi.read_inventory_metadata()
version = None
if map_name in map_inventory:
    map_definition = map_inventory[map_name]
    if "version" in map_definition:
        version = int(map_definition["version"])
else:
    map_inventory[map_name] = {}

version_question = "What version?"
if version != None:
    version_question += " ({})".format(version)

print(version_question)
version_requested = sys.stdin.readline().strip()

if version_requested == None or len(version_requested) == 0:
    if version == None:
        print("Can not leave version blank when no previous version is loaded.")
        exit()
else:
    version = int(version_requested)
map_inventory[map_name]["version"] = version
map_inventory[map_name]["tileWidth"] = TILE_WIDTH
mi.write_inventory_metadata(map_inventory)

# Download the zip file and extract it
zip_url = "https://aeronav.faa.gov/content/aeronav/sectional_files/{}_{}.zip".format(map_name, version)
zip_file = "maps/{}_{}.zip".format(map_name, version)
tif_file = "maps/{}_SEC_{}.tif".format(map_name, version)

if path.exists(zip_file):
    print("Using cached file " + zip_file)
else:
    print("Pulling zip from " + zip_url)
    urllib.request.urlretrieve(zip_url, zip_file)
with zipfile.ZipFile(zip_file) as z:
    with open(tif_file, "wb") as f:
        f.write(z.read("{} SEC {}.tif".format(map_name, version)))

# Create the web mercator image
web_tiff_path = gdal_util.convert_tiff_to_web_mercator(tif_file)

# Define the crop area
existing_bounds = None
if "mapBounds" in map_inventory[map_name]:
    with rasterio.open(web_tiff_path) as src:
        existing_bounds = map_inventory[map_name]["mapBounds"]
        for i in range(len(existing_bounds)):
            bound = existing_bounds[i]
            outProj = Proj(src.crs.to_proj4())
            inProj = Proj('epsg:4326')
            meterX, meterY = transform(inProj, outProj, bound[1], bound[0])
            y, x = src.index(meterX, meterY)
            existing_bounds[i] = (x, y)

map_bounds = define_crops(web_tiff_path, existing_bounds)
if map_bounds != None and len(map_bounds) > 2:
    with rasterio.open(web_tiff_path) as src:
        for i in range(len(map_bounds)):
            bound = map_bounds[i]
            meterX, meterY = src.xy(row=bound[1], col=bound[0])
            inProj = Proj(src.crs.to_proj4())
            outProj = Proj('epsg:4326')
            map_bounds[i] = transform(inProj,outProj, meterX, meterY)
            map_bounds[i] = [map_bounds[i][1], map_bounds[i][0]]
    map_bounds.append(map_bounds[0])
    map_inventory[map_name]["mapBounds"] = map_bounds
    mi.write_inventory_metadata(map_inventory)
else:
    print("No bounds given.")
    exit()

# Create the cropped image
geojsonGeometryObject = {}
geojsonGeometryObject["type"] = "Polygon"
geojsonGeometryObject["coordinates"] = [map_bounds]

geojsonFeatureObject = {}
geojsonFeatureObject["type"] = "Feature"
geojsonFeatureObject["geometry"] = geojsonGeometryObject

geojsonObj = {}
geojsonObj["type"] = "FeatureCollection"
geojsonObj["features"] = [geojsonFeatureObject]
cropped_tiff_path = gdal_util.convert_web_mercator_to_cropped(web_tiff_path, geojsonObj)

# Get the geographic and physical bounds of the cropped image
with rasterio.open(cropped_tiff_path) as src:
    mask = src.dataset_mask()
    file_extent = None
    print("Finding extents")
    for geom, val in rasterio.features.shapes(
            mask, transform=src.transform): 
        new_extent = getExtent("EPSG:4326", src, geom)
        if file_extent is None:
            file_extent = new_extent
        else:
            if file_extent["topLeft"]["latitude"] < new_extent["topLeft"]["latitude"]:
                file_extent["topLeft"]["latitude"] = new_extent["topLeft"]["latitude"]
                file_extent["topRight"]["latitude"] = new_extent["topLeft"]["latitude"]
            if file_extent["topLeft"]["longitude"] > new_extent["topLeft"]["longitude"]:
                file_extent["topLeft"]["longitude"] = new_extent["topLeft"]["longitude"]
                file_extent["bottomLeft"]["longitude"] = new_extent["topLeft"]["longitude"]
            if file_extent["bottomRight"]["latitude"] > new_extent["bottomRight"]["latitude"]:
                file_extent["bottomRight"]["latitude"] = new_extent["bottomRight"]["latitude"]
                file_extent["bottomLeft"]["latitude"] = new_extent["bottomRight"]["latitude"]
            if file_extent["bottomRight"]["longitude"] < new_extent["bottomRight"]["longitude"]:
                file_extent["bottomRight"]["longitude"] = new_extent["bottomRight"]["longitude"]
                file_extent["topRight"]["longitude"] = new_extent["bottomRight"]["longitude"]
    map_inventory[map_name]["fileExtent"] = file_extent
    map_inventory[map_name]["imageWidth"] = src.width
    map_inventory[map_name]["imageHeight"] = src.height
    map_inventory[map_name]["maxZoom"] = math.ceil(math.log2(src.width / TILE_WIDTH))
mi.write_inventory_metadata(map_inventory)

# Cleanup intermediate files
os.system("rm -f {}".format(tif_file))
os.system("rm -f {}".format(web_tiff_path))