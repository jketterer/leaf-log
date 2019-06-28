import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/screens/tea_widgets.dart';
import 'package:leaf_log/services/database_helper.dart';

class CustomSearchDelegate extends SearchDelegate {
  DatabaseHelper helper = DatabaseHelper.instance;

  Function callParentFunction;

  CustomSearchDelegate({this.callParentFunction});

  @override
  ThemeData appBarTheme(BuildContext context) {
    assert(context != null);
    final ThemeData theme = Theme.of(context);
    assert(theme != null);
    return theme;
  }

  // Define actions in appbar
  @override
  List<Widget> buildActions(BuildContext context) {
    return [
      IconButton(
        icon: Icon(Icons.clear),
        onPressed: () {
          query = "";
        },
      )
    ];
  }

  // Define actions at left of appbar
  @override
  Widget buildLeading(BuildContext context) {
    return IconButton(
      icon: Icon(Icons.arrow_back),
      onPressed: () {
        close(context, null);
      },
    );
  }

  // Currently unused
  @override
  Widget buildResults(BuildContext context) {
    return Container();
  }

  // Build a listview of teas containing search query
  @override
  Widget buildSuggestions(BuildContext context) {
    return FutureBuilder<List<Tea>>(
      future: helper.searchTeaList(query),
      builder: (context, snapshot) {
        if (snapshot.hasError) print(snapshot.error);
        var list = snapshot.data;
        return snapshot.hasData
            ? new ListView.builder(
                itemCount: list.length,
                itemBuilder: (BuildContext context, int index) {
                  return new TeaCard(
                      tea: list[index],
                      callParentFunction: callParentFunction,
                      onPressed: () {
                        close(context, list[index]);
                      });
                },
              )
            : new Center(child: new CircularProgressIndicator());
      },
    );
  }
}
