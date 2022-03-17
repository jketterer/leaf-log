class BrewingVessel {
  String name;
  VesselType type;
  String? description;

  BrewingVessel(this.name, this.type, this.description);

  BrewingVessel.mock()
    : name = "DefaultVessel",
      type = VesselType.mug,
      description = "Default vessel";
}

enum VesselType {
  teapot,
  gaiwan,
  mug,
  cup,
  other,
}
