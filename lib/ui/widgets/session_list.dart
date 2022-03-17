import 'package:flutter/material.dart';
import 'package:leaf_log/models/brew_session.dart';
import 'package:leaf_log/ui/widgets/session_card.dart';

class SessionList extends StatelessWidget {
  final List<BrewSession> sessionList;
  final bool detailsOnly;
  
  const SessionList({Key? key, required this.sessionList, this.detailsOnly = false}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(6.0),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text("The filter bar will go here eventually"),
          Expanded(
            child: ListView.builder(
                itemCount: sessionList.length,
                itemBuilder: (BuildContext context, int index) {
                  return SessionCard(session: sessionList[index], detailsOnly: detailsOnly,);
                }),
          )
        ],
      ),
    );
  }
}
