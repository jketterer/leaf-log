import 'package:flutter/material.dart';

class TeaType {
  String name;
  Color color;
  Duration defaultBrewTime;

  TeaType(this.name, this.color, this.defaultBrewTime);

  TeaType.green()
      : name = "Green",
        color = Colors.lightGreen,
        defaultBrewTime = const Duration(minutes: 3);

  TeaType.black()
      : name = "Black",
        color = Colors.brown,
        defaultBrewTime = const Duration(minutes: 5);

  TeaType.oolong()
      : name = "Oolong",
        color = Colors.amber,
        defaultBrewTime = const Duration(minutes: 3);
}
