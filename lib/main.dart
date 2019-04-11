import 'package:flutter/material.dart';
import 'package:leaf_log/home.dart';
import 'package:leaf_log/screens/timer.dart';
import 'package:leaf_log/models/teaModel.dart';
import 'package:scoped_model/scoped_model.dart';

void main() {
  final teaList = TeaModel();

  runApp(
    ScopedModel<TeaModel>(
      model: teaList,
      child: MyApp(),
    )
  );
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Leaf Log',
      theme: ThemeData(
        // This is the theme of your application.
        primarySwatch: Colors.lightGreen,
      ),
      initialRoute: '/',
      routes: {
        '/': (context) => HomePage(),
        '/timer': (context) => TimerPage(),
      },
    );
  }
}
