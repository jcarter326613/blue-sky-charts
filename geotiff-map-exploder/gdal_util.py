
from os import path
from os import system

def convert_tiff_to_rgb(image_file):
    new_filename = ".".join(image_file.split(".")[0:-1]) + "_RGB.png"
    if path.exists(new_filename):
        return new_filename

    print("Converting color pallet image to rgb")

    '''
    docker run --rm -v /home:/home osgeo/gdal:alpine-ultrasmall-latest gdalwarp -t_srs EPSG:3857 $PWD/Nome_SEC_60.tif $PWD/test.tif
    '''
    docker_command = "docker run --rm -v /home:/home osgeo/gdal:alpine-ultrasmall-latest gdal_translate -b 1 -expand rgb $PWD/{} $PWD/{}".format(
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
