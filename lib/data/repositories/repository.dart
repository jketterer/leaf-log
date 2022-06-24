import 'package:flutter/widgets.dart';

class Repository extends ChangeNotifier {
  bool _isLoading = false;
  String? _loadingMessage = null;

  bool get isLoading => _isLoading;

  String? get loadingMessage => _loadingMessage;

  void startLoading({String? message}) {
    _isLoading = true;
    _loadingMessage = message;
    notifyListeners();
  }

  void finishLoading() {
    _isLoading = false;
    _loadingMessage = null;
    notifyListeners();
  }
}