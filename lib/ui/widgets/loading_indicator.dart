import 'package:flutter/material.dart';

class LoadingIndicator extends StatelessWidget {
  const LoadingIndicator({this.loadingMessage});

  final String? loadingMessage;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (loadingMessage != null) Text(loadingMessage!),
        Padding(
          padding: const EdgeInsets.all(12.0),
          child: CircularProgressIndicator(),
        )
      ],
    );
  }
}
