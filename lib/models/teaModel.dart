import 'package:scoped_model/scoped_model.dart';
import 'package:leaf_log/models/tea.dart';
import 'dart:collection';

class TeaModel extends Model {
  final List<Tea> _teaList = [
    Tea.full("Golden Monkey", "Black", 9),
    Tea.full("Ti Quan Yin", "Oolong", 9),
    Tea.full("Big Red Sun", "Black", 7)
  ];

  UnmodifiableListView<Tea> get teaList => UnmodifiableListView(_teaList);

  void add(Tea tea) {
    _teaList.add(tea);
    notifyListeners();
  }
}