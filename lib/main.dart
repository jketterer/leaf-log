import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
import 'package:leaf_log/home.dart';
import 'package:leaf_log/screens/timer.dart';
import 'package:leaf_log/services/timer_service.dart';

// Main function, calls runApp to start the app
void main() {
  final timerService = TimerService(); // Provides timer to entire app

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
    return DynamicTheme(
      defaultBrightness: Brightness.light,
      data: (brightness) => new ThemeData(
          primarySwatch: Colors.lightGreen, brightness: brightness),
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
