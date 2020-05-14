
import json

from os import path
from os import system

def convert_tiff_to_rgb(image_file):
    new_filename = ".".join(image_file.split(".")[0:-1]) + "_RGB.png"
    if path.exists(new_filename):
        return new_filename

    print("Converting color pallet image to rgba")

    docker_command = "docker run --rm -v /home:/home osgeo/gdal:alpine-ultrasmall-latest gdal_translate -b 1 -expand rgba $PWD/{} $PWD/{}".format(
        image_file, new_filename
    )
    system(docker_command)

    return new_filename

def convert_tiff_to_web_mercator(image_file, geojsonObj):
    new_filename = ".".join(image_file.split(".")[0:-1]) + "_WEB.tif"
    geojson_filename = ".".join(image_file.split(".")[0:-1]) + "_CROP.geojson"
    if path.exists(new_filename):
        return new_filename

    geojsonString = json.dumps(geojsonObj)
    f = open(geojson_filename, "w")
    f.write(geojsonString)
    f.close()

    print("Warping to Web Mercator")

    docker_command = "docker run --rm -v /home:/home osgeo/gdal:alpine-ultrasmall-latest gdalwarp -t_srs EPSG:3857 -cutline $PWD/{} -crop_to_cutline $PWD/{} $PWD/{}".format(
        geojson_filename, image_file, new_filename
    )
    system(docker_command)

    return new_filename
