class Tea implements Comparable {
  String name;
  String type;
  String brand;
  int rating;
  int brewTime;
  int temperature;
  int frequency = 0;

  Tea(this.name, this.type, this.brand, this.brewTime, this.temperature, this.rating);

  Tea.test(this.name,
      {this.type = "Green", this.brand = "Harney and Sons", this.brewTime = 180, this.temperature = 212, this.rating = 10});

  void addBrewCount() {
    frequency++;
  }

  void setName(String newName) {
    this.name = newName;
  }

  void setType(String newType) {
    this.type = newType;
  }

  void setBrand(String newBrand) {
    this.brand = newBrand;
  }

  void setRating(int newRating) {
    this.rating = newRating;
  }

  void setBrewTime(int newBrewTime) {
    this.brewTime = newBrewTime;
  }

  void setTemperature(int newTemp) {
    this.temperature = newTemp;
  }

  @override
  int compareTo(other) {
    return other.rating - this.rating;
  }
}
