import 'package:flutter/material.dart';
import 'package:leaf_log/models/brew_session.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/ui/widgets/session_list.dart';
import 'package:leaf_log/ui/widgets/tea_details_header.dart';

class TeaDetailsScreen extends StatelessWidget {
  final Tea tea;

  const TeaDetailsScreen({Key? key, required this.tea}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Tea Details"),
      ),
      body: Column(
        children: [
          TeaDetailsHeader(tea: tea),
          Expanded(
            child: SessionList(sessionList: [
              BrewSession.simple(tea),
              BrewSession.simple(tea),
              BrewSession.simple(tea),
              BrewSession.simple(tea),
              BrewSession.simple(tea),
            ],),
          )
        ],
      ),
    );
  }
}
