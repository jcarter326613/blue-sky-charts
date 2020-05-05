import numpy as np
import rasterio
import rasterio.features
import rasterio.warp
import matplotlib.pyplot as pl

# Read raster bands directly to Numpy arrays.
#
with rasterio.open('Bethel SEC 61.tif') as src:
    # Read the dataset's valid data mask as a ndarray.
    mask = src.dataset_mask()

    print("Transform: " + str(src.transform))
    # Extract feature shapes and values from the array.
    for geom, val in rasterio.features.shapes(
            mask, transform=src.transform):

        # Transform shapes from the dataset's own coordinate
        # reference system to CRS84 (EPSG:4326).
        geomLatLong = rasterio.warp.transform_geom(
            src.crs, 'EPSG:4326', geom, precision=6)

        # Print GeoJSON shapes to stdout.
        print("Geometry: " +str(geomLatLong))
        x = geom["coordinates"][0][0][0]
        y = geom["coordinates"][0][0][1]
        index = src.index(x, y)
        print("Index({},{}): {}".format(x, y, index))

        #xy = src.xy(x,y)
        #print("xy(0,0): " + str(xy))
        
        '''
        x_initial_index = src.bounds.left
        y_initial_index = src.bounds.top
        index = src.index(x_initial_index, y_initial_index)
        print("Index(0,0): " + str(index))
        '''
        print("width: {}, height: {}".format(src.width, src.height))

        image_data = np.array(src.read(1))
        image_data = image_data[index[0]:,index[1]:]
        pl.imshow(image_data, cmap='pink')
        pl.show()

        break