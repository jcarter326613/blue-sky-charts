import sys
import map_inventory as mi
import urllib.request
import shutil
import zipfile

from define_crops import define_crops
from os import path

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
map_inventory[map_name]["tileWidth"] = 256

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

# Save off the changes to the map inventory
mi.write_inventory_metadata(map_inventory)

# Define the crop area
existing_bounds = None
if "mapBounds" in map_inventory[map_name]:
    existing_bounds = map_inventory[map_name]["mapBounds"]
map_bounds = define_crops(tif_file, existing_bounds)
if map_bounds != None and len(map_bounds) > 0:
    map_bounds.append(map_bounds[0])
    map_inventory[map_name]["mapBounds"] = map_bounds
    mi.write_inventory_metadata(map_inventory)
