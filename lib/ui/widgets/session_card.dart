
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:leaf_log/models/brew_session.dart';
import 'package:leaf_log/models/brewing_vessel.dart';

class SessionCard extends StatelessWidget {
  final BrewSession session;
  final bool detailsOnly;

  const SessionCard({Key? key, required this.session, required this.detailsOnly}) : super(key: key);

  final primaryStyle = const TextStyle(
    fontSize: 18,
  );

  final secondaryStyle = const TextStyle(
    fontSize: 12,
  );

  DateFormat _getDateFormat(BrewSession session) {
    if (session.timeBrewed.difference(DateTime.now()).inDays <= -365) {
      return DateFormat.yMMMMd();
    } else return DateFormat.MMMMd();
  }

  @override
  Widget build(BuildContext context) {
    TextStyle timeStyle = detailsOnly ? primaryStyle : secondaryStyle;
    DateFormat dateFormat = _getDateFormat(session);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                detailsOnly ? Container() : Text(session.tea.name, style: primaryStyle,),
                Text(dateFormat.format(session.timeBrewed), style: timeStyle,),
              ],
            ),
            Row(
              children: [
                VesselIcon(vessel: session.vessel),
                IconButton(onPressed: () => {}, icon: Icon(Icons.star_border))
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class VesselIcon extends StatelessWidget {
  final BrewingVessel vessel;

  VesselIcon({Key? key, required this.vessel}) : super(key: key);

  // TODO: Use better icons here
  IconData _getIcon(BrewingVessel vessel) {
    if (vessel.type == VesselType.mug) {
      return Icons.coffee_outlined;
    } else if (vessel.type == VesselType.gaiwan) {
      return Icons.star;
    } else if (vessel.type == VesselType.cup) {
      return Icons.cloud_upload;
    } else if (vessel.type == VesselType.teapot) {
      return Icons.circle;
    } else {
      return Icons.cancel;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Icon(_getIcon(vessel));
  }
}
