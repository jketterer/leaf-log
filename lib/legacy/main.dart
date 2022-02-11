import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
import 'package:leaf_log/home.dart';
import 'package:leaf_log/screens/timer.dart';
import 'package:leaf_log/services/timer_service.dart';
import 'package:preferences/preferences.dart';

import 'models/color_maps.dart';

// Main function, calls runApp to start the app
void main() async {
  final timerService = TimerService(); // Provides timer to entire app
  await PrefService.init(
      prefix: 'pref_'); // Provides access to preferences page

  // MyApp is nested within timerService so it gets provided throughout app
  runApp(
    TimerServiceProvider(
      service: timerService,
      child: MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    // Dynamic theme allows theme to be changed on the fly
    return DynamicTheme(
      defaultBrightness: Brightness.light,
      data: (brightness) => new ThemeData(
          primarySwatch: ColorMaps.themeColors[PrefService.getString("theme_color")] ?? Colors.lightGreen,
          brightness: brightness),
      themedWidgetBuilder: (context, theme) {
        return new MaterialApp(
          title: 'Leaf Log',
          theme: theme,
          home: new HomePage(),
        );
      },
    );
  }
}
