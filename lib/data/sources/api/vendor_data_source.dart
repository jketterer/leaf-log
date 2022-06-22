import '../../../models/vendor.dart';

abstract class VendorDataSource {
  List<Vendor> loadVendors();

  void upsertVendors(Iterable<Vendor> vendors);
}
