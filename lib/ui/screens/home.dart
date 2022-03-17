import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/models/tea_type.dart';
import 'package:leaf_log/ui/widgets/navbar.dart';
import 'package:leaf_log/ui/widgets/tea_card.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("My Teas"),
      ),
      body: Center(
        child: GridView.count(
          crossAxisCount: 2,
          mainAxisSpacing: 4,
          crossAxisSpacing: 4,
          padding: const EdgeInsets.all(12.0),
          children: [
            TeaCard(tea: Tea.mock("Imperial Mojiang Pure Bud Black Tea", TeaType.black())),
            TeaCard(tea: Tea.mock("Jasmine", TeaType.green())),
            TeaCard(tea: Tea.mock("Big Red Sun", TeaType.black())),
            TeaCard(tea: Tea.mock("Jade Snails", TeaType.green())),
            TeaCard(tea: Tea.mock("High Mountain Red", TeaType.black())),
            TeaCard(tea: Tea.mock("Iron Goddess of Mercy", TeaType.oolong())),
            TeaCard(tea: Tea.mock("Tea 7", TeaType.black())),
            TeaCard(tea: Tea.mock("Tea 8", TeaType.black())),
            TeaCard(tea: Tea.mock("Tea 9", TeaType.black())),
          ],
        ),
      ),
      bottomNavigationBar: BottomNavBar(),
    );
  }
}
