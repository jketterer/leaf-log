import 'package:flutter/material.dart';

class BottomNavBar extends StatelessWidget {
  const BottomNavBar({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return BottomNavigationBar(
      items: const <BottomNavigationBarItem>[
        BottomNavigationBarItem(icon: Icon(Icons.book), label: "Teas"),
        BottomNavigationBarItem(icon: Icon(Icons.history_edu), label: "Sessions"),
        BottomNavigationBarItem(
          icon: Icon(Icons.timer),
          label: "Timer",
        ),
      ],
    );
  }
}
