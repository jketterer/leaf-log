import 'dart:math';

import '../../../data/sources/api/tea_data_source.dart';
import '../../../models/tea.dart';
import '../../../models/tea_type.dart';

class MockTeaDataSource implements TeaDataSource {
  final List<Tea> _teas = [
    Tea.mock(id: 1, name: "Imperial Mojiang Pure Bud Black Tea"),
    Tea.mock(id: 2, name: "Jasmine", type: TeaType.green()),
    Tea.mock(id: 3, name: "Big Red Sun"),
    Tea.mock(id: 4, name: "Jade Snails", type: TeaType.green()),
    Tea.mock(id: 5, name: "High Mountain Red"),
    Tea.mock(id: 6, name: "Iron Goddess of Mercy", type: TeaType.oolong()),
  ];

  @override
  Future<List<Tea>> loadTeas() async {
    return Future.delayed(Duration(seconds: 3), () => List.unmodifiable(_teas));
  }

  @override
  Future<int> upsertTea(Tea tea) async {
    return Future.delayed(Duration(seconds: 2), () => Random().nextInt(9999));
  }

  @override
  Future deleteTea(int id) async {
    return Future.delayed(Duration(seconds: 1));
  }
}
