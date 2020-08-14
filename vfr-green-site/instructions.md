---
layout: page
title: Instructions
contentAsMarkDown: true
---

The Blue Sky Charts app can be used to consult FAA VFR charts as well as overlay current weather information and your current location.  Instructions on how to use the app can be found here.

* TOC
{:toc}

# Loading the app for the first time
On your first launch of the application after download, you will be presented with a privacy policy and terms of service.  You must accept these to continue using the app.  You will also be asked to opt into data collection using Firebase.  It is recommended that you check this box to help us detect and issues you may face when using the app as well to help us understand how you are navigating through the app.  This information helps us to make improvements in future releases.

Once you've accepted the privacy policy, you are asked to sign up for a free trial for the subscription required to use the app.  The app is subscription-based to provide you with the current FAA charts as well as updated weather data.  The subscription period is one year and the free trial lasts for 14 days.  We make it easy to manage your subscription, including cancel before the end of the free trial.  Instructions can be found in the [Manage my subscription](#manage-my-subscription) section.

# Map View screen
The Map View screen is where you will spend most of your time while using this app.  It allows you to view your selected chart as well as view weather overlay data.  The map can be moved by dragging your finger across the screen.  The map can be zoomed by pinching in or out.

When you first load the app, you will be presented with the world view.  This map combines all the sectional charts into a single view where each map is projected so that they can be matched up.  This transformation is known as <a href="https://en.wikipedia.org/wiki/Web_Mercator_projection" target="_blank">Web Mercator</a>.  Untransformed Sectional and Terminal Charts with full margins can be selected for view using the menu.

## Selecting a map to view
To change the map displayed, follow these steps.

1. Click the menu on the Map View screen.
2. Click *Maps*.
3. Click the category containing the map you want to view.
4. Click the map you want to view.  You may need to scroll the list by dragging up or down on the screen.

You will immediately be brought to the Map View screen with the selected map displaying.  If you do not have an internet connection and have not downloaded the selected map, you may see a blank screen.  The map will load when the internet connection has been restored.  All maps under the *VFR Sectional* and *VFR Terminal* groups are named according to the official map names as provided by the FAA.

## Overlaying weather data
Weather data can be overlayed on any map.  To add a weather overlay, follow these steps.

1. Click the menu on the Map View screen.
2. Click *Weather*.
3. Click the overlay you wish to view.  You may need to scroll the list up or down to find the overlay you want.  You can do this by dragging your finger up or down on the screen.
4. If you wish to turn off the weather overlay, click the *None* overlay.

Once an overlay other than *None* is selected, you will see the *Loading weather data...* message.  When this message is displayed, the app is downloading weather information over the internet.  When the information has been loaded, the most typical scenario is that you will be shown the requested overlay information along with a message indicating when the information was issued.  As you move around the map, the issue time is altered to reflect those weather stations which are currently visible.

The issue time is the time at which the weather station reported its observation.  It is important to consider the age of an observation when making determinations about current weather conditions.  Typically, weather stations will report their observations once every hour with additional reports if weather conditions change significantly.  The Blue Sky Charts app uses data that is cached on our servers to provide a stable user experience.  Due to this caching, it may be several minutes before a new report can be seen on the app.  You must, therefore, make use of alternate sources, such as ATIS or AWOS, when you need real-time weather data such as just before taxi.

Depending on your zoom level and how many weather stations are visible on your view of the map, you may see a range of issue times.  The range of visible issue times is indicated with two background colors.  Any weather observation displayed that corresponds to the earliest issue time will be outlined in the corresponding background color.  Similarly, the oldest observations will be displayed in the darker background color.  For any information that was issued between the earliest and latest time, the shade of the outline of those observations will be adjusted to lie between the two background colors.

If you are viewing an area where there are no weather stations or the most recent observation is older than 2 hours, the issue time will not be displayed and no observation data will be visible.

## Weather data representation
For a description on the various symbols and conventions used to represent data on the map, please refer to the [Weather Symbols](/instructions/weather-symbols) page

## Current location
Your current location will be displayed on a map using a blue dot under the following conditions.

1. You have granted permission for the app to get your location from your device.
2. Your device can determine its location using a GPS signal.  If your device does not have a clear view of the sky, this may not be possible.
3. Your location is on your currently selected map.

If these conditions are met you will be able to turn on tracking mode by hitting the crosshair icon next to the menu icon.   In tracking mode, the map stays centered on your current location as you move.

# Offline maps
To download a map so that you can access it when your device is in airplane mode or for when you are unable to receive data over the internet on your mobile device, follow these steps.

1. Click the menu on the Map View screen.
2. Click *Settings*.
3. Click *Downloads*.
4. Expand the category of the map you wish to download.
5. Click the map you wish to download.

At this point, the map will begin downloading if you are on an un-metered wifi connection.  If you are not on an un-metered wifi connection, the map will be prevented from downloading to prevent accidental consumption of mobile data.  Please note that if a wifi connection falsely reports to your device that it is un-metered, the app will not be able to detect this scenario and will proceed with a download.

Once the map completes downloading, you will be able to view it any time whether connected to the internet or not.

# Managing memory usage
Maps occupy storage space on your device in two categories.  The first category is maps that you opt to download for offline viewing.  To remove these from your device and free up memory, simply follow the instructions for [downloading offline maps](#offline-maps) and in step 5, click the map that is already downloaded.  This will remove it from the list of offline maps.

The second category of maps stored on your device is cached maps.  As you view maps that are not marked for offline storage, the parts that you are viewing are stored locally in a cache.  This cache has a maximum size of 50 Mb.  When you reach the size limit of the cache, parts of maps that you haven't viewed recently will be erased to make room for the parts you're currently viewing.  If for some reason you would like to clear the cache entirely and start over, you can do so by applying the following steps.

1. Click the menu on the Map View screen.
2. Click *Settings*.
3. Click *Memory*.
4. Click *Clear Cache Data*.

You should see the *Cached content* amount quickly go down to 0 Mb.

# Managing privacy settings
To help us track any instability in the app and learn about how our customers are using our app in the field, we ask that you allow us to collect some personal information.  More about what information we collect can be found by referring to our [privacy policy](/privacy.html).  If you wish to opt-in or out of the *crash* and *behavior tracking* data collection, follow these steps.

1. Click the menu on the Map View screen.
2. Click *Settings*.
3. Click *Privacy*.
4. Click the checkbox at the top of the page so that it is checked (if you want to share data) or unchecked (if you do not want to share data).
5. Press the *Save Changes* button.

You must click *Save Changes* for your selection to be applied.  Once you've clicked the button, you will be taken back to the Map View screen.

# Manage my subscription
If you wish to modify your payment method or cancel your subscription, you can do so by applying the following steps.

1. Click the menu on the Map View screen
2. Click *Account*
3. Click *Manage Subscription*

You will be taken to the Google Play app with the subscription for Blue Sky Charts loaded.  There are links here to update your payment method and cancel your subscription.  If you choose to cancel, your subscription will continue until the next payment cycle.  There are no refunds for partial-year usage.  If you've canceled during your 14-day free trial, no money will be collected nor owed.