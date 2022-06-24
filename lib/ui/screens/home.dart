import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/ui/screens/edit_tea_screen.dart';
import 'package:leaf_log/ui/widgets/loading_indicator.dart';
import 'package:leaf_log/ui/widgets/navbar.dart';
import 'package:leaf_log/ui/widgets/tea_card.dart';
import 'package:provider/provider.dart';

import '../../data/repositories/tea_repository.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("My Teas"),
        actions: [
          IconButton(onPressed: () {}, icon: Icon(Icons.search)),
          IconButton(
              onPressed: () {
                Navigator.push(context,
                    MaterialPageRoute(builder: (BuildContext context) {
                  return EditTeaScreen();
                }));
              },
              icon: Icon(Icons.add))
        ],
      ),
      body: Center(
          child: Consumer<TeaRepository>(
              builder: (context, repository, child) => repository.isLoading
                  ? LoadingIndicator(loadingMessage: repository.loadingMessage)
                  : _buildGridView(repository.teas))),
      bottomNavigationBar: BottomNavBar(),
    );
  }

  GridView _buildGridView(List<Tea> teas) {
    return GridView.count(
      crossAxisCount: 2,
      mainAxisSpacing: 4,
      crossAxisSpacing: 4,
      padding: const EdgeInsets.all(12.0),
      children: _buildTeaCards(teas),
    );
  }

  List<TeaCard> _buildTeaCards(List<Tea> teas) {
    return teas.map((tea) => TeaCard(tea: tea)).toList();
  }
}
