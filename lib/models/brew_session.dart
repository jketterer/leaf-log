import 'dart:math';

import 'brewing_vessel.dart';
import 'tea.dart';

class BrewSession {
  final int id;
  final Tea tea;
  final BrewingVessel vessel;
  final DateTime timeBrewed;
  final int? teaQuantity;
  final int? waterQuantity;
  final int? temperature;

  const BrewSession(this.id, this.tea, this.vessel, this.timeBrewed,
      this.teaQuantity, this.waterQuantity, this.temperature);

  BrewSession.simple(this.tea)
      : id = Random().nextInt(999999),
        vessel = BrewingVessel.mock(),
        timeBrewed = DateTime.now(),
        teaQuantity = null,
        waterQuantity = null,
        temperature = null;

  BrewSession copyWith(
          {int? id,
          Tea? tea,
          BrewingVessel? vessel,
          DateTime? timeBrewed,
          int? teaQuantity,
          int? waterQuantity,
          int? temperature}) =>
      BrewSession(
          id ?? this.id,
          tea ?? this.tea,
          vessel ?? this.vessel,
          timeBrewed ?? this.timeBrewed,
          teaQuantity ?? this.teaQuantity,
          waterQuantity ?? this.waterQuantity,
          temperature ?? this.temperature);
}
