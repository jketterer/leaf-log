class Tea implements Comparable {
  String name;
  String type;
  String brand;
  int rating;
  int brewTime;
  int frequency = 0;

  Tea(name, type, brand, brewTime, rating);

  Tea.test(name,
      {type = "Green", brand = "Harney and Sons", rating = 10, brewTime = 180});

  void addBrewCount() {
    frequency++;
  }

  void setName(String newName) {
    name = newName;
  }

  void setType(String newType) {
    type = newType;
  }

  void setBrand(String newBrand) {
    brand = newBrand;
  }

  void setRating(int newRating) {
    rating = newRating;
  }

  void setBrewTime(int newBrewTime) {
    brewTime = newBrewTime;
  }

  @override
  int compareTo(other) {
    return other.rating - this.rating;
  }
}
