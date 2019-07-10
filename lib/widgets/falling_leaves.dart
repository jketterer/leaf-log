import 'dart:math';

import 'package:flutter/material.dart';
import 'package:leaf_log/models/color_maps.dart';
import 'package:leaf_log/services/timer_service.dart';
import 'package:preferences/preference_service.dart';

class FallingLeafBackground extends StatefulWidget {
  @override
  _FallingLeafBackgroundState createState() => _FallingLeafBackgroundState();
}

class _FallingLeafBackgroundState extends State<FallingLeafBackground>
    with TickerProviderStateMixin {
  AnimationController verticalController;
  AnimationController horizontalController;

  // Each leaf gets its own vertical animation to stagger them
  Animation verticalAnimation1,
      verticalAnimation2,
      verticalAnimation3,
      verticalAnimation4,
      verticalAnimation5;

  Animation horizontalAnimation;

  @override
  void initState() {
    super.initState();

    // These control the vertical and horizontal animations
    verticalController =
        AnimationController(vsync: this, duration: Duration(seconds: 60));
    horizontalController =
        AnimationController(vsync: this, duration: Duration(seconds: 3));

    // Each leaf has its own animation, the only difference being the interval times
    verticalAnimation1 = Tween(begin: Offset(0, -1), end: Offset(0, 20))
        .animate(CurvedAnimation(
          parent: verticalController,
          curve: Interval(0, 0.5)
        ));
    verticalAnimation2 = Tween(begin: Offset(0, -1), end: Offset(0, 20))
        .animate(CurvedAnimation(
          parent: verticalController,
          curve: Interval(0.3, 0.8)
        ));
    verticalAnimation3 = Tween(begin: Offset(0, -1), end: Offset(0, 20))
        .animate(CurvedAnimation(
          parent: verticalController,
          curve: Interval(0.15, 0.65)
        ));
    verticalAnimation4 = Tween(begin: Offset(0, -1), end: Offset(0, 20))
        .animate(CurvedAnimation(
          parent: verticalController,
          curve: Interval(0.5, 1)
        ));
    verticalAnimation5 = Tween(begin: Offset(0, -1), end: Offset(0, 20))
        .animate(CurvedAnimation(
          parent: verticalController,
          curve: Interval(0.4, 0.9)
        ));

    // Each leaf shares the same horizontal animation, may change in the future
    horizontalAnimation = Tween(begin: Offset(0.5, 0), end: Offset(-0.5, 0))
        .animate(horizontalController);

    // Repeat both controllers indefinitely
    verticalController.repeat();
    horizontalController.repeat(reverse: true);
  }

  @override
  void dispose() {
    verticalController.dispose();
    horizontalController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
        child: Row(
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      children: <Widget>[
        // Each leaf is wrapped in 2 SlideTransitions, one for vertical and one for horizontal
        SlideTransition(
          position: verticalAnimation1,
          child: SlideTransition(
            position: horizontalAnimation,
            child: FallingLeaf(),
          ),
        ),
        SlideTransition(
          position: verticalAnimation2,
          child: SlideTransition(
            position: horizontalAnimation,
            child: FallingLeaf(),
          ),
        ),
        SlideTransition(
          position: verticalAnimation3,
          child: SlideTransition(
            position: horizontalAnimation,
            child: FallingLeaf(),
          ),
        ),
        SlideTransition(
          position: verticalAnimation4,
          child: SlideTransition(
            position: horizontalAnimation,
            child: FallingLeaf(),
          ),
        ),
        SlideTransition(
          position: verticalAnimation5,
          child: SlideTransition(
            position: horizontalAnimation,
            child: FallingLeaf(),
          ),
        ),
      ],
    ));
  }
}

class FallingLeaf extends StatefulWidget {
  @override
  _FallingLeafState createState() => _FallingLeafState();
}

class _FallingLeafState extends State<FallingLeaf>
    with SingleTickerProviderStateMixin {
  AnimationController rotationController;
  Animation rotateAnimation;

  @override
  void initState() {
    super.initState();
    // Leaf rotation is controlled by this
    rotationController =
        AnimationController(vsync: this, duration: Duration(seconds: 3))
          ..repeat(reverse: true);
    // Rotation value in radians is tweened here
    rotateAnimation = 
        Tween(begin: pi / 32, end: pi / 2).animate(rotationController);
  }

  @override
  void dispose() {
    rotationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    TimerService timerService = TimerService.of(context);

    // Get theme color from user preferences or set it to green by default
    Color themeColor =
      ColorMaps.themeColors[PrefService.getString("theme_color")] ??
          Colors.lightGreen;

    // Leaf color is based on tea being brewed or user defined theme color
    Color leafColor = timerService.currentTea != null
        ? ColorMaps.getTypeColor(timerService.currentTea.type)
        : themeColor;

    return Container(
      height: 40,
      // AnimatedBuilder allows for animating a child based on a custom animation
      child: AnimatedBuilder(
        animation: rotateAnimation,
        child: Image.asset(
          "assets/leaf.png",
          color: leafColor,
        ),
        builder: (context, child) {
          // Rotate the leaf based on rotateAnimation's value
          return Transform.rotate(
            angle: rotateAnimation.value,
            child: child,
          );
        },
      ),
    );
  }
}
