import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
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
    // Maps string key to corresponding color
    final Map<String, Color> _colorMap = {
      "Green": Colors.lightGreen,
      "Red": Colors.red,
      "Blue": Colors.blue,
      "Yellow": Colors.yellow,
      "Purple": Colors.purple,
      "Brown": Colors.brown,
      "Grey": Colors.grey
    };

    DynamicTheme.of(context)
        .setThemeData(ThemeData(primarySwatch: _colorMap[color]));
  }
}
