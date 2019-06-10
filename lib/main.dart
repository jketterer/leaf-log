import 'package:flutter/material.dart';
import 'package:leaf_log/home.dart';
import 'package:leaf_log/screens/timer.dart';
import 'package:leaf_log/services/timerService.dart';

// Main function, calls runApp to start the app
void main() {
  final timerService = TimerService(); // Provides timer to entire app

  // MyApp is nested within timerService so it gets provided throughout app
  runApp(TimerServiceProvider(
    service: timerService,
      child: MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Leaf Log',
      theme: ThemeData(
        primarySwatch: Colors.lightGreen,
      ),
      initialRoute: '/',
      // Currently unused
      routes: {
        '/': (context) => HomePage(),
        '/timer': (context) => TimerPage(),
      },
    );
  }
}
