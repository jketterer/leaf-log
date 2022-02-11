import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/ui/screens/tea_details.dart';

class TeaCard extends StatelessWidget {
  final Tea tea;

  const TeaCard({Key? key, required this.tea}) : super(key: key);

  final TextStyle teaNameStyle = const TextStyle(
    fontSize: 22,
    // height: 100,
  );

  final TextStyle teaVendorStyle =
      const TextStyle(fontSize: 16, color: Colors.black45);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        onTap: () => {openTeaDetails(context, tea)},
        onLongPress: () => {"delete tea here"},
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
                          icon: const Icon(Icons.coffee_outlined)),
                      IconButton(
                          onPressed: () => {},
                          icon: const Icon(Icons.favorite_border))
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

  void openTeaDetails(BuildContext context, Tea tea) {
    Navigator.push(context, MaterialPageRoute(builder: (BuildContext context) {
      return TeaDetailsScreen(tea: tea);
    }));
  }
}
