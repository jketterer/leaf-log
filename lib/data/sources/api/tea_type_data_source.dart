import '../../../models/tea_type.dart';

abstract class TeaTypeDataSource {
  Set<TeaType> loadTeaTypes();

  void upsertTeaTypes(Set<TeaType> types);
}
