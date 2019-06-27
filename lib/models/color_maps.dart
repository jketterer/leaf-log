import 'package:flutter/material.dart';

class ColorMaps {
  // Maps string key to corresponding color
  static const Map<String, Color> _colorMap = {
    "Green": Colors.lightGreen,
    "Red": Colors.red,
    "Blue": Colors.blue,
    "Yellow": Colors.yellow,
    "Purple": Colors.purple,
    "Brown": Colors.brown,
    "Grey": Colors.grey
  };

  static Map<String, Color> _typeColors = {
    "Green": Colors.lightGreen,
    "Black": Colors.brown[400],
    "Oolong": Colors.lime[500],
    "White": Colors.grey,
    "Herbal": Colors.pink[300],
    "Other": Colors.cyan[200]
  };

  static Map get themeColors => _colorMap;
  static Map get typeColors => _typeColors;
}
