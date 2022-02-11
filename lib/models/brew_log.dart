import 'package:leaf_log/models/brewing_vessel.dart';
import 'package:leaf_log/models/tea.dart';

class BrewLogEntry {
  Tea tea;
  BrewingVessel? vessel;
  int? teaQuantity;
  int? waterQuantity;
  int? temperature;
  DateTime timeBrewed;

  BrewLogEntry(this.tea, this.vessel, this.teaQuantity, this.waterQuantity,
      this.temperature, this.timeBrewed);

  BrewLogEntry.simple(this.tea)
      : vessel = null,
        teaQuantity = null,
        waterQuantity = null,
        temperature = null,
        timeBrewed = DateTime.now();
}
