import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
import 'package:leaf_log/models/color_maps.dart';
import 'package:preferences/preference_dialog.dart';
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
        ),
        PreferenceDialogLink(
          'Tea Type Colors',
          dialog: PreferenceDialog([
            DropdownPreference(
              'Green tea',
              'green_tea_color',
              defaultVal: 'Green',
              values: ['Green', 'Brown', 'Yellow', 'Grey', 'Pink', 'Blue'],
            ),
            DropdownPreference(
              'Black tea',
              'black_tea_color',
              defaultVal: 'Brown',
              values: ['Green', 'Brown', 'Yellow', 'Grey', 'Pink', 'Blue'],
            ),
            DropdownPreference(
              'Oolong tea',
              'oolong_tea_color',
              defaultVal: 'Yellow',
              values: ['Green', 'Brown', 'Yellow', 'Grey', 'Pink', 'Blue'],
            ),
            DropdownPreference(
              'White tea',
              'white_tea_color',
              defaultVal: 'Grey',
              values: ['Green', 'Brown', 'Yellow', 'Grey', 'Pink', 'Blue'],
            ),
            DropdownPreference(
              'Herbal tea',
              'Herbal_tea_color', // This crashes if i use a lowercase "h" in "herbal"????????
              defaultVal: 'Pink',
              values: ['Green', 'Brown', 'Yellow', 'Grey', 'Pink', 'Blue'],
            ),
            DropdownPreference(
              'Other tea',
              'other_tea_color',
              defaultVal: 'Blue',
              values: ['Green', 'Brown', 'Yellow', 'Grey', 'Pink', 'Blue'],
            ),
          ]),
        )
      ]),
    );
  }

  _setTheme(String color) {
    DynamicTheme.of(context)
        .setThemeData(ThemeData(primarySwatch: ColorMaps.themeColors[color]));
  }
}
