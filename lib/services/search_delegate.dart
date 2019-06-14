import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/services/database_helper.dart';

class CustomSearchDelegate extends SearchDelegate {
  List<Tea> teaList;
  DatabaseHelper helper = DatabaseHelper.instance;

  CustomSearchDelegate(

  );

  @override
  ThemeData appBarTheme(BuildContext context) {
    assert(context != null);
    final ThemeData theme = Theme.of(context);
    assert(theme != null);
    return theme;
  }

  @override
  List<Widget> buildActions(BuildContext context) {
    // TODO: implement buildActions
    return [
      IconButton(
        icon: Icon(Icons.clear),
        onPressed: () {
          query = "";
        },
      )
    ];
  }

  @override
  Widget buildLeading(BuildContext context) {
    // TODO: implement buildLeading
    return IconButton(
      icon: Icon(Icons.arrow_back),
      onPressed: () {
        close(context, null);
      },
    );
  }

  @override
  Widget buildResults(BuildContext context) {
    // TODO: implement buildResults
    return Container();
  }

  @override
  Widget buildSuggestions(BuildContext context) {
    // TODO: implement buildSuggestions
    List<String> test = ["test1", "test2", "test3"];

    return FutureBuilder<List<Tea>>(
      future: helper.searchTeaList(query),
      builder: (context, snapshot) {
        if (snapshot.hasError) print(snapshot.error);
        var list = snapshot.data;
        return snapshot.hasData
            ? new ListView.builder(
              itemCount: list.length,
              itemBuilder: (BuildContext context, int index) {
                return new Text(list[index].name);
              },
            )
            : new Center(child: new CircularProgressIndicator());
      },
    );
  }
}
