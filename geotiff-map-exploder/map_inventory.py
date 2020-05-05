import json

def read_inventory_metadata():
    try:
        f = open("maps/metadata.json", "r")
        inventory = json.load(f)
        f.close()
        return inventory
    except:
        return {}
    #file_lines = f.readlines()
    #if len(file_lines) == 0:
    #    return {}

    #raise BaseException("Not implemented")

def write_inventory_metadata(metadata):
    metadata_string = json.dumps(metadata, indent=4, sort_keys=True)
    f = open("maps/metadata.json", "w")
    f.write(metadata_string)
    f.close()
