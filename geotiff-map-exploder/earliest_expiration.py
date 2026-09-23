
import map_inventory as mi

map_inventory = mi.read_inventory_metadata()
earliestExpiration = None
map_name = None
for map in map_inventory:
    thisMap = map_inventory[map]
    latestExpiration = None
    for version in thisMap["versions"]:
        thisVersion = thisMap["versions"][version]
        if "expirationDate" in thisVersion and (latestExpiration == None or latestExpiration < thisVersion["expirationDate"]):
            latestExpiration = thisVersion["expirationDate"]
    
    if earliestExpiration == None or latestExpiration < earliestExpiration:
        earliestExpiration = latestExpiration
        map_name = map

print(map_name)
print(earliestExpiration)
