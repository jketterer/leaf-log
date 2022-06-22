import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/ui/screens/tea_details.dart';
import 'package:provider/provider.dart';

import '../../data/repositories/tea_repository.dart';

class TeaCard extends StatelessWidget {
  final Tea tea;

  const TeaCard({Key? key, required this.tea}) : super(key: key);

  final TextStyle teaNameStyle = const TextStyle(
    fontSize: 22,
  );

  final TextStyle teaVendorStyle =
      const TextStyle(fontSize: 16, color: Colors.black45);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        onTap: () => _openTeaDetails(context, tea),
        onLongPress: () => _removeTea(context, tea),
        child: Padding(
          padding: const EdgeInsets.only(top: 10, left: 10, right: 10),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    tea.name,
                    style: teaNameStyle,
                  ),
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      tea.vendor.name,
                      style: teaVendorStyle,
                    ),
                  ),
                ],
              ),
              Column(
                children: [
                  const Divider(),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      IconButton(
                          onPressed: () => {},
                          icon: const Icon(Icons.favorite_border)),
                      IconButton(
                          onPressed: () => {},
                          icon: const Icon(Icons.coffee_outlined))
                    ],
                  )
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openTeaDetails(BuildContext context, Tea tea) {
    Navigator.push(context, MaterialPageRoute(builder: (BuildContext context) {
      return TeaDetailsScreen(tea: tea);
    }));
  }

  void _removeTea(BuildContext context, Tea tea) {
    Provider.of<TeaRepository>(context, listen: false).removeTea(tea.id);
  }
}
