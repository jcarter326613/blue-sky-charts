import tkinter
from PIL import Image, ImageTk
import sys

window = tkinter.Tk()

Image.MAX_IMAGE_PIXELS = 300000000
image = Image.open("Bethel SEC 61.tif")
image = image.resize((1000, 800), Image.ANTIALIAS)
canvas = tkinter.Canvas(window, width=image.size[0] - 200, height=image.size[1] - 200)
canvas.pack()
image_tk = ImageTk.PhotoImage(image)

canvas.create_image(image.size[0]//2, image.size[1]//2, image=image_tk)

previous_point = None
def callback(event):
    global previous_point
    this_point = (event.x, event.y)
    if previous_point != None:
        canvas.create_line(previous_point[0], previous_point[1], this_point[0], this_point[1])
    previous_point = this_point

canvas.bind("<Button-1>", callback)
tkinter.mainloop()