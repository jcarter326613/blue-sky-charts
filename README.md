# Blue Sky Charts

Archived historical project. Blue Sky Charts was a beta Android application for viewing FAA VFR charts in a map-style interface, including downloadable charts for offline use and FAA weather overlays. This repository was assembled from the original separate repositories hosted on another Git server. It is retained as a record of the product and its supporting systems, not as an actively maintained or deployable application.

## Projects

- `vfr-green-app`: The Kotlin Android application, including chart rendering, offline tile caching, GPS location, weather overlays, subscriptions, and privacy controls.
- `adds-data-collector`: Scheduled AWS Lambda that retrieved and processed aviation weather observations.
- `airport-condition-server`: AWS API that served weather-overlay data to the clients.
- `support-server`: AWS API that handled website support requests through SES.
- `version-authorization`: AWS API used by the Android app to check version and policy status.
- `geotiff-map-exploder`: Python tooling for preparing FAA GeoTIFF chart data and map tiles.
- `geotiff-mosaic`: TypeScript tooling for generating and publishing sectional, terminal, and combined chart mosaics.
- `vfr-green-map`: Browser-based TypeScript map component that shared the chart and weather concepts.
- `coordinates`: Shared TypeScript geographic, Web Mercator, and 2-D coordinate utilities.
- `easy-await`: Shared TypeScript helpers for asynchronous work and Lambda HTTP handling.
- `vfr-green-site`: Product website, instructions, privacy policy, and terms.
- `graphical-assets`: Source artwork for the Android app and website.
