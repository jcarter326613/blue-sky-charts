
import json

from os import path
from os import system

def convert_tiff_to_png(image_file):
    new_filename = ".".join(image_file.split(".")[0:-1]) + "_RGB.png"
    if path.exists(new_filename):
        return new_filename

    print("Converting to png")

    docker_command = "docker run --rm -v /home:/home osgeo/gdal:alpine-ultrasmall-latest gdal_translate $PWD/{} $PWD/{}".format(
        image_file, new_filename
    )
    system(docker_command)

    return new_filename

def convert_tiff_to_web_mercator(image_file):
    new_filename = ".".join(image_file.split(".")[0:-1]) + "_WEB.tif"
    if path.exists(new_filename):
        return new_filename

    print("Warping to Web Mercator")
    docker_command = "docker run --rm -v /home:/home osgeo/gdal:alpine-ultrasmall-latest gdalwarp -t_srs EPSG:3857 $PWD/{} $PWD/{}".format(
        image_file, new_filename
    )
    system(docker_command)

    return new_filename

def convert_web_mercator_to_cropped(image_file, geojsonObj):
    alpha_filename = ".".join(image_file.split(".")[0:-1]) + "_ALPHA.tif"
    new_filename = ".".join(image_file.split(".")[0:-1]) + "_CROPPED.tif"
    geojson_filename = ".".join(image_file.split(".")[0:-1]) + "_CROP.geojson"
    if path.exists(new_filename):
        return new_filename

    geojsonString = json.dumps(geojsonObj)
    f = open(geojson_filename, "w")
    f.write(geojsonString)
    f.close()
    
    print("Converting color pallet image to rgba")

    docker_command = "docker run --rm -v /home:/home osgeo/gdal:alpine-ultrasmall-latest gdal_translate -b 1 -expand rgba $PWD/{} $PWD/{}".format(
        image_file, alpha_filename
    )
    system(docker_command)
    
    print("Cropping image")

    docker_command = "docker run --rm -v /home:/home osgeo/gdal:alpine-ultrasmall-latest gdalwarp -cutline $PWD/{} -crop_to_cutline $PWD/{} $PWD/{}".format(
        geojson_filename, alpha_filename, new_filename
    )
    system(docker_command)

    return new_filename