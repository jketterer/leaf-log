import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'data/repositories/session_repository.dart';
import 'data/repositories/tea_repository.dart';
import 'data/sources/impl/mock_brew_session_data_source.dart';
import 'data/sources/impl/mock_tea_data_source.dart';
import 'ui/screens/home.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    final teaRepository = TeaRepository(teaDataSource: MockTeaDataSource());
    final sessionRepository =
        BrewSessionRepository(dataSource: MockBrewSessionDataSource());
    return MultiProvider(
        providers: [
          ChangeNotifierProvider(
              create: (context) => teaRepository..loadTeas()),
          ChangeNotifierProvider(
              create: (context) => sessionRepository..loadSessions())
        ],
        child: MaterialApp(
          title: 'Leaf Log',
          theme: ThemeData(
            primarySwatch: Colors.lightGreen,
          ),
          home: const HomeScreen(),
        ));
  }
}
