import 'package:flutter/material.dart';

class TeaType {
  final String name;
  final Color color;
  final Duration defaultBrewTime;

  TeaType(this.name, this.color, this.defaultBrewTime);

  const TeaType.green()
      : name = "Green",
        color = Colors.lightGreen,
        defaultBrewTime = const Duration(minutes: 3);

  const TeaType.black()
      : name = "Black",
        color = Colors.brown,
        defaultBrewTime = const Duration(minutes: 5);

  const TeaType.oolong()
      : name = "Oolong",
        color = Colors.amber,
        defaultBrewTime = const Duration(minutes: 3);
}
