import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
import 'package:leaf_log/models/color_maps.dart';
import 'package:preferences/preferences.dart';

class PreferenceMenu extends StatefulWidget {
  @override
  _PreferenceMenuState createState() => _PreferenceMenuState();
}

class _PreferenceMenuState extends State<PreferenceMenu> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Preferences"),
        actions: <Widget>[
          IconButton(
            icon: Icon(Icons.check),
            onPressed: () {},
          )
        ],
      ),
      body: PreferencePage([
        PreferenceTitle('Customization'),
        DropdownPreference(
          "Theme Color",
          "theme_color",
          defaultVal: 'Green',
          values: ['Red', 'Blue', 'Green', 'Yellow', 'Purple', 'Brown', 'Grey'],
          onChange: (value) {
            _setTheme(value);
          },
        )
      ]),
    );
  }

  _setTheme(String color) {
    DynamicTheme.of(context)
        .setThemeData(ThemeData(primarySwatch: ColorMaps.themeColors[color]));
  }
}
