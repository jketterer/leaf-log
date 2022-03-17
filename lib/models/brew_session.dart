import 'package:leaf_log/models/brewing_vessel.dart';
import 'package:leaf_log/models/tea.dart';

class BrewSession {
  Tea tea;
  BrewingVessel vessel;
  DateTime timeBrewed;
  int? teaQuantity;
  int? waterQuantity;
  int? temperature;

  BrewSession(this.tea, this.vessel, this.teaQuantity, this.waterQuantity,
      this.temperature, this.timeBrewed);

  BrewSession.simple(this.tea)
      : vessel = BrewingVessel.mock(),
        timeBrewed = DateTime.now(),
        teaQuantity = null,
        waterQuantity = null,
        temperature = null;
}
