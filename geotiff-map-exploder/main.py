import numpy as np
import rasterio
import rasterio.features
import rasterio.warp

# Read raster bands directly to Numpy arrays.
#
with rasterio.open('Bethel SEC 61.tif') as src:
    # Read the dataset's valid data mask as a ndarray.
    mask = src.dataset_mask()

    # Extract feature shapes and values from the array.
    for geom, val in rasterio.features.shapes(
            mask, transform=src.transform):

        # Transform shapes from the dataset's own coordinate
        # reference system to CRS84 (EPSG:4326).
        geom = rasterio.warp.transform_geom(
            src.crs, 'EPSG:4326', geom, precision=6)

        # Print GeoJSON shapes to stdout.
        print(geom)

