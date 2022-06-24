import '../../models/tea.dart';
import '../../models/tea_type.dart';
import '../../models/vendor.dart';

class TeaBuilder {
  int? id;
  String? name;
  TeaType? type;
  Vendor? vendor;
  int? rating;
  String? notes;

  int? brewCount;
  DateTime? lastBrewed;

  TeaBuilder(
      {this.id,
      this.name,
      this.type,
      this.vendor,
      this.rating,
      this.notes,
      this.brewCount,
      this.lastBrewed});

  TeaBuilder.from(Tea? tea)
      : id = tea?.id,
        name = tea?.name,
        type = tea?.type,
        vendor = tea?.vendor,
        rating = tea?.rating,
        notes = tea?.notes,
        brewCount = tea?.brewCount,
        lastBrewed = tea?.lastBrewed;

  Tea build() {
    _validate();
    return Tea(id ?? 0, name!, type!, vendor!, rating!, notes, brewCount ?? 0,
        lastBrewed ?? DateTime.fromMillisecondsSinceEpoch(0));
  }

  void _validate() {
    if (name == null) {
      throw Exception("Name cannot be null");
    } else if (type == null) {
      throw Exception("Type cannot be null");
    } else if (vendor == null) {
      throw Exception("Vendor cannot be null");
    } else if (rating == null) {
      throw Exception("Rating cannot be null");
    }
  }
}
