import 'package:flutter/material.dart';
import 'package:leaf_log/models/brew_session.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/models/tea_type.dart';
import 'package:leaf_log/ui/widgets/navbar.dart';
import 'package:leaf_log/ui/widgets/session_list.dart';


class SessionsScreen extends StatelessWidget {
  SessionsScreen({Key? key}) : super(key: key);

  final tea = Tea.mock("Test Tea", TeaType.black());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Tea Sessions"),
      ),
      body: SessionList(sessionList: [
        BrewSession.simple(tea),
        BrewSession.simple(tea),
        BrewSession.simple(tea),
        BrewSession.simple(tea),
        BrewSession.simple(tea),
      ],
      detailsOnly: false,),
      bottomNavigationBar: BottomNavBar(),
    );
  }
}
