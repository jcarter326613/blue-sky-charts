import json

def read_inventory_metadata(location = "maps/metadata.json"):
    try:
        f = open(location, "r")
        inventory = json.load(f)
        f.close()
        return inventory
    except:
        return {}

def write_inventory_metadata(metadata, location = "maps/metadata.json", pp = True):
    if not pp:
        metadata_string = json.dumps(metadata, sort_keys=True)
    else:
        metadata_string = json.dumps(metadata, indent=4, sort_keys=True)
    f = open(location, "w")
    f.write(metadata_string)
    f.close()
