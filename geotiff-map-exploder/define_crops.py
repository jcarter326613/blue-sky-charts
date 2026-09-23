import tkinter
from PIL import Image, ImageTk
import sys
import math

_previous_points = None
_previous_lines = None
_canvas = None
_canvas_image = None
_last_middle = None

def define_crops(tif_path, existing_points):
    global _previous_points
    global _previous_lines
    global _canvas
    global _canvas_image
    global _canvas_offset

    # Read in the image and scale it to something manageable
    Image.MAX_IMAGE_PIXELS = 300000000
    image = Image.open(tif_path)
    original_size = image.size
    image = image.resize((6000, math.floor((image.size[1] / float(image.size[0])) * 6000)), Image.ANTIALIAS)
    scaled_size = image.size

    # Create the widow and canvas we are going to draw on
    window = tkinter.Tk()
    _canvas = tkinter.Canvas(window, width=image.size[0], height=image.size[1])
    _canvas.pack()
    image_tk = ImageTk.PhotoImage(image)

    _canvas_image = _canvas.create_image(image.size[0] // 2, image.size[1] // 2, image=image_tk)

    _previous_points = []
    _previous_lines = []
    _canvas_offset = (0,0)

    # Add the existing points to the map
    if existing_points != None:
        for i in range(len(existing_points) - 1):
            point = existing_points[i]
            scaled_point = (round(point[0] * scaled_size[0] / original_size[0]), \
                round(point[1] * scaled_size[1] / original_size[1]))
            if len(_previous_points) > 0:
                _previous_lines.append(_canvas.create_line(_previous_points[-1][0] + _canvas_offset[0], \
                    _previous_points[-1][1] + _canvas_offset[1], \
                    scaled_point[0], scaled_point[1]))
            
            _previous_points.append(scaled_point)

    _canvas.bind("<Button-1>", _left_down_callback)
    _canvas.bind("<Button-3>", _right_down_callback)
    _canvas.bind("<Button-2>", _middle_down_callback)
    _canvas.bind("<Left>", _middle_down_callback)
    _canvas.focus_set()
    tkinter.mainloop()

    # Correct the points for the scaled image
    scaled_previous_points = []
    for point in _previous_points:
        new_point = (round(point[0] * original_size[0] / scaled_size[0]), round(point[1] * original_size[1] / scaled_size[1]))
        if new_point[0] >= original_size[0]:
            new_point = (original_size[0] - 1, new_point[1])
        if new_point[1] >= original_size[1]:
            new_point = (new_point[0], original_size[1] - 1)
        if new_point[0] < 0:
            new_point = (0, new_point[1])
        if new_point[1] < 0:
            new_point = (new_point[0], 0)
        scaled_previous_points.append(new_point)
    return scaled_previous_points

def _left_down_callback(event):
    global _previous_points
    global _previous_lines
    global _canvas
    global _canvas_offset

    this_point = (event.x, event.y)
    if _previous_points != None and len(_previous_points) > 0:
        _previous_lines.append(_canvas.create_line(_previous_points[-1][0] + _canvas_offset[0], \
            _previous_points[-1][1] + _canvas_offset[1], \
            this_point[0], this_point[1]))
    
    _previous_points.append((this_point[0] - _canvas_offset[0], this_point[1] - _canvas_offset[1]))

def _right_down_callback(event):
    global _previous_points
    global _previous_lines
    global _canvas

    if _previous_points != None and len(_previous_points) > 0:
        if len(_previous_points) > 1:
            _canvas.delete(_previous_lines.pop())
        _previous_points.pop()

def _middle_down_callback(event):
    global _last_middle
    global _canvas
    global _previous_lines
    global _canvas_offset

    if _last_middle == None:
        _last_middle = (event.x, event.y)
    else:
        xDiff = event.x - _last_middle[0]
        yDiff = event.y - _last_middle[1]
        _canvas.move(_canvas_image, xDiff, yDiff)
        for line in _previous_lines:
            _canvas.move(line, xDiff, yDiff)
        _canvas_offset = (_canvas_offset[0] + xDiff, _canvas_offset[1] + yDiff)
        _last_middle = None