import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../data/builders/tea_builder.dart';
import '../../data/repositories/tea_repository.dart';
import '../../models/tea.dart';
import '../../models/tea_type.dart';
import '../../models/vendor.dart';

class EditTeaScreen extends StatefulWidget {
  const EditTeaScreen({this.tea});

  final Tea? tea;

  @override
  State<EditTeaScreen> createState() => _EditTeaScreenState(tea);
}

class _EditTeaScreenState extends State<EditTeaScreen> {
  _EditTeaScreenState(Tea? existingTea)
      : _builder = TeaBuilder.from(existingTea);

  TeaBuilder _builder;

  final _formKey = GlobalKey<FormState>();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Add new tea"),
      ),
      body: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              TextFormField(
                decoration: const InputDecoration(hintText: "Tea name"),
                onSaved: (String? name) => _builder.name = name,
              ),
              TextFormField(
                decoration: const InputDecoration(hintText: "Tea type"),
                onSaved: (String? type) =>
                    _builder.type = _convertTeaType(type),
              ),
              TextFormField(
                decoration: const InputDecoration(hintText: "Vendor"),
                onSaved: (String? vendor) =>
                    _builder.vendor = _convertVendor(vendor),
              ),
              TextFormField(
                decoration: const InputDecoration(hintText: "Rating"),
                onSaved: (String? rating) =>
                    _builder.rating = _convertRating(rating),
              ),
              TextFormField(
                decoration: const InputDecoration(hintText: "Notes"),
                onSaved: (String? notes) => _builder.notes = notes,
              ),
              Consumer<TeaRepository>(
                builder: (context, repository, _) => MaterialButton(
                    child: const Text("Save"),
                    onPressed: () => _saveTea(context, repository)),
              )
            ],
          )),
    );
  }

  TeaType _convertTeaType(String? type) {
    // TODO
    return TeaType.green();
  }

  Vendor _convertVendor(String? vendor) {
    // TODO
    return Vendor("Test Vendor", "");
  }

  int? _convertRating(String? rating) {
    if (rating == null)
      return null;
    else
      return int.tryParse(rating);
  }

  void _saveTea(BuildContext context, TeaRepository repository) {
    var formState = _formKey.currentState;
    if (formState == null) return;

    formState.save();
    var tea = _builder.build();
    repository.addTea(tea);

    // _builder = TeaBuilder();
    Navigator.of(context).pop();
  }
}
