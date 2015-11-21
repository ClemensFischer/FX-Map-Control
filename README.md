# FX Map Control

A set of controls for Java FX for rendering digital maps from different providers and various types
of map overlays. Similar to the Bing Maps control on XAML platforms.

Main classes are

- **MapBase**: The core map control. Provides properties like Center, ZoomLevel and Heading, which
define the currently displayed map viewport.

- **Map**: MapBase with basic mouse and touch input handling for zoom, pan, and rotation.

- **MapTileLayer**: Provides tiled map content (e.g. from OpenStreetMap) by means of a **TileSource**.

- **MapImageLayer**: Provides map content that covers the entire viewport (e.g. from a Web Map Service).

- **MapItemsControl**: Displays a collection of **MapItem** objects (with a geographic **Location**).

Please take a look at the SampleApplication project to learn more.

--- 

This library is a port of the [XAML Map Control Library on Codeplex](http://xamlmapcontrol.codeplex.com/) to Java FX.
