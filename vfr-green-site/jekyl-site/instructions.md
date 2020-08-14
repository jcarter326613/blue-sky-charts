---
layout: page
title: Instructions
contentAsMarkDown: true
---

* TOC
{:toc}

# Basic Features
The Blue Sky Charts app can be used to consult FAA VFR charts as well as overlay weather information and your current location.

# Loading the app for the first time
On your first launch of the application after download, you will be presented with a privacy policy and terms of service.  You must accept these in order to continue using the app.  You will also be asked to opt into data collection using Firebase.  It is recommended that you check this box so as to help us detect and issues you may face when using the app as well to help us understand how you are navigating through the app.  This information helps us to make improvements in future releases.

Once you've accepted the privacy policy, you are asked to sign up for a free trial for the subscription required to use the app.  The app is subscription based so as to provide you with the current FAA charts as well as updated weather data.  The subscription period is one year and the free trial lasts for 14 days.  We make it easy to manage your subscription, including cancel prior to the end fo the free trial.  Instructions can be found in the [Manage my subscription](#manage-my-subscription) section.

# Map View screen
The Map View screen is where you will spend most of your time while using this app.  It allows you to view your selected chart as well as view weather overlay data.  The map can be moved by dragging your finger across the screen.  The map can be zoomed by pinching in or out.

When you first load the app, you will be presented with the world view.  This map combines all the sectional charts into a single view where each map is transformed so that they can be matched up.  This transformation is known as <a href="https://en.wikipedia.org/wiki/Web_Mercator_projection" target="_blank">Web Mercator</a>.  Untransformed Sectional and Terminal Charts with full margins can be selected for view using the menu.

## Selecting a map to view
To change the map displayed, follow these steps.

1. Click the menu on the Map View screen.
2. Click *Maps*.
3. Click the category containin the map you want to view.
4. Click the map you want to view.  You may need to scroll the list by dragging up or down on the screen.

You will immediately be brought to the Map View screen with the selected map displaying.  If you do not have an internet connection and have not downloaded the selected map, you may see a blank screen.  The map will load when internet connection has been restored.  All maps under the *VFR Sectional* and *VFR Terminal* groups are named according to the official map names as provided by the FAA.

## Overlaying weather data
Weather data can be overlayed on any map.  To add a weather overlay, follow these steps.

1. Click the menu on the Map View screen.
2. Click *Weather*.
3. Click the overlay you wish to view.  You may need to scroll the list up or down to find the overlay you want.  You can do this by dragging your finger up or down on the screen.
4. If you wish to turn off the weather overlay, click the *None* overlay.

Once an overlay other than *None* is selected, you will see the *Loading weather data...* message.  When this message is displayed, the app is downloading weather information over the internet.  When the information has been loaded, the most typical scenario is that you will be shown the requested overlay information along with a message indicating when the information was issued.  As you move around the map, the issue time is altered to reflect those weather stations which are currently visible.

The issue time is the time at which the weather station reported its observation.  It is important to consider the age of an observation when making determinations about current weather conditions.  Typically, weather stations will report their observations once every hour with additional reports if weather conditions change significantly.  The Blue Sky Charts app uses data that is cached on our servers to provide a stable user experience.  Due to this caching, it may be several minutes before a new report is able to be seen on the app.  You must therefore make use of alternate sources, such as ATIS or AWOS, when you need real-time weather data such as just prior to taxi.

Depending on your zoom level and how many weather stations are visible on your view of the map, you may see a range of issue times.  The range of visible issue times are indicated with two background colors.  Any weather observation displayed that corresponds to the earliest issue time will be outlines in the corresponding background color.  Similarly, the oldest observations will be displayed in the darker background color.  For any information that was issued between the earliest and latest time, the shade of the outline of those observations will be adjusted to lie between the two background colors.

If you are viewing an area where there are no weather stations or the most recent observation is older than 2 hours, the issue time will not be displayed and no observation data will be visible.

## Weather data symbols
For a description on the various symbols and conventions used to represent data on the map, please refer to the [Weather Symbols](/instructions/seather-symbols) page

## Current location
You current location will be displayed on a map using a blue dot under the following conditions.

1. You have granted permission for the app to get your location from your device.
2. Your device is able to determine its location using a GPS signal.  If your device does not have a clear view of the sky, this may not be possible.
3. Your location is on your currently selected map.

If these conditions are met, you will be able to turn on tracking mode, where the map will stay centered on your current location as you move, by hitting the crosshair icon next to the menu icon.

# Offline maps
In order to download a map so that you can access it when your device is in airplane mode or for when you are unable to receive data over the internet on your mobile device, follow these steps.

1. Click the menu on the Map View screen
2. Click *Settings*
3. Click *Downloads*
4. Expand the category of map you wish to download
5. Click the map you wish to download

At this point, the map will begin downloading if you are on an un-metered wifi connection.  If you are not on an un-metered wifi connection, the map will be prevented from downloading to prevent accidential consumption of mobile data.  Please note that if a wifi connection falsly reports to your device that it is un-metered, the app will not be able to detect this scenario and will proceed with a download.

Once the map completed downloading, you will be able to view it any time whether connected to the internet or not.

# Manage my subscription
If you wish to modify your payment method or cancel your subscription, you can do so through the following steps.

1. Click the menu on the Map View screen
2. Click *Account*
3. Click *Manage Subscription*

You will be taken to the Google Play app with the subscription for Blue Sky Charts loaded.  There are links here to update your payment method and cancel your subscription.  If you choose to cancel, your subscription will continue until the next payment cycle.  There are no refunds for partial year usage.  If you've cancelled during your 14 day free trial, no money will be collected or owed.