---
layout: page
title: Weather Symbols
contentAsMarkDown: true
additional_css:
- instructions
---

This page goes over the various symbols used to represent each type of weather observation

* TOC
{:toc}

# Zooming in and out
As you zoom in and out, you may notice that weather indicators appear and disappear.  To keep information on the map as readable as possible while ensuring potentially dangerous conditions are easy to spot, the app will adjust which indicators it shows to ensure the worse conditions for a given area are being displayed in place of more favorable conditions.  For example, if you are zoomed out and two airports appear near each other, one has a ceiling of 5000 feet and the other has a ceiling of 2000 feet, the airport reporting 2000 feet will be the one that's displayed.  As you zoom in and the airports move farther from each other on the screen, both airports will display their information.

# Ceiling
A cloud ceiling is the lowest elevation above sea level where the clouds are overcast or broken.  We represent the cloud ceiling as the number of feet above sea level divided by 100.  For example, if you see a ceiling of *65*, that means there is an overcast or broken layer of clouds 6500 feet above sea level at the weather station at that position on the map.  A ceiling of *>180* means that the ceiling is greater than 18000 feet or that there is no ceiling.

As you zoom out, the station with the lowest ceiling will be shown for an area.

# Visibility
Visibility is given in statute miles.  The number indicated on the weather overlay is the maximum distance in statute miles an observer at the weather station could see.

As you zoom out, the station with the lowest visibility will be shown for an area.

# Cloud Cover
Cloud cover is represented as a white circle that is partially obscured with black based on the category of cloud cover.  

* A fully white circle means *clear skies*
* A quarter covered circle means *few*
* A half-covered circle means *scattered*
* A three quarter covered circle means *broken*
* A fully covered circle means *overcast*
* A circle with an **X** through it means that the sky can not be seen at all.  One example of this being used is when there is a nearby forest fire with smoke obscuring visibility.

The cloud cover shown corresponds to the category with the highest percentage of sky coverage at that location and does not necessarily correspond to the layer being reported for ceiling.  For example, if there is a layer of few clouds at 2000 feet, a ceiling of broken clouds at 3000 feet, and another layer of overcast at 5000 feet, you will see a fully covered circle as the indicator for overcast.

As you zoom out, the station with the most sky coverage will be shown for an area.

# Surface Wind
Surface wind has two components.  Speed and direction.  Blue Sky Charts uses wind barbs to represent surface wind which allows you to see both components using one symbol.  Here is an example of a few wind barbs.

<div class="wind-barbs">
    <div><div><img alt="5 knots" src="/assets/img/weather-icons/wind-barb5.png" /></div><div>5 knots</div></div>
    <div><div><img alt="10 knots" src="/assets/img/weather-icons/wind-barb10.png" /></div><div>10 knots</div></div>
    <div><div><img alt="25 knots" src="/assets/img/weather-icons/wind-barb25.png" /></div><div>25 knots</div></div>
    <div><div><img alt="60 knots" src="/assets/img/weather-icons/wind-barb60.png" /></div><div>60 knots</div></div>
</div>

Wind barbs are made up of three components.  There is the base, which is a small circle, the stem, which is the stick protruding from the base, and the flags, which are the lines or triangles coming out of the stem.  The base and stem indicate the direction the wind is coming from.  If the stem is pointing to the northwest out of the base, the wind is coming from the northwest.  On all maps, regardless of their projections, the wind barb is oriented so that a stem pointing straight up indicates the wind is coming from a true bearing of zero degrees and a stem pointing straight down indicates a wind coming from a true bearing of 180 degrees.

To determine the wind speed indicated by a barb you sum together the value of the flags.  There are three types of flags: triangles, long lines, short lines.  Each triangle represents the number 50.  Each long line represents the number 10.  Each short line, of which there can only be one of, represents the number 5.  When you sum together the values of all the flags present you get the total wind speed rounded up to the nearest 5 knots.  It is important to note that when you only see one line, you can tell if it is a short or long line by looking at the position on the stem.  A short line will only appear by itself halfway up the stem.  A long line will only appear by itself at the tip of the stem.

If the surface wind has a gust component, the gust value plus the base wind speed will be shown as the surface wind speed.  For example, if the wind is gusting from 5 to 15 knots, you will see a barb representing 15 knots.  

There are two other symbols you may see when looking at surface wind.  The first is an empty white circle with a black border.  This indicates the weather station is reporting calm wind.  The second is a white circle with another black circle inside it.  This indicates variable wind which is defined as wind with a changing direction that is equal to or less than 3 knots.

As you zoom out, the station with the highest surface wind will be shown for an area.

# Surface Wind Gust
The component of the [surface wind](#surface-wind) that is due to gusts.  For example, if the surface wind is 15 knots gusting to 20 knots, the surface wind gust will be 5 knots.  As you zoom out, the station with the lowest temperature will be shown for an area.

# Temperature Celcius
Temperature is given in degrees celsius.  As you zoom out, the station with the lowest temperature will be shown for an area.

# Dew Point Spread Celcius
Dewpoint spread is given as the temperature minus the dewpoint in celsius.  As you zoom out, the station with the lowest dewpoint spread will be shown for an area.

# Flight Category
The list of valid flight categories is as follows.

* VFR - Visual Flight Rules
* MVFR - Marginal Visual Flight Rules
* IFR - Instrument Flight Rules
* LIFR - Low Instrument Flight Rules

The flight category is defined by a legal definition of ceiling and visibility thresholds and is not a statement about the safety or future conditions of a flight.  As you zoom out, the station with the most restrictive flight category will be shown for an area.
