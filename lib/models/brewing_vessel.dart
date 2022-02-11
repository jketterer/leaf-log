class BrewingVessel {
  String name;
  VesselType type;
  String? description;

  BrewingVessel(this.name, this.type, this.description);
}

enum VesselType {
  teapot,
  gaiwan,
  mug,
  cup,
  other,
}
