import 'dart:math';

import 'tea_type.dart';
import 'vendor.dart';

class Tea {
  final int id;
  final String name;
  final TeaType type;
  final Vendor vendor;
  final int rating;
  final String? notes;
  final int brewCount;
  final DateTime lastBrewed;

  Tea(this.id, this.name, this.type, this.vendor, this.rating, this.notes,
      this.brewCount, this.lastBrewed);

  Tea.mock(
      {int? id = null,
      this.name = "Test tea",
      this.type = const TeaType.black()})
      : vendor = Vendor("Vendor", "Vendor description"),
        rating = 5,
        notes = "These are mock notes",
        brewCount = 0,
        id = id == null ? Random().nextInt(9999999) : id,
        lastBrewed = DateTime.now();

  Tea copyWith(
          {int? id,
          String? name,
          TeaType? type,
          Vendor? vendor,
          int? rating,
          String? notes,
          int? brewCount,
          DateTime? lastBrewed}) =>
      Tea(
          id ?? this.id,
          name ?? this.name,
          type ?? this.type,
          vendor ?? this.vendor,
          rating ?? this.rating,
          notes ?? this.notes,
          brewCount ?? this.brewCount,
          lastBrewed ?? this.lastBrewed);
}
