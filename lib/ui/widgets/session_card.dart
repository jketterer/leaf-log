import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:leaf_log/models/brew_session.dart';

class SessionCard extends StatelessWidget {
  final BrewSession session;

  const SessionCard({Key? key, required this.session}) : super(key: key);

  final timeStyle = const TextStyle(
    fontSize: 18,
  );

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(DateFormat.MMMMd().format(session.timeBrewed), style: timeStyle,),
            Text(session.teaQuantity.toString())
          ],
        ),
      ),
    );
  }
}
