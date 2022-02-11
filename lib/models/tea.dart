import 'package:leaf_log/models/tea_type.dart';
import 'package:leaf_log/models/vendor.dart';

class Tea {
  String name;
  TeaType type;
  Vendor vendor;
  int rating;
  String? notes;
  int brewCount;
  DateTime lastBrewed;

  Tea(this.name, this.type, this.vendor, this.rating, this.notes,
      this.brewCount, this.lastBrewed);

// Tea.simple(this.name, this.rating, this.notes)
//     : brewCount = 0, lastBrewed = DateTime.fromMillisecondsSinceEpoch(0);

  Tea.mock(this.name)
      : type = TeaType.green(),
        vendor = Vendor("Vendor", "Vendor description"),
        rating = 5,
        notes = "These are mock notes",
        brewCount = 0,
        lastBrewed = DateTime.now();
}
