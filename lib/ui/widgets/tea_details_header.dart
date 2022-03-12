import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';

class TeaDetailsHeader extends StatelessWidget {
  final Tea tea;

  TeaDetailsHeader({Key? key, required this.tea}) : super(key: key);

  final nameStyle = const TextStyle(
    fontSize: 36,
    fontWeight: FontWeight.bold,
  );

  final vendorStyle = const TextStyle(
    fontSize: 24,
    color: Colors.black45,
  );

  @override
  Widget build(BuildContext context) {
    TextStyle typeStyle = TextStyle(
      color: tea.type.color.withAlpha(150),
      fontSize: 24,
      fontStyle: FontStyle.italic,
    );

    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Card(
        child: ConstrainedBox(
          constraints: BoxConstraints(
            minHeight: 220,
            maxHeight: 400
          ),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
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
                        Text(tea.vendor.name, style: vendorStyle,),
                        Text("${tea.type.name} Tea", style: typeStyle,),
                      ],
                    ),
                    Icon(Icons.circle, size: 100,),
                  ],
                )
              ],
            ),
          ),
        ),
      ),
    );
  }
}
