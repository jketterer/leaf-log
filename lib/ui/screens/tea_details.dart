import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';

class TeaDetailsScreen extends StatelessWidget {
  final Tea tea;

  const TeaDetailsScreen({Key? key, required this.tea}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Tea Details"),
      ),
      body: Center(
        child: Text(tea.name),
      ),
    );
  }
}
