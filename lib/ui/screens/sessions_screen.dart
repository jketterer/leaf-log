import 'package:flutter/material.dart';
import 'package:leaf_log/ui/widgets/navbar.dart';
import 'package:leaf_log/ui/widgets/session_list.dart';
import 'package:provider/provider.dart';

import '../../data/repositories/session_repository.dart';

class SessionsScreen extends StatelessWidget {
  SessionsScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Tea Sessions"),
      ),
      body: Consumer<BrewSessionRepository>(
        builder: (context, repository, _) => repository.isLoading
            ? CircularProgressIndicator()
            : SessionList(
                sessionList: repository.brewSessions,
                detailsOnly: false,
              ),
      ),
      bottomNavigationBar: BottomNavBar(),
    );
  }
}
