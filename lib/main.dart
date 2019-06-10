import 'package:flutter/material.dart';
import 'package:leaf_log/home.dart';
import 'package:leaf_log/screens/timer.dart';
import 'package:leaf_log/services/databaseHelper.dart';
import 'package:leaf_log/services/timerService.dart';
import 'package:leaf_log/models/teaModel.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:scoped_model/scoped_model.dart';

// Main function, calls runApp to start the app
void main() {
  final teaList = TeaModel(); // Provides list of tea to entire app
  //teaList.add(Tea("Test Tea", "Harney", "Green", 5, 90, 100, ""));
  final timerService = TimerService(); // Provides timer to entire app

  // MyApp is nested within teaList and timerService so they get both get provided throughout app
  runApp(TimerServiceProvider(
    service: timerService,
    child: ScopedModel<TeaModel>(
      model: teaList,
      child: MyApp(),
    ),
  ));
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
