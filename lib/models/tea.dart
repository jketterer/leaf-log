class Tea implements Comparable {
  int id;
  String name;
  String type;
  String brand;
  int rating;
  int brewTime;
  int temperature;
  int frequency = 0;
  String notes;

  static final columns = [
    "id",
    "name",
    "brand",
    "type",
    "rating",
    "time",
    "temp",
    "notes"
  ];

  Tea(this.name, this.type, this.brand, this.brewTime, this.temperature,
      this.rating, this.notes);

  Tea.fromTable(this.id, this.name, this.brand, this.type, this.rating,
      this.brewTime, this.temperature, this.notes);

  Tea.test(this.name,
      {this.type = "Green",
      this.brand = "Harney and Sons",
      this.brewTime = 180,
      this.temperature = 212,
      this.rating = 10});

  void addBrewCount() {
    frequency++;
  }

  Map toMap() {
    Map map = <String, dynamic>{
      "id": id,
      "name": name,
      "brand": brand,
      "type": type,
      "rating": rating,
      "time": brewTime,
      "temp": temperature,
      "notes": notes
    };

    if (id != null) {
      map["id"] = id;
    }

    return map;
  }

  static fromMap(Map<String, dynamic> map) {
    Tea tea = new Tea.fromTable(map["id"], map["name"], map["brand"],
        map["type"], map["rating"], map["time"], map["temp"], map["notes"]);

    return tea;
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
