import 'package:flutter/material.dart';
import 'package:preferences/preferences.dart';

class ColorMaps {
  // Maps string key to corresponding color
  static const Map<String, Color> _themeColors = {
    "Green": Colors.lightGreen,
    "Red": Colors.red,
    "Blue": Colors.blue,
    "Yellow": Colors.yellow,
    "Purple": Colors.purple,
    "Brown": Colors.brown,
    "Grey": Colors.grey
  };

  // Gets color from preferences and returns color based on type
  static Color getTypeColor(String type) {
    // Maps color names to actual color
    Map<String, Color> _colors = {
      "Green": Colors.lightGreen,
      "Brown": Colors.brown[400],
      "Yellow": Colors.lime,
      "Pink": Colors.pinkAccent,
      "Blue": Colors.indigoAccent,
      "Grey": Colors.grey
    };

    // Maps tea type to user defined color
    Map<String, Color> _types = {
      "Green": _colors[PrefService.getString("green_tea_color")] ?? _colors["Green"],
      "Black": _colors[PrefService.getString("black_tea_color")] ?? _colors["Brown"],
      "Oolong": _colors[PrefService.getString("oolong_tea_color")] ?? _colors["Yellow"],
      "White": _colors[PrefService.getString("white_tea_color")] ?? _colors["Grey"],
      "Herbal": _colors[PrefService.getString("Herbal_tea_color")] ?? _colors["Pink"],
      "Other": _colors[PrefService.getString("other_tea_color")] ?? _colors["Blue"]
    };

    return _types[type];
  }

  static Map get themeColors => _themeColors;
}
