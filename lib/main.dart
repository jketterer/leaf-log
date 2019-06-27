import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
import 'package:leaf_log/home.dart';
import 'package:leaf_log/screens/timer.dart';
import 'package:leaf_log/services/timer_service.dart';
import 'package:preferences/preferences.dart';

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

  @override
  Widget build(BuildContext context) {
    return DynamicTheme(
      defaultBrightness: Brightness.light,
      data: (brightness) => new ThemeData(
          primarySwatch: _colorMap[PrefService.getString("theme_color")],
          brightness: brightness),
      themedWidgetBuilder: (context, theme) {
        return new MaterialApp(
          title: 'Leaf Log',
          theme: theme,
          initialRoute: '/',
          // Currently unused
          routes: {
            '/': (context) => HomePage(),
            '/timer': (context) => TimerPage(),
          },
        );
      },
    );
  }
}
