# Jenkins Plugin - Custom Build Properties

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/custom-build-properties.svg)](https://plugins.jenkins.io/custom-build-properties)

## Purpose

Add custom properties to a build.
There are different ways of setting and getting them:
* Pipeline steps: setCustomBuildProperty, getCustomBuildProperty
* Exported to Remote API
* HTTP GET and POST (e.g. http://.../someJob/lastCompletedBuild/custombuildproperties/get?key=MyKey)

The properties are displayed on the build summary page - per default as a key value table.

The step waitForCustomBuildProperties can be used for synchronization in parallel branches.

### Multi column table rendering

Per default custom build properties are displayed as a key value table. In order to create a multi column table you need to add a regex matching two groups: 1st is the rowName, 2nd is the columnName.
The regex is applied to each key. Add the regex as custom build property using the special key `_cbp_table_<tablename>`.

Example: `setCustomBuildProperty(key: '_cbp_table_Activities', value: 'Activity-(.*?)-(.*)')`

### Formatting

Custom build properties can be used to display additional information to users.
For this purpose keys and values can be formatted using HTML. It is even possible to embed SVG.
The values are sanitized by default using the global Jenkins MarkupFormatter.

SVG is not well supported by the existing MarkupFormatters therefore an internal one (SvgAwareSanitizer) can be used by
setting a custom build property using the special key `_cbp_sanitizer_<tablename>_<rowname>_<columnname>` and value `internal`.

Example: `setCustomBuildProperty(key: '_cbp_sanitizer_Key_Activity-Graph_Value', value: 'internal')`

## License
[MIT License](http://opensource.org/licenses/MIT)

## Versioning

This plugin uses automatic versioning with a manually controlled prefix.
