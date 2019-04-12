import 'package:scoped_model/scoped_model.dart';
import 'package:leaf_log/models/tea.dart';
import 'dart:collection';

class TeaModel extends Model {
  final List<Tea> _teaList = [
    Tea.test("Golden Monkey"),
    Tea.test("Ti Quan Yin"),
    Tea.test("Big Red Sun"),
  ];

  UnmodifiableListView<Tea> get teaList => UnmodifiableListView(_teaList);

  void add(Tea tea) {
    _teaList.add(tea);
    notifyListeners();
  }

  void sort() {
    _teaList.sort();
    notifyListeners();
  }
}