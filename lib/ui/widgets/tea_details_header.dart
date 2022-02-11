import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';

class TeaDetailsHeader extends StatelessWidget {
  final Tea tea;

  const TeaDetailsHeader({Key? key, required this.tea}) : super(key: key);

  final nameStyle = const TextStyle(
    fontSize: 40,
    fontWeight: FontWeight.bold,
  );

  final typeStyle = const TextStyle(
    color: Colors.black45,
    fontSize: 32,
  );

  final vendorStyle = const TextStyle(
    fontSize: 30,
    color: Colors.black45,
    fontStyle: FontStyle.italic,
  );

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: BoxConstraints(
        minHeight: 260,
        maxHeight: 400
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(tea.name, style: nameStyle,),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("${tea.type.name} Tea", style: typeStyle,),
                    Text(tea.vendor.name, style: vendorStyle,),
                  ],
                ),
                Icon(Icons.circle, size: 180,),
              ],
            )
          ],
        ),
      ),
    );
  }
}
