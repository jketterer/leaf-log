import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/ui/widgets/session_list.dart';
import 'package:leaf_log/ui/widgets/tea_details_header.dart';
import 'package:provider/provider.dart';

import '../../data/repositories/session_repository.dart';

class TeaDetailsScreen extends StatelessWidget {
  const TeaDetailsScreen({Key? key, required this.tea}) : super(key: key);

  final Tea tea;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Tea Details"),
      ),
      body: Column(
        children: [
          TeaDetailsHeader(tea: tea),
          Expanded(
            child: Consumer<BrewSessionRepository>(
                builder: (context, repository, _) => repository.isLoading
                    ? Center(child: CircularProgressIndicator())
                    : SessionList(
                        sessionList: repository.getForTea(tea.id),
                        detailsOnly: true,
                      )),
          )
        ],
      ),
    );
  }
}
