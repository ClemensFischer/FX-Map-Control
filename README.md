# FX Map Control

A set of Java FX controls for rendering digital maps from different providers and various types
of map overlays.

This library is a port of [XAML Map Control](https://github.com/ClemensFischer/XAML-Map-Control) to Java FX.

--- 

Main classes are

- **MapBase**: The core map control. Provides properties like Center, ZoomLevel and Heading, which
define the currently displayed map viewport.

- **Map**: MapBase with basic mouse and touch input handling for zoom, pan, and rotation.

- **MapTileLayer**: Provides tiled map content (e.g. from OpenStreetMap) by means of a **TileSource**.

- **MapImageLayer**: Provides map content that covers the entire viewport (e.g. from a Web Map Service).

- **MapItemsControl**: Displays a collection of **MapItem** objects (with a geographic **Location**).

--- 

Please take a look at the SampleApplication project to learn more.
