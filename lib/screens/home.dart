import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
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
            TeaCard(tea: Tea.mock("Imperial Mojiang Pure Bud Black Tea")),
            TeaCard(tea: Tea.mock("Jasmine")),
            TeaCard(tea: Tea.mock("Big Red Sun")),
            TeaCard(tea: Tea.mock("Jade Snails")),
            TeaCard(tea: Tea.mock("High Mountain Red")),
            TeaCard(tea: Tea.mock("Iron Goddess of Mercy")),
            TeaCard(tea: Tea.mock("Tea 7")),
            TeaCard(tea: Tea.mock("Tea 8")),
            TeaCard(tea: Tea.mock("Tea 9")),
          ],
        ),
      ),
      bottomNavigationBar: BottomNavigationBar(
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(icon: Icon(Icons.book), label: "Teas"),
          BottomNavigationBarItem(icon: Icon(Icons.history_edu), label: "Logs"),
          BottomNavigationBarItem(
            icon: Icon(Icons.timer),
            label: "Timer",
          ),
        ],
      ),
    );
  }
}
